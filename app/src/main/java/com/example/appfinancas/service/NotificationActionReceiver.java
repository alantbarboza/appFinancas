package com.example.appfinancas.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Transaction;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_CONFIRM = "com.example.appfinancas.ACTION_CONFIRM";
    public static final String ACTION_IGNORE = "com.example.appfinancas.ACTION_IGNORE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notificationId", 0);
        
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);

        if (ACTION_CONFIRM.equals(action)) {
            String type = intent.getStringExtra("type");
            double amount = intent.getDoubleExtra("amount", 0);
            String desc = intent.getStringExtra("description");
            String bankName = intent.getStringExtra("bankName");

            // Mandatory description check for notifications
            if (desc == null || desc.trim().isEmpty()) {
                desc = "RECEITA".equals(type) ? "Receita" : "Despesa";
            }

            // Updated with 17 parameters to match the new Transaction model
            // Set category to null instead of "Outros" to be "Sem Categoria"
            long now = System.currentTimeMillis();
            Transaction transaction = new Transaction(
                    type, amount, desc, null,
                    now, "Notificação", "NOTIFICACAO", null,
                    bankName, false, 1, true, false, null, null, null, now
            );

            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getDatabase(context).transactionDao().insert(transaction);
            });
            
            Toast.makeText(context, "Lançamento confirmado!", Toast.LENGTH_SHORT).show();
        } else if (ACTION_IGNORE.equals(action)) {
            String hash = intent.getStringExtra("hash");
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(context);
                com.example.appfinancas.model.NotificationEvent event = db.notificationEventDao().getByHash(hash);
                if (event != null) {
                    db.notificationEventDao().delete(event);
                }
            });
            Toast.makeText(context, "Lançamento ignorado.", Toast.LENGTH_SHORT).show();
        }
    }
}
