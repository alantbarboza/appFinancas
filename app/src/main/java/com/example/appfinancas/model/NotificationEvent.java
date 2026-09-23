package com.example.appfinancas.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "notification_events", indices = {@Index(value = {"notificationHash"}, unique = true)})
public class NotificationEvent {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String packageName;
    public String title;
    public String text;
    public String notificationHash; // To avoid duplicates
    public String detectedType; // RECEITA or DESPESA
    public double detectedAmount;
    public boolean processed;
    public long createdAt;

    public NotificationEvent() {}

    public NotificationEvent(String packageName, String title, String text, String notificationHash, String detectedType, double detectedAmount, boolean processed, long createdAt) {
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.notificationHash = notificationHash;
        this.detectedType = detectedType;
        this.detectedAmount = detectedAmount;
        this.processed = processed;
        this.createdAt = createdAt;
    }
}
