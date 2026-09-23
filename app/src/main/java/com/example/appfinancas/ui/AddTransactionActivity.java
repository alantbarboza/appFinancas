package com.example.appfinancas.ui;

import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Card;
import com.example.appfinancas.model.Transaction;
import com.example.appfinancas.util.CurrencyUtils;
import com.example.appfinancas.util.MoneyTextWatcher;

public class AddTransactionActivity extends AppCompatActivity {
    private EditText editAmount, editDescription, editInstallments;
    private RadioGroup radioGroupType;
    private Spinner spinnerCategory, spinnerCard, spinnerInvoice;
    private SwitchCompat switchIsCard, switchIsPaid, switchIsFixed;
    private View layoutInstallments, layoutCardOptions;
    private TextView textNoCardWarning;
    private Button btnSave, btnPickDate, btnPickDueDate, btnDelete;
    private ImageButton btnAddCategoryQuick;
    private int transactionId = -1;
    private String recurringId = null;
    private long selectedDate = System.currentTimeMillis();
    private Long selectedDueDate = null;
    
    private List<Card> allCards = new ArrayList<>();
    private List<InvoiceOption> invoiceOptions = new ArrayList<>();

    private static class InvoiceOption {
        int month;
        int year;
        String label;
        InvoiceOption(int m, int y, String l) { month = m; year = y; label = l; }
        @Override public String toString() { return label; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        editAmount = findViewById(R.id.edit_amount);
        editDescription = findViewById(R.id.edit_description);
        editInstallments = findViewById(R.id.edit_installments);
        radioGroupType = findViewById(R.id.radio_group_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerCard = findViewById(R.id.spinner_card);
        spinnerInvoice = findViewById(R.id.spinner_invoice);
        switchIsCard = findViewById(R.id.switch_is_card);
        switchIsPaid = findViewById(R.id.switch_is_paid);
        switchIsFixed = findViewById(R.id.switch_is_fixed);
        layoutInstallments = findViewById(R.id.layout_installments);
        layoutCardOptions = findViewById(R.id.layout_card_options);
        textNoCardWarning = findViewById(R.id.text_no_card_warning);
        btnSave = findViewById(R.id.btn_save);
        btnPickDate = findViewById(R.id.btn_pick_date);
        btnPickDueDate = findViewById(R.id.btn_pick_due_date);
        btnDelete = findViewById(R.id.btn_delete);
        btnAddCategoryQuick = findViewById(R.id.btn_add_category_quick);

        setupCurrencyMask();
        setupDescriptionWatcher();
        loadCategories();
        loadCards();
        setupInvoiceOptions();

        switchIsCard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutInstallments.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            layoutCardOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                switchIsFixed.setChecked(false);
                if (transactionId == -1) applySmartDefaultInvoice();
            }
            updateSaveButtonState();
        });

