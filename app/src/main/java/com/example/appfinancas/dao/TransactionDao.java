package com.example.appfinancas.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appfinancas.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Update
    void updateAll(List<Transaction> transactions);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactionsList();

    @Query("SELECT * FROM transactions WHERE isFixed = 1 AND recurringId IS NOT NULL")
    List<Transaction> getActiveFixedTransactions();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'RECEITA'")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DESPESA'")
    LiveData<Double> getTotalExpense();

    @Query("SELECT * FROM transactions WHERE cardId = :cardId")
    LiveData<List<Transaction>> getTransactionsByCard(int cardId);

    @Query("SELECT * FROM transactions WHERE recurringId = :recId")
    List<Transaction> getTransactionsByRecurringId(String recId);

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    void updateCategoryName(String oldCategory, String newCategory);

    @Query("UPDATE transactions SET category = NULL WHERE category = 'Outros'")
    void cleanOutrosCategory();

    @Query("UPDATE transactions SET description = :desc, amount = :amount, category = :cat, type = :type WHERE recurringId = :recId")
    void updateRecurringTransactions(String recId, String desc, double amount, String cat, String type);

    @Query("UPDATE transactions SET bankName = :newName WHERE cardId = :cardId")
    void updateBankName(int cardId, String newName);

    @Query("DELETE FROM transactions WHERE recurringId = :recId")
    void deleteRecurringTransactions(String recId);
}
