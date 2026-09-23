package com.example.appfinancas.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CategoryListActivity extends AppCompatActivity {
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter();
        
        adapter.setListener(new CategoryAdapter.OnCategoryListener() {
            @Override
            public void onEdit(Category category) {
                Intent intent = new Intent(CategoryListActivity.this, AddCategoryActivity.class);
                intent.putExtra("id", category.id);
                intent.putExtra("name", category.name);
                intent.putExtra("color", category.color);
                startActivity(intent);
            }

            @Override
            public void onDelete(Category category) {
                new AlertDialog.Builder(CategoryListActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setMessage("Deseja excluir esta categoria? As transações vinculadas ficarão sem categoria.")
                        .setPositiveButton("Sim", (dialog, which) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                AppDatabase db = AppDatabase.getDatabase(CategoryListActivity.this);
                                // Cascade: set category to null in transactions
                                db.transactionDao().updateCategoryName(category.name, null);
                                db.categoryDao().delete(category);
                            });
                        })
                        .setNegativeButton("Não", null)
                        .show();
            }
        });
        
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_category);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, AddCategoryActivity.class));
        });

        AppDatabase.getDatabase(this).categoryDao().getAllCategories().observe(this, categories -> {
            adapter.setCategories(categories);
        });
    }
}
