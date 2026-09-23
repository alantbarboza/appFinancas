package com.example.appfinancas.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.appfinancas.model.Card;
import com.example.appfinancas.util.CurrencyUtils;
import com.example.appfinancas.util.MoneyTextWatcher;

public class AddCardActivity extends AppCompatActivity {
    private EditText editName, editClosingDay, editDueDay;
    private CardView cardPreview;
    private TextView textPreview;
    private int cardId = -1;
    private String selectedColor = "#2196F3";

    private String[] presetColors = {
        "#A54BFF", "#2196F3", "#FFD54F", "#FFB74D", "#03A9F4", 
        "#7986CB", "#A1887F", "#90A4AE", "#E1BEE7", "#FFCCBC", "#FFF9C4",
        "#D1C4E9", "#CFD8DC", "#F5F5F5", "#80DEEA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        editName = findViewById(R.id.edit_card_name);
        editClosingDay = findViewById(R.id.edit_closing_day);
        editDueDay = findViewById(R.id.edit_due_day);
        cardPreview = findViewById(R.id.card_preview);
        textPreview = findViewById(R.id.text_preview_name);
        LinearLayout layoutColors = findViewById(R.id.layout_card_colors);
        Button btnSave = findViewById(R.id.btn_save_card);

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

        editName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                textPreview.setText(s.toString().isEmpty() ? "Pré-visualização" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (getIntent().hasExtra("id")) {
            cardId = getIntent().getIntExtra("id", -1);
            editName.setText(getIntent().getStringExtra("name"));
            editClosingDay.setText(String.valueOf(getIntent().getIntExtra("closingDay", 1)));
            editDueDay.setText(String.valueOf(getIntent().getIntExtra("dueDay", 10)));
            selectedColor = getIntent().getStringExtra("color");
            if (selectedColor == null) selectedColor = "#2196F3";
            ((TextView)findViewById(R.id.text_add_card_title)).setText("Editar Cartão");
        }

        updatePreview();
        btnSave.setOnClickListener(v -> saveCard());
    }

    private void saveCard() {
        String name = editName.getText().toString();
        String closingStr = editClosingDay.getText().toString();
        String dueStr = editDueDay.getText().toString();

        if (name.isEmpty() || closingStr.isEmpty() || dueStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double limit = 0.0;
        int closing = Integer.parseInt(closingStr);
        int due = Integer.parseInt(dueStr);

        if (closing < 1 || closing > 31 || due < 1 || due > 31) {
            Toast.makeText(this, "Informe dias válidos (1-31)", Toast.LENGTH_SHORT).show();
            return;
        }

        Card card = new Card(name, limit, closing, due, selectedColor);
        if (cardId != -1) card.id = cardId;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (cardId == -1) {
                AppDatabase.getDatabase(this).cardDao().insert(card);
            } else {
                AppDatabase db = AppDatabase.getDatabase(this);
                db.cardDao().update(card);
                // Update all transactions that were linked to this card's ID
                db.transactionDao().updateBankName(card.id, card.name);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Cartão salvo!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
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
}
