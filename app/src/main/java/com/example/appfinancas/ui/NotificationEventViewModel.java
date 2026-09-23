package com.example.appfinancas.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.NotificationEvent;
import com.example.appfinancas.model.Transaction;

import java.util.List;

public class NotificationEventViewModel extends AndroidViewModel {
    private final LiveData<List<NotificationEvent>> allEvents;
    private final AppDatabase db;

    public NotificationEventViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getDatabase(application);
        allEvents = db.notificationEventDao().getAllEvents();
    }

    public LiveData<List<NotificationEvent>> getAllEvents() {
        return allEvents;
    }

    public void processEvent(NotificationEvent event, String category) {
        final String finalCategory = ("Outros".equals(category)) ? null : category;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String desc = event.detectedType.equals("RECEITA") ? "Receita" : "Despesa";
            // Do not use event.title as it usually contains the bank name
            // Use the detected type as description to match manual entry behavior

            // Create transaction updated with 17 parameters (including dueDate)
            long now = System.currentTimeMillis();
            Transaction transaction = new Transaction(
                    event.detectedType,
                    event.detectedAmount,
                    desc,
                    finalCategory,
                    now,
                    "Notificação",
                    "NOTIFICACAO",
                    null,
                    getBankName(event.packageName),
                    false,
                    1,
                    true,
                    false,
                    null,
                    null,
                    null,
                    now
            );
            db.transactionDao().insert(transaction);

            // Mark event as processed
            event.processed = true;
            db.notificationEventDao().update(event);
        });
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
}
