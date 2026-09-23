package com.example.appfinancas.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;

public class MoneyTextWatcher implements TextWatcher {
    private final WeakReference<EditText> editTextWeakReference;
    private String current = "";

    public MoneyTextWatcher(EditText editText) {
        this.editTextWeakReference = new WeakReference<>(editText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        EditText editText = editTextWeakReference.get();
        if (editText == null) return;

        if (!s.toString().equals(current)) {
            editText.removeTextChangedListener(this);

            String cleanString = s.toString().replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) cleanString = "0";
            
            double parsed = Double.parseDouble(cleanString) / 100;
            String formatted = CurrencyUtils.formatPlain(parsed);

            current = formatted;
            editText.setText(formatted);
            editText.setSelection(formatted.length());

            editText.addTextChangedListener(this);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}
    
    public static double parseValue(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            String clean = value.replaceAll("[^\\d]", "");
            if (clean.isEmpty()) return 0;
            return Double.parseDouble(clean) / 100;
        } catch (Exception e) {
            return 0;
        }
    }
}
