package com.example.appfinancas.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Card;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CardListActivity extends AppCompatActivity {
    private CardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_cards);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CardAdapter();
        
        adapter.setListener(new CardAdapter.OnCardListener() {
            @Override
            public void onEdit(Card card) {
                Intent intent = new Intent(CardListActivity.this, AddCardActivity.class);
                intent.putExtra("id", card.id);
                intent.putExtra("name", card.name);
                intent.putExtra("limit", card.limit);
                intent.putExtra("closingDay", card.closingDay);
                intent.putExtra("dueDay", card.dueDay);
                intent.putExtra("color", card.color);
                startActivity(intent);
            }

            @Override
            public void onDelete(Card card) {
                new AlertDialog.Builder(CardListActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setMessage("Deseja excluir este cartão?")
                        .setPositiveButton("Sim", (dialog, which) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                AppDatabase.getDatabase(CardListActivity.this).cardDao().delete(card);
                            });
                        })
                        .setNegativeButton("Não", null)
                        .show();
            }
        });
        
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_card);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, AddCardActivity.class));
        });

        AppDatabase.getDatabase(this).cardDao().getAllCards().observe(this, cards -> {
            adapter.setCards(cards);
        });
    }
}
