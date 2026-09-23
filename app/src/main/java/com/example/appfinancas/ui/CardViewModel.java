package com.example.appfinancas.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Card;

import java.util.List;

public class CardViewModel extends AndroidViewModel {
    private final LiveData<List<Card>> allCards;

    public CardViewModel(@NonNull Application application) {
        super(application);
        allCards = AppDatabase.getDatabase(application).cardDao().getAllCards();
    }

    public LiveData<List<Card>> getAllCards() {
        return allCards;
    }
}