        switchIsFixed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchIsCard.setChecked(false);
                layoutInstallments.setVisibility(View.GONE);
                layoutCardOptions.setVisibility(View.GONE);
            }
            updateSaveButtonState();
        });

        spinnerCard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { 
                if (transactionId == -1) applySmartDefaultInvoice(); 
                updateSaveButtonState();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                updateSaveButtonState();
            }
        });

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickDueDate.setOnClickListener(v -> showDueDatePicker());

        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isExpense = checkedId == R.id.radio_expense;
            switchIsCard.setVisibility(isExpense ? View.VISIBLE : View.GONE);
            switchIsFixed.setVisibility(isExpense ? View.VISIBLE : View.GONE);
            updateDueDateVisibility();
            if (!isExpense) {
                switchIsCard.setChecked(false);
                switchIsFixed.setChecked(false);
                layoutInstallments.setVisibility(View.GONE);
                layoutCardOptions.setVisibility(View.GONE);
            }
        });

        switchIsCard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutInstallments.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            layoutCardOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateDueDateVisibility();
            if (isChecked) {
                switchIsFixed.setChecked(false);
                if (transactionId == -1) applySmartDefaultInvoice();
            }
            updateSaveButtonState();
        });

        boolean isExpense = radioGroupType.getCheckedRadioButtonId() == R.id.radio_expense;
        switchIsCard.setVisibility(isExpense ? View.VISIBLE : View.GONE);
        switchIsFixed.setVisibility(isExpense ? View.VISIBLE : View.GONE);
        updateDueDateVisibility();

        checkIntent();
        updateDateButton();
        updateDueDateButton();
        updateSaveButtonState();

        btnSave.setOnClickListener(v -> {
            if (transactionId != -1 && recurringId != null) {
                new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setMessage("Deseja atualizar todas as parcelas/lançamentos futuros deste registro?")
                        .setPositiveButton("Sim, todos", (dialog, which) -> saveTransaction(true))
                        .setNegativeButton("Não, apenas este", (dialog, which) -> saveTransaction(false))
                        .show();
            } else {
                saveTransaction(false);
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnAddCategoryQuick.setOnClickListener(v -> showQuickAddCategoryDialog());
    }

    private void showQuickAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quick_category, null);
        EditText editName = dialogView.findViewById(R.id.edit_category_name);
        LinearLayout layoutColors = dialogView.findViewById(R.id.layout_colors);
        
        final String[] selectedColor = {"#2196F3"};
        String[] presetColors = {
            "#A54BFF", "#2196F3", "#FFD54F", "#FFB74D", "#03A9F4", 
            "#7986CB", "#A1887F", "#90A4AE", "#E1BEE7", "#FFCCBC", "#FFF9C4",
            "#D1C4E9", "#CFD8DC", "#F5F5F5", "#80DEEA"
        };

        for (String colorHex : presetColors) {
            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(colorHex));
            colorView.setBackground(shape);
            
            colorView.setOnClickListener(v -> {
                selectedColor[0] = colorHex;
                for (int i = 0; i < layoutColors.getChildCount(); i++) {
                    layoutColors.getChildAt(i).setAlpha(0.5f);
                }
                v.setAlpha(1.0f);
            });
            layoutColors.addView(colorView);
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        com.example.appfinancas.model.Category newCat = new com.example.appfinancas.model.Category(name, "ic_category", selectedColor[0]);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getDatabase(this).categoryDao().insert(newCat);
                            runOnUiThread(() -> Toast.makeText(this, "Categoria adicionada!", Toast.LENGTH_SHORT).show());
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Tem certeza que deseja excluir este registro?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        Transaction t = new Transaction();
                        t.id = transactionId;
                        AppDatabase.getDatabase(this).transactionDao().delete(t);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupCurrencyMask() {
        editAmount.addTextChangedListener(new MoneyTextWatcher(editAmount) {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                super.onTextChanged(s, start, before, count);
                updateSaveButtonState();
            }
        });
    }

    private void setupDescriptionWatcher() {
        editDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    private void updateSaveButtonState() {
        double amount = MoneyTextWatcher.parseValue(editAmount.getText().toString());
        String desc = editDescription.getText().toString().trim();
        
        boolean isCard = switchIsCard.isChecked();
        boolean hasCardSelected = spinnerCard != null && spinnerCard.getSelectedItem() != null;
        
        boolean isValid = amount > 0 && !desc.isEmpty();
        if (isCard) {
            isValid = isValid && hasCardSelected;
        }
        
        btnSave.setEnabled(isValid);
    }

    private void setupInvoiceOptions() {
        invoiceOptions.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM, yyyy", new Locale("pt", "BR"));
        for (int i = 0; i < 24; i++) {
            String label = sdf.format(cal.getTime());
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            invoiceOptions.add(new InvoiceOption(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), label));
            cal.add(Calendar.MONTH, 1);
        }
        ArrayAdapter<InvoiceOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, invoiceOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInvoice.setAdapter(adapter);
    }

    private void loadCards() {
        AppDatabase.getDatabase(this).cardDao().getAllCards().observe(this, cards -> {
            allCards = cards;
            
            boolean hasCards = !cards.isEmpty();
            spinnerCard.setVisibility(hasCards ? View.VISIBLE : View.GONE);
            textNoCardWarning.setVisibility(hasCards ? View.GONE : View.VISIBLE);

            List<String> cardNames = new ArrayList<>();
            for (Card c : cards) cardNames.add(c.name);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cardNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCard.setAdapter(adapter);
            
            if (getIntent().hasExtra("cardId")) {
                int cardId = getIntent().getIntExtra("cardId", -1);
                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).id == cardId) {
                        spinnerCard.setSelection(i);
                        break;
                    }
                }
            }
            if (transactionId == -1) applySmartDefaultInvoice();
        });
    }

    private void applySmartDefaultInvoice() {
        if (!switchIsCard.isChecked() || allCards.isEmpty() || spinnerCard.getSelectedItem() == null) return;
        
        Card selectedCard = allCards.get(spinnerCard.getSelectedItemPosition());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);

        if (day > selectedCard.closingDay) {
            month++;
            if (month > 11) { month = 0; year++; }
        }

        for (int i = 0; i < invoiceOptions.size(); i++) {
            InvoiceOption opt = invoiceOptions.get(i);
            if (opt.month == month && opt.year == year) {
                spinnerInvoice.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDate = cal.getTimeInMillis();
            
            // Auto-set due date to same as purchase date if it was null
            if (selectedDueDate == null) {
                selectedDueDate = selectedDate;
                updateDueDateButton();
            }
            
            updateDateButton();
            if (transactionId == -1) applySmartDefaultInvoice();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDueDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDueDate != null) cal.setTimeInMillis(selectedDueDate);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDueDate = cal.getTimeInMillis();
            updateDueDateButton();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        btnPickDate.setText("📅 Data Compra: " + sdf.format(selectedDate));
    }

    private void updateDueDateButton() {
        if (selectedDueDate == null) {
            btnPickDueDate.setText("🚨 Vencimento: --/--/----");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            btnPickDueDate.setText("🚨 Vencimento: " + sdf.format(selectedDueDate));
        }
    }

    private void updateDueDateVisibility() {
        boolean isExpense = radioGroupType.getCheckedRadioButtonId() == R.id.radio_expense;
        boolean isCard = switchIsCard.isChecked();
        btnPickDueDate.setVisibility((isExpense && !isCard) ? View.VISIBLE : View.GONE);
    }

    private void loadCategories() {
        AppDatabase.getDatabase(this).categoryDao().getAllCategories().observe(this, categoriesList -> {
            List<String> names = new ArrayList<>();
            names.add("Sem Categoria");
            if (categoriesList != null) {
                for (com.example.appfinancas.model.Category c : categoriesList) names.add(c.name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
            String prefilledCat = getIntent().getStringExtra("category");
            if (prefilledCat != null) {
                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).equals(prefilledCat)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void checkIntent() {
        Intent intent = getIntent();
        if (intent == null) return;
        if (intent.hasExtra("id")) {
            transactionId = intent.getIntExtra("id", -1);
            ((TextView)findViewById(R.id.text_title)).setText("Editar Lançamento");
            recurringId = intent.getStringExtra("recurringId");
            btnDelete.setVisibility(View.VISIBLE);
        }
        if (intent.hasExtra("amount")) {
            double amount = intent.getDoubleExtra("amount", 0);
            editAmount.setText(CurrencyUtils.formatPlain(amount));
        }
        if (intent.hasExtra("description")) {
            String desc = intent.getStringExtra("description");
            if (desc != null && desc.contains("/")) desc = desc.replaceAll(" \\d+/\\d+$", "");
            editDescription.setText(desc);
        }
        updateSaveButtonState();
        if (intent.hasExtra("type")) {
            if ("RECEITA".equals(intent.getStringExtra("type"))) radioGroupType.check(R.id.radio_income);
            else radioGroupType.check(R.id.radio_expense);
        }
        if (intent.hasExtra("isCard")) switchIsCard.setChecked(intent.getBooleanExtra("isCard", false));
        if (intent.hasExtra("installments")) editInstallments.setText(String.valueOf(intent.getIntExtra("installments", 1)));
        if (intent.hasExtra("isPaid")) {
            switchIsPaid.setChecked(intent.getBooleanExtra("isPaid", false));
        } else {
            // Default logic: Always checked for new Normal Expenses and Income
            switchIsPaid.setChecked(true);
        }
        if (intent.hasExtra("isFixed")) switchIsFixed.setChecked(intent.getBooleanExtra("isFixed", false));
        if (intent.hasExtra("date")) selectedDate = intent.getLongExtra("date", System.currentTimeMillis());
        if (intent.hasExtra("dueDate")) {
            selectedDueDate = intent.getLongExtra("dueDate", 0);
            if (selectedDueDate == 0) selectedDueDate = null;
        }
        updateDueDateButton();
        updateDueDateVisibility();
        
        if (intent.hasExtra("invoiceMonth")) {
            int im = intent.getIntExtra("invoiceMonth", -1);
            int iy = intent.getIntExtra("invoiceYear", -1);
            for (int i = 0; i < invoiceOptions.size(); i++) {
                if (invoiceOptions.get(i).month == im && invoiceOptions.get(i).year == iy) {
                    spinnerInvoice.setSelection(i);
                    break;
                }
            }
        }

        if (intent.hasExtra("notificationId")) {
            int notificationId = intent.getIntExtra("notificationId", 0);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
        }
    }

    private void saveTransaction(boolean updateAllRecurring) {
        String amountStr = editAmount.getText().toString();
        double amount = MoneyTextWatcher.parseValue(amountStr);
        String description = editDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        if ("Sem Categoria".equals(category)) category = null;
        
        if (description.isEmpty()) {
            Toast.makeText(this, "A descrição é obrigatória!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount == 0) {
            Toast.makeText(this, "Informe um valor maior que zero", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = radioGroupType.getCheckedRadioButtonId() == R.id.radio_income ? "RECEITA" : "DESPESA";
        boolean isCard = switchIsCard.isChecked();
        boolean isPaid = switchIsPaid.isChecked();
        boolean isFixed = switchIsFixed.isChecked();
        int installments = 1;
        try { installments = Integer.parseInt(editInstallments.getText().toString()); } catch (Exception ignored) {}

        Integer cardId = null;
        String bankName = "Manual";
        Integer startMonth = null;
        Integer startYear = null;

        if (isCard && !allCards.isEmpty()) {
            Card selectedCard = allCards.get(spinnerCard.getSelectedItemPosition());
            cardId = selectedCard.id;
            bankName = selectedCard.name;
            InvoiceOption opt = (InvoiceOption) spinnerInvoice.getSelectedItem();
            if (opt != null) {
                startMonth = opt.month;
                startYear = opt.year;
            }
        }

        final int finalInstallments = installments;
        final Integer finalCardId = cardId;
        final String finalBank = bankName;
        final Integer finalStartM = startMonth;
        final Integer finalStartY = startYear;
        final String finalCategory = category;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            if (updateAllRecurring && recurringId != null) {
                List<Transaction> allRec = db.transactionDao().getTransactionsByRecurringId(recurringId);
                Transaction currentT = null;
                for (Transaction r : allRec) {
                    if (r.id == transactionId) {
                        currentT = r;
                        break;
                    }
                }

                for (int i = 0; i < allRec.size(); i++) {
                    Transaction recT = allRec.get(i);
                    String newDesc = description;
                    if (recT.description.contains("/")) {
                        String suffix = recT.description.substring(recT.description.lastIndexOf(" "));
                        newDesc += suffix;
                    }
                    recT.description = newDesc;
                    recT.amount = amount;
                    recT.category = finalCategory;
                    recT.type = type;
                    recT.cardId = finalCardId;
                    recT.bankName = finalBank;
                    recT.isPaid = isPaid; 
                    recT.isFixed = isFixed; 
                    recT.isCard = isCard;
                    recT.paymentMethod = isCard ? "Cartão" : "Manual";
                    recT.origin = "EDIT";

                    if (isCard && finalStartM != null && currentT != null) {
                        int currentInstallmentNum = 1;
                        if (currentT.description.contains("/")) {
                            try {
                                String part = currentT.description.substring(currentT.description.lastIndexOf(" ") + 1);
                                currentInstallmentNum = Integer.parseInt(part.split("/")[0]);
                            } catch (Exception ignored) {}
                        }
                        int thisInstallmentNum = 1;
                        if (recT.description.contains("/")) {
                            try {
                                String part = recT.description.substring(recT.description.lastIndexOf(" ") + 1);
                                thisInstallmentNum = Integer.parseInt(part.split("/")[0]);
                            } catch (Exception ignored) {}
                        }
                        int monthDiff = thisInstallmentNum - currentInstallmentNum;
                        int newM = finalStartM + monthDiff;
                        int newY = finalStartY;
                        while (newM > 11) { newM -= 12; newY++; }
                        while (newM < 0) { newM += 12; newY--; }
                        recT.invoiceMonth = newM;
                        recT.invoiceYear = newY;
                    } else if (!isCard) {
                        recT.invoiceMonth = null;
                        recT.invoiceYear = null;
                        recT.isCard = false;
                        
                        // Update due date for manual recurring expenses
                        if (selectedDueDate != null && currentT != null) {
                            int monthDiff = getMonthDifference(currentT.date, recT.date);
                            recT.dueDate = getDueDateForMonth(selectedDueDate, monthDiff);
                        }
                    }
                    db.transactionDao().update(recT);
                }
            } else if (transactionId == -1) {
                String newRecId = (finalInstallments > 1 || isFixed) ? UUID.randomUUID().toString() : null;
                int count = isFixed ? 12 : finalInstallments;
                for (int i = 1; i <= count; i++) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(selectedDate);
                    cal.add(Calendar.MONTH, i - 1);
                    Integer currentInvM = finalStartM;
                    Integer currentInvY = finalStartY;
                    if (currentInvM != null) {
                        currentInvM += (i - 1);
                        while (currentInvM > 11) { currentInvM -= 12; currentInvY++; }
                    }
                    String desc = description;
                    if (finalInstallments > 1) desc += " " + i + "/" + finalInstallments;
                    Transaction t = new Transaction(
                            type, amount / (finalInstallments > 1 ? finalInstallments : 1), desc, finalCategory,
                            cal.getTimeInMillis(), isCard ? "Cartão" : "Manual", "MANUAL", finalCardId,
                            finalBank, isCard, 1, i == 1 && isPaid, isFixed, newRecId, currentInvM, currentInvY,
                            selectedDueDate != null ? getDueDateForMonth(selectedDueDate, i - 1) : null
                    );
                    db.transactionDao().insert(t);
                }
            } else {
                // Check if we are converting a single transaction to Fixed or Installments
                if (recurringId == null && (finalInstallments > 1 || isFixed)) {
                    String newRecId = UUID.randomUUID().toString();
                    int count = isFixed ? 12 : finalInstallments;
                    double amountPerParcel = (finalInstallments > 1) ? amount / finalInstallments : amount;

                    // 1. Update the current transaction to be the first in the series
                    Transaction first = new Transaction(
                            type, amountPerParcel, description, finalCategory, selectedDate,
                            isCard ? "Cartão" : "Manual", "EDIT", finalCardId, finalBank, isCard, 1, isPaid, isFixed, newRecId, finalStartM, finalStartY,
                            selectedDueDate
                    );
                    first.id = transactionId;
                    db.transactionDao().update(first);

                    // 2. Create the remaining future transactions
                    for (int i = 2; i <= count; i++) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(selectedDate);
                        cal.add(Calendar.MONTH, i - 1);

                        Integer currentInvM = finalStartM;
                        Integer currentInvY = finalStartY;
                        if (currentInvM != null) {
                            currentInvM += (i - 1);
                            while (currentInvM > 11) { currentInvM -= 12; currentInvY++; }
                        }

                        String parcelDesc = description;
                        if (finalInstallments > 1) parcelDesc += " " + i + "/" + finalInstallments;

                        Transaction next = new Transaction(
                                type, amountPerParcel, parcelDesc, finalCategory, cal.getTimeInMillis(),
                                isCard ? "Cartão" : "Manual", "MANUAL", finalCardId, finalBank,
                                isCard, 1, false, isFixed, newRecId, currentInvM, currentInvY,
                                selectedDueDate != null ? getDueDateForMonth(selectedDueDate, i - 1) : null
                        );
                        db.transactionDao().insert(next);
                    }
                } else {
                    // Normal single update
                    Transaction t = new Transaction(
                            type, amount, description, finalCategory, selectedDate,
                            isCard ? "Cartão" : "Manual", "EDIT", finalCardId, finalBank, isCard, 1, isPaid, isFixed, recurringId, finalStartM, finalStartY,
                            selectedDueDate
                    );
                    t.id = transactionId;
                    db.transactionDao().update(t);
                }
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
                
                // If started from notification, go back to Home instead of just closing
                if (getIntent().hasExtra("notificationId") || isTaskRoot()) {
                    Intent mainIntent = new Intent(this, com.example.appfinancas.MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                }
                
                finish();
            });
        });
    }

    private Long getDueDateForMonth(long baseDueDate, int monthOffset) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseDueDate);
        cal.add(Calendar.MONTH, monthOffset);
        return cal.getTimeInMillis();
    }

    private int getMonthDifference(long date1, long date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(date2);
        
        int years = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
        int months = cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
        return years * 12 + months;
    }
}
