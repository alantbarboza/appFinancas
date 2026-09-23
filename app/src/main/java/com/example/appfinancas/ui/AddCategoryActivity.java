package com.example.appfinancas.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Category;

public class AddCategoryActivity extends AppCompatActivity {
    private EditText editName;
    private TextView textPreview;
    private CardView cardPreview;
    private String selectedColor = "#2196F3";
    private String oldName = null;
    private int categoryId = -1;

    private String[] presetColors = {
        "#A54BFF", "#2196F3", "#FFD54F", "#FFB74D", "#03A9F4", 
        "#7986CB", "#A1887F", "#90A4AE", "#E1BEE7", "#FFCCBC", "#FFF9C4",
        "#D1C4E9", "#CFD8DC", "#F5F5F5", "#80DEEA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        editName = findViewById(R.id.edit_category_name);
        textPreview = findViewById(R.id.text_preview);
        cardPreview = findViewById(R.id.card_preview);
        LinearLayout layoutColors = findViewById(R.id.layout_colors);
        Button btnSave = findViewById(R.id.btn_save_category);

        for (String colorHex : presetColors) {
            View view = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(16, 16, 16, 16);
            view.setLayoutParams(params);
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(colorHex));
            view.setBackground(shape);
            
            view.setOnClickListener(v -> {
                selectedColor = colorHex;
                updatePreview();
            });
            layoutColors.addView(view);
        }

        checkIntent();
        updatePreview();

        btnSave.setOnClickListener(v -> saveCategory());
    }

    private void checkIntent() {
        if (getIntent().hasExtra("id")) {
            categoryId = getIntent().getIntExtra("id", -1);
            oldName = getIntent().getStringExtra("name");
            editName.setText(oldName);
            selectedColor = getIntent().getStringExtra("color");
            ((TextView)findViewById(R.id.text_add_category_title)).setText("Editar Categoria");
        }
    }

    private void updatePreview() {
        int color = Color.parseColor(selectedColor);
        cardPreview.setCardBackgroundColor(color);
        if (isColorLight(color)) {
            textPreview.setTextColor(Color.BLACK);
        } else {
            textPreview.setTextColor(Color.WHITE);
        }
    }

    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    private void saveCategory() {
        String name = editName.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this, "Informe o nome", Toast.LENGTH_SHORT).show();
            return;
        }

        Category category = new Category(name, "ic_category", selectedColor);
        if (categoryId != -1) category.id = categoryId;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            if (categoryId == -1) {
                db.categoryDao().insert(category);
            } else {
                db.categoryDao().update(category);
                // Update all transactions that had the old name
                if (oldName != null && !oldName.equals(name)) {
                    db.transactionDao().updateCategoryName(oldName, name);
                }
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Categoria salva!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
