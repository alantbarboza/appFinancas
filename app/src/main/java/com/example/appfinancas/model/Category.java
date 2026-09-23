package com.example.appfinancas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public String icon; // Resource name or emoji
    public String color; // Hex string

    public Category() {}

    public Category(String name, String icon, String color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }
}
