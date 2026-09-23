package com.example.appfinancas.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Transaction;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    private final LiveData<Double> totalIncome;
    private final LiveData<Double> totalExpense;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<com.example.appfinancas.model.Card>> allCards;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        totalIncome = db.transactionDao().getTotalIncome();
        totalExpense = db.transactionDao().getTotalExpense();
        allTransactions = db.transactionDao().getAllTransactions();
        allCards = db.cardDao().getAllCards();
    }

    public LiveData<List<com.example.appfinancas.model.Card>> getAllCards() {
        return allCards;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }
}
