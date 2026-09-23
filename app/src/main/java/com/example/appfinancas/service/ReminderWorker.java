package com.example.appfinancas.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.appfinancas.MainActivity;
import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Transaction;

import java.util.Calendar;
import java.util.List;

public class ReminderWorker extends Worker {
    private static final String CHANNEL_ID = "payment_reminders";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE);
        int frequency = prefs.getInt("reminder_frequency_days", 1);
        
        AppDatabase db = AppDatabase.getDatabase(context);
        List<Transaction> transactions = db.transactionDao().getAllTransactionsList();
        List<com.example.appfinancas.model.Card> allCards = db.cardDao().getAllCardsList();
        
        int unpaidCount = 0;
        double totalUnpaid = 0;
        
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        
        String title = "Lembrete de Pagamento";
        String content = "";
        int offset = 0;

        if (frequency == 0) { // "No dia do vencimento"
            offset = 0;
            title = "Vence Hoje";
        } else if (frequency == -1) { // "1 dia antes"
            offset = 1;
            title = "Vence Amanhã";
        } else if (frequency == -7) { // "1 semana antes"
            offset = 7;
            title = "Vence em 1 Semana";
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(now);
        targetCal.add(Calendar.DAY_OF_YEAR, offset);
        targetCal.set(Calendar.HOUR_OF_DAY, 0);
        targetCal.set(Calendar.MINUTE, 0);
        targetCal.set(Calendar.SECOND, 0);
        long startOfTargetDay = targetCal.getTimeInMillis();
        
        targetCal.set(Calendar.HOUR_OF_DAY, 23);
        targetCal.set(Calendar.MINUTE, 59);
        targetCal.set(Calendar.SECOND, 59);
        long endOfTargetDay = targetCal.getTimeInMillis();

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeInMillis(now);
        todayCal.set(Calendar.HOUR_OF_DAY, 23);
        todayCal.set(Calendar.MINUTE, 59);
        todayCal.set(Calendar.SECOND, 59);
        long endOfToday = todayCal.getTimeInMillis();

        java.util.Set<String> countedInvoices = new java.util.HashSet<>();

        for (Transaction t : transactions) {
            if (t.isPaid || !"DESPESA".equals(t.type)) continue;

            long dueDate;
            String invoiceKey;

            if (t.isCard) {
                com.example.appfinancas.model.Card card = null;
                if (t.cardId != null) {
                    for (com.example.appfinancas.model.Card c : allCards) {
                        if (c.id == t.cardId) { card = c; break; }
                    }
                }

                if (card != null && t.invoiceMonth != null && t.invoiceYear != null) {
                    Calendar dueCal = Calendar.getInstance();
                    dueCal.set(Calendar.YEAR, t.invoiceYear);
                    dueCal.set(Calendar.MONTH, t.invoiceMonth);
                    dueCal.set(Calendar.DAY_OF_MONTH, card.dueDay);
                    dueCal.set(Calendar.HOUR_OF_DAY, 12);
                    dueDate = dueCal.getTimeInMillis();
                    invoiceKey = "CARD_" + card.id + "_" + t.invoiceMonth + "_" + t.invoiceYear;
                } else {
                    dueDate = t.dueDate != null ? t.dueDate : t.date;
                    invoiceKey = "MANUAL_" + t.id;
                }
            } else {
                // Use the new dueDate field if available, fallback to transaction date
                dueDate = t.dueDate != null ? t.dueDate : t.date;
                invoiceKey = "MANUAL_" + t.id;
            }

            boolean matches;
            if (frequency <= 0) {
                matches = dueDate >= startOfTargetDay && dueDate <= endOfTargetDay;
            } else {
                matches = dueDate <= endOfToday;
            }

            if (matches) {
                if (!countedInvoices.contains(invoiceKey)) {
                    unpaidCount++;
                    countedInvoices.add(invoiceKey);
                }
                totalUnpaid += t.amount;
            }
        }

        if (unpaidCount > 0) {
            String subject = (unpaidCount == 1) ? "pagamento pendente" : "pagamentos pendentes";

            if (frequency == 1) {
                title = "Contas Pendentes";
                content = "Você tem " + unpaidCount + " " + subject + " para hoje ou atrasados.";
            } else if (frequency == 0) {
                title = "Vencimento Hoje";
                content = "Você tem " + unpaidCount + " " + subject + " para hoje.";
            } else if (frequency == -1) {
                title = "Vencimento Amanhã";
                content = "Você tem " + unpaidCount + " " + subject + " para amanhã.";
            } else if (frequency == -7) {
                title = "Vencimento em 1 Semana";
                content = "Você tem " + unpaidCount + " " + subject + " para daqui a uma semana.";
            } else {
                content = "Você tem " + unpaidCount + " " + subject + " identificados.";
            }
            sendNotification(title, content);
        }

        return Result.success();
    }

    private void sendNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Lembretes de Pagamento", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(android.graphics.Color.parseColor("#D5CDCD"))
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }
}
