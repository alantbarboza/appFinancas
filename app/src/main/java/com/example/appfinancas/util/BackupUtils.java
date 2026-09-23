package com.example.appfinancas.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Card;
import com.example.appfinancas.model.Category;
import com.example.appfinancas.model.Transaction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupUtils {

    public static String getFullBackupJson(Context context) throws Exception {
        AppDatabase db = AppDatabase.getDatabase(context);
        List<Transaction> transactions = db.transactionDao().getAllTransactionsList();
        List<Card> cards = db.cardDao().getAllCardsList();
        List<Category> categories = db.categoryDao().getAllCategoriesList();

        JSONObject root = new JSONObject();
        
        JSONArray tArray = new JSONArray();
        for (Transaction t : transactions) {
            JSONObject obj = new JSONObject();
            obj.put("desc", t.description);
            obj.put("amount", t.amount);
            obj.put("type", t.type);
            obj.put("cat", t.category);
            obj.put("date", t.date);
            obj.put("isCard", t.isCard);
            obj.put("isPaid", t.isPaid);
            obj.put("isFixed", t.isFixed);
            obj.put("recId", t.recurringId);
            obj.put("cardId", t.cardId);
            obj.put("bank", t.bankName);
            obj.put("invM", t.invoiceMonth);
            obj.put("invY", t.invoiceYear);
            obj.put("due", t.dueDate);
            tArray.put(obj);
        }
        root.put("transactions", tArray);

        JSONArray cardArray = new JSONArray();
        for (Card c : cards) {
            JSONObject obj = new JSONObject();
            obj.put("name", c.name);
            obj.put("close", c.closingDay);
            obj.put("due", c.dueDay);
            obj.put("color", c.color);
            cardArray.put(obj);
        }
        root.put("cards", cardArray);

        JSONArray catArray = new JSONArray();
        for (Category c : categories) {
            JSONObject obj = new JSONObject();
            obj.put("name", c.name);
            obj.put("icon", c.icon);
            obj.put("color", c.color);
            catArray.put(obj);
        }
        root.put("categories", catArray);

        return root.toString(2);
    }

    public static void exportToDownloads(Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String jsonStr = getFullBackupJson(context);
                
                // Generate filename with timestamp
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault());
                String timestamp = sdf.format(new java.util.Date());
                String fileName = "backup_appFinancas_" + timestamp + ".txt";

                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");
                }

                Uri collection = MediaStore.Files.getContentUri("external");
                resolver.delete(collection, MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[]{"backup.txt"});

                Uri uri = resolver.insert(collection, contentValues);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                    }
                    showToast(context, "Backup baixado na pasta Downloads!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast(context, "Erro ao baixar backup");
            }
        });
    }

    public static String processAndExpandJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!root.has("transactions")) return json;

        JSONArray tArray = root.getJSONArray("transactions");
        
        class SeriesInfo {
            long latestDate = -1;
            int lastIndexInArray = -1;
        }
        java.util.Map<String, SeriesInfo> seriesMap = new java.util.HashMap<>();

        for (int i = 0; i < tArray.length(); i++) {
            JSONObject obj = tArray.getJSONObject(i);
            String desc = obj.optString("desc", "");
            long date = obj.optLong("date", 0);
            
            if (obj.optBoolean("isFixed", false) || desc.contains("/")) {
                String key = getSeriesKey(desc, obj);
                SeriesInfo info = seriesMap.get(key);
                if (info == null) {
                    info = new SeriesInfo();
                    seriesMap.put(key, info);
                }

                if (date > info.latestDate || (date == info.latestDate && i > info.lastIndexInArray)) {
                    info.latestDate = date;
                    info.lastIndexInArray = i;
                }
            }
        }

        JSONArray newArray = new JSONArray();
        boolean expanded = false;

        for (int i = 0; i < tArray.length(); i++) {
            JSONObject obj = tArray.getJSONObject(i);
            String desc = obj.optString("desc", "");
            boolean isFixed = obj.optBoolean("isFixed", false);
            String recId = obj.optString("recId", "");
            
            String key = getSeriesKey(desc, obj);
            SeriesInfo info = seriesMap.get(key);
            boolean isChosenLatest = (info != null && info.lastIndexInArray == i);

            int currentInstallment = -1;
            int totalInstallments = -1;
            String baseDesc = desc;

            Pattern pattern = Pattern.compile("(\\d+)/(\\d+)$");
            Matcher matcher = pattern.matcher(desc.trim());
            if (matcher.find()) {
                try {
                    String g1 = matcher.group(1);
                    String g2 = matcher.group(2);
                    if (g1 != null && g2 != null) {
                        currentInstallment = Integer.parseInt(g1);
                        totalInstallments = Integer.parseInt(g2);
                        baseDesc = desc.substring(0, matcher.start()).trim();
                    }
                } catch (Exception ignored) {}
            }

            if (isChosenLatest && recId.isEmpty() && (totalInstallments > 0 || isFixed)) {
                expanded = true;
                String newRecId = UUID.randomUUID().toString();
                
                int startAt = (currentInstallment != -1) ? currentInstallment : 1;
                int endAt = isFixed ? (startAt + 11) : totalInstallments;
                
                double amountPerParcel = obj.optDouble("amount", 0);
                long startDate = obj.optLong("date", System.currentTimeMillis());
                
                Integer startInvM = obj.has("invM") && !obj.isNull("invM") ? obj.getInt("invM") : null;
                Integer startInvY = obj.has("invY") && !obj.isNull("invY") ? obj.getInt("invY") : null;

                JSONObject firstEntry = new JSONObject(obj.toString());
                firstEntry.put("recId", newRecId);
                
                if (isFutureDate(startDate, startInvM, startInvY)) {
                    firstEntry.put("isPaid", false);
                }
                
                newArray.put(firstEntry);

                for (int j = startAt + 1; j <= endAt; j++) {
                    JSONObject parcel = new JSONObject(obj.toString());
                    int monthOffset = j - startAt;
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(startDate);
                    cal.add(Calendar.MONTH, monthOffset);
                    
                    String newDesc = baseDesc;
                    if (totalInstallments != -1) {
                        newDesc += " " + j + "/" + totalInstallments;
                    }
                    
                    parcel.put("desc", newDesc);
                    parcel.put("amount", amountPerParcel);
                    parcel.put("date", cal.getTimeInMillis());
                    parcel.put("recId", newRecId);
                    parcel.put("isPaid", false); 
                    
                    if (obj.has("due") && !obj.isNull("due")) {
                        Calendar dCal = Calendar.getInstance();
                        dCal.setTimeInMillis(obj.getLong("due"));
                        dCal.add(Calendar.MONTH, monthOffset);
                        parcel.put("due", dCal.getTimeInMillis());
                    }
                    
                    if (startInvM != null && startInvY != null) {
                        int m = startInvM + monthOffset;
                        int y = startInvY;
                        while (m > 11) { m -= 12; y++; }
                        while (m < 0) { m += 12; y--; }
                        parcel.put("invM", m);
                        parcel.put("invY", y);
                    }
                    newArray.put(parcel);
                }
            } else {
                newArray.put(obj);
            }
        }
        
        if (!expanded) return json;
        
        root.put("transactions", newArray);
        return root.toString(2);
    }

    private static String getSeriesKey(String desc, JSONObject obj) {
        Pattern pattern = Pattern.compile("(.+)\\s\\d+/\\d+$");
        Matcher matcher = pattern.matcher(desc.trim());
        if (matcher.find()) {
            String baseName = matcher.group(1);
            if (baseName != null) {
                return "INST_" + baseName.trim() + "_" + obj.optString("cat");
            }
        }
        if (obj.optBoolean("isFixed", false)) {
            return "FIXED_" + desc.trim() + "_" + obj.optString("cat");
        }
        return "NONE";
    }

    private static boolean isFutureDate(long date, Integer invM, Integer invY) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        if (invM != null && invY != null) {
            return (invY > currentYear) || (invY == currentYear && invM > currentMonth);
        }

        Calendar tCal = Calendar.getInstance();
        tCal.setTimeInMillis(date);
        return (tCal.get(Calendar.YEAR) > currentYear) || 
               (tCal.get(Calendar.YEAR) == currentYear && tCal.get(Calendar.MONTH) > currentMonth);
    }

    public static void importFromJson(Context context, String json) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                JSONObject root = new JSONObject(json);
                AppDatabase db = AppDatabase.getDatabase(context);
                db.clearAllTables();

                if (root.has("categories")) {
                    JSONArray cats = root.getJSONArray("categories");
                    for (int i = 0; i < cats.length(); i++) {
                        JSONObject obj = cats.getJSONObject(i);
                        db.categoryDao().insert(new com.example.appfinancas.model.Category(obj.getString("name"), obj.getString("icon"), obj.getString("color")));
                    }
                }

                if (root.has("cards")) {
                    JSONArray cards = root.getJSONArray("cards");
                    for (int i = 0; i < cards.length(); i++) {
                        JSONObject obj = cards.getJSONObject(i);
                        String color = obj.has("color") ? obj.getString("color") : "#2196F3";
                        db.cardDao().insert(new Card(obj.getString("name"), 0.0, obj.getInt("close"), obj.getInt("due"), color));
                    }
                }

                final List<Card> importedCards = db.cardDao().getAllCardsList();

                JSONArray tArray = root.getJSONArray("transactions");
                for (int i = 0; i < tArray.length(); i++) {
                    JSONObject obj = tArray.getJSONObject(i);
                    Integer invM = obj.has("invM") && !obj.isNull("invM") ? obj.getInt("invM") : null;
                    Integer invY = obj.has("invY") && !obj.isNull("invY") ? obj.getInt("invY") : null;
                    
                    String bankName = obj.optString("bank", "Manual");
                    Integer cardId = null;
                    if (obj.optBoolean("isCard", false)) {
                        for (Card c : importedCards) {
                            if (c.name.equals(bankName)) {
                                cardId = c.id;
                                break;
                            }
                        }
                    }

                    boolean isPaid = obj.optBoolean("isPaid", false);
                    long tDate = obj.optLong("date", System.currentTimeMillis());
                    Long dueDate = obj.has("due") && !obj.isNull("due") ? obj.getLong("due") : null;
                    
                    if ("DESPESA".equals(obj.optString("type")) && isFutureDate(tDate, invM, invY)) {
                        isPaid = false;
                    }

                    Transaction t = new Transaction(
                            obj.optString("type", "DESPESA"),
                            obj.optDouble("amount", 0),
                            obj.optString("desc", "Importado"),
                            obj.isNull("cat") ? null : obj.optString("cat", null),
                            tDate,
                            obj.optBoolean("isCard", false) ? "Cartão" : "Manual", 
                            "MANUAL", 
                            cardId, 
                            bankName,
                            obj.optBoolean("isCard", false),
                            1,
                            isPaid,
                            obj.optBoolean("isFixed", false),
                            obj.isNull("recId") ? null : obj.optString("recId", null),
                            invM, invY,
                            dueDate
                    );
                    db.transactionDao().insert(t);
                }
                showToast(context, "Dados importados com sucesso!");
            } catch (Exception e) {
                e.printStackTrace();
                showToast(context, "Erro ao importar: JSON inválido");
            }
        });
    }

    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
