package com.example.appfinancas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cards")
public class Card {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public double limit;
    public int closingDay;
    public int dueDay;
    public String color; // Hex string

    public Card() {}

    public Card(String name, double limit, int closingDay, int dueDay, String color) {
        this.name = name;
        this.limit = limit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.color = color;
    }
}
