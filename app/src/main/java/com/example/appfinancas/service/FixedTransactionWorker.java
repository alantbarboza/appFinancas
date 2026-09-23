package com.example.appfinancas.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Transaction;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixedTransactionWorker extends Worker {

    public FixedTransactionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        List<Transaction> fixedTransactions = db.transactionDao().getActiveFixedTransactions();
        
        if (fixedTransactions.isEmpty()) return Result.success();

        // Group by recurringId and find the latest transaction for each
        Map<String, Transaction> latestTransactions = new HashMap<>();
        for (Transaction t : fixedTransactions) {
            String rid = t.recurringId;
            if (!latestTransactions.containsKey(rid) || t.date > latestTransactions.get(rid).date) {
                latestTransactions.put(rid, t);
            }
        }

        long elevenMonthsFromNow = getFutureTimestamp(11);

        for (Transaction latest : latestTransactions.values()) {
            if (latest.date < elevenMonthsFromNow) {
                // Generate more months until we have coverage for at least 12 months from now
                extendFixedTransaction(db, latest);
            }
        }

        return Result.success();
    }

    private long getFutureTimestamp(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, months);
        return cal.getTimeInMillis();
    }

    private void extendFixedTransaction(AppDatabase db, Transaction latest) {
        // Goal: ensure there are transactions up to 12 months from today
        long targetDate = getFutureTimestamp(12);
        long currentDate = latest.date;
        
        int monthCount = 1;
        while (currentDate < targetDate) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(latest.date);
            cal.add(Calendar.MONTH, monthCount);
            currentDate = cal.getTimeInMillis();

            Integer nextInvM = null;
            Integer nextInvY = null;
            if (latest.invoiceMonth != null && latest.invoiceYear != null) {
                nextInvM = latest.invoiceMonth + monthCount;
                nextInvY = latest.invoiceYear;
                while (nextInvM > 11) { nextInvM -= 12; nextInvY++; }
            }

            Long nextDueDate = null;
            if (latest.dueDate != null) {
                Calendar dCal = Calendar.getInstance();
                dCal.setTimeInMillis(latest.dueDate);
                dCal.add(Calendar.MONTH, monthCount);
                nextDueDate = dCal.getTimeInMillis();
            }

            Transaction next = new Transaction(
                    latest.type,
                    latest.amount,
                    latest.description,
                    latest.category,
                    currentDate,
                    latest.paymentMethod,
                    "AUTO_GEN",
                    latest.cardId,
                    latest.bankName,
                    latest.isCard,
                    1,
                    false, // Always unpaid/unfaturado
                    true,
                    latest.recurringId,
                    nextInvM,
                    nextInvY,
                    nextDueDate
            );
            db.transactionDao().insert(next);
            monthCount++;
            
            // Safety break to prevent infinite loops (shouldn't happen with month addition)
            if (monthCount > 24) break; 
        }
    }
}
