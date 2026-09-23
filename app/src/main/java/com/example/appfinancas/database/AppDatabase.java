package com.example.appfinancas.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.appfinancas.dao.CardDao;
import com.example.appfinancas.dao.CategoryDao;
import com.example.appfinancas.dao.NotificationEventDao;
import com.example.appfinancas.dao.TransactionDao;
import com.example.appfinancas.model.Card;
import com.example.appfinancas.model.Category;
import com.example.appfinancas.model.NotificationEvent;
import com.example.appfinancas.model.Transaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Transaction.class, Card.class, Category.class, NotificationEvent.class}, version = 8)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();
    public abstract CardDao cardDao();
    public abstract CategoryDao categoryDao();
    public abstract NotificationEventDao notificationEventDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "finance_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
