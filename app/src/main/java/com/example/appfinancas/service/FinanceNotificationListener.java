package com.example.appfinancas.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.appfinancas.R;
import com.example.appfinancas.MainActivity;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.NotificationEvent;
import com.example.appfinancas.util.CurrencyUtils;
import com.example.appfinancas.util.NotificationParser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FinanceNotificationListener extends NotificationListenerService {
    private static final String TAG = "FinanceNL";
    private static final String CHANNEL_ID = "finance_suggestions_v3";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if (packageName.equals(getPackageName())) {
            Log.d(TAG, "Ignoring notification from own app.");
            return;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        
        // Try to get BigText (Gmail)
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String text = (bigText != null) ? bigText.toString() : extras.getString(Notification.EXTRA_TEXT);

        Log.d(TAG, "Notification received from: " + packageName);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text: " + text);

        if (text == null || text.isEmpty()) {
            Log.d(TAG, "Notification text is null or empty, ignoring.");
            return;
        }

        if (!getSharedPreferences("finance_prefs", MODE_PRIVATE).getBoolean("monitoring_enabled", true)) {
            Log.d(TAG, "Monitoring is disabled in settings, ignoring.");
            return;
        }

        // Add a timestamp rounding (per minute) to the hash to allow identical purchases in different times
        long minuteBucket = System.currentTimeMillis() / 60000;
        String hash = generateHash(packageName + "|" + title + "|" + text + "|" + minuteBucket);

        // Run with a small delay to let the bank app finish updating the notification text (common in Nubank/Inter)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(this);
                if (db.notificationEventDao().getByHash(hash) != null) {
                    Log.d(TAG, "Duplicate notification ignored: " + hash);
                    return;
                }

                NotificationParser.TransactionSuggestion suggestion = NotificationParser.parse(packageName, title, text);
                if (suggestion != null) {
                    Log.d(TAG, "Transaction identified! Type: " + suggestion.type + ", Amount: " + suggestion.amount);
                    NotificationEvent event = new NotificationEvent(
                            packageName, title, text, hash,
                            suggestion.type, suggestion.amount, false, System.currentTimeMillis()
                    );
                    long id = db.notificationEventDao().insert(event);
                    
                    if (id != -1) {
                        showSuggestionNotification(suggestion, hash, packageName);
                    }
                }
            });
        }, 500); // 500ms delay to ensure text is stable
    }

    private void showSuggestionNotification(NotificationParser.TransactionSuggestion suggestion, String hash, String packageName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Use a stable ID based on the content hash to prevent duplicate alerts
        int notificationId = hash.hashCode();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sugestões de Gastos", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String bankName = getBankName(packageName);

        // Confirm Action
        Intent confirmIntent = new Intent(this, NotificationActionReceiver.class);
        confirmIntent.setAction(NotificationActionReceiver.ACTION_CONFIRM);
        confirmIntent.putExtra("notificationId", notificationId);
        confirmIntent.putExtra("type", suggestion.type);
        confirmIntent.putExtra("amount", suggestion.amount);
        confirmIntent.putExtra("description", suggestion.description);
        confirmIntent.putExtra("bankName", bankName);
        PendingIntent confirmPending = PendingIntent.getBroadcast(this, notificationId + 1, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Ignore Action
        Intent ignoreIntent = new Intent(this, NotificationActionReceiver.class);
        ignoreIntent.setAction(NotificationActionReceiver.ACTION_IGNORE);
        ignoreIntent.putExtra("notificationId", notificationId);
        ignoreIntent.putExtra("hash", hash);
        PendingIntent ignorePending = PendingIntent.getBroadcast(this, notificationId + 2, ignoreIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Edit/Open App Action
        Intent editIntent = new Intent(this, com.example.appfinancas.ui.AddTransactionActivity.class);
        editIntent.putExtra("type", suggestion.type);
        editIntent.putExtra("amount", suggestion.amount);
        editIntent.putExtra("description", suggestion.description);
        editIntent.putExtra("category", suggestion.suggestedCategory);
        editIntent.putExtra("bankName", bankName);
        editIntent.putExtra("notificationId", notificationId);
        
        // Remove CLEAR_TASK to keep MainActivity in background if it was open,
        // or allow TaskStackBuilder/Manual navigation to work better.
        editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent editPending = PendingIntent.getActivity(this, notificationId + 3, editIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(android.graphics.Color.parseColor("#D5CDCD"))
                .setContentTitle("Nova " + suggestion.type + " identificada")
                .setContentText(CurrencyUtils.format(suggestion.amount) + " - " + suggestion.description)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for pop-up
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(new long[]{0, 250, 250, 250})
                .setContentIntent(editPending)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_add, "Confirmar", confirmPending)
                .addAction(android.R.drawable.ic_menu_edit, "Editar", editPending)
                .addAction(android.R.drawable.ic_delete, "Ignorar", ignorePending);

        notificationManager.notify(notificationId, builder.build());
    }

    private String getBankName(String packageName) {
        if (packageName == null) return "Banco";
        if (packageName.contains("nubank")) return "Nubank";
        if (packageName.contains("itau")) return "Itaú";
        if (packageName.contains("bradesco")) return "Bradesco";
        if (packageName.contains("santander")) return "Santander";
        if (packageName.contains("inter")) return "Inter";
        if (packageName.contains("google.android.gm")) return "Gmail";
        return "Banco";
    }

    private String generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
