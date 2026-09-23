package com.example.appfinancas.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appfinancas.model.NotificationEvent;

import java.util.List;

@Dao
public interface NotificationEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(NotificationEvent event);

    @Update
    void update(NotificationEvent event);

    @Delete
    void delete(NotificationEvent event);

    @Query("SELECT * FROM notification_events WHERE notificationHash = :hash LIMIT 1")
    NotificationEvent getByHash(String hash);

    @Query("SELECT * FROM notification_events ORDER BY createdAt DESC")
    LiveData<List<NotificationEvent>> getAllEvents();

    @Query("SELECT * FROM notification_events WHERE processed = 0 ORDER BY createdAt DESC")
    LiveData<List<NotificationEvent>> getUnprocessedEvents();

    @Query("DELETE FROM notification_events")
    void deleteAll();
}
