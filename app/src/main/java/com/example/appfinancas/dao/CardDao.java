package com.example.appfinancas.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appfinancas.model.Card;

import java.util.List;

@Dao
public interface CardDao {
    @Insert
    void insert(Card card);

    @Update
    void update(Card card);

    @Delete
    void delete(Card card);

    @Query("SELECT * FROM cards")
    LiveData<List<Card>> getAllCards();

    @Query("SELECT * FROM cards")
    List<Card> getAllCardsList();
}
