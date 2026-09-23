package com.example.appfinancas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.Transaction;
import com.example.appfinancas.util.CurrencyUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import android.widget.Button;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TransactionListFragment extends Fragment {
    private DashboardViewModel viewModel;
    private SharedFilterViewModel filterViewModel;
    private TransactionAdapter adapter;
    private TextView textFilterLabel, textSummaryLabel, textSummaryValue;
    private android.widget.EditText editSearch;
    private Button btnPayInvoice, btnUndoPayment;
    private LinearLayout layoutFilter;
    private TabLayout tabLayout;
    private List<Transaction> rawTransactions = new ArrayList<>();
    private List<com.example.appfinancas.model.Card> allCards = new ArrayList<>();
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_transactions, container, false);

        textFilterLabel = root.findViewById(R.id.text_filter_label);
        textSummaryLabel = root.findViewById(R.id.text_tab_summary_label);
        textSummaryValue = root.findViewById(R.id.text_tab_summary_value);
        btnPayInvoice = root.findViewById(R.id.btn_pay_invoice);
        btnUndoPayment = root.findViewById(R.id.btn_undo_payment);
        layoutFilter = root.findViewById(R.id.layout_filter_trigger);
        tabLayout = root.findViewById(R.id.tab_layout_transactions);
        editSearch = root.findViewById(R.id.edit_search_transactions);

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                filterAndDisplay();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Move view initialization here to avoid scope issues in onViewCreated
        RecyclerView recyclerView = root.findViewById(R.id.recycler_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter();
        
        adapter.setListener(new TransactionAdapter.OnTransactionListener() {
            @Override
            public void onEdit(Transaction t) {
                Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
                intent.putExtra("id", t.id);
                intent.putExtra("amount", t.amount);
                intent.putExtra("description", t.description);
                intent.putExtra("type", t.type);
                intent.putExtra("category", t.category);
                intent.putExtra("isCard", t.isCard);
                intent.putExtra("isPaid", t.isPaid);
                intent.putExtra("isFixed", t.isFixed);
                intent.putExtra("recurringId", t.recurringId);
                intent.putExtra("date", t.date);
                intent.putExtra("dueDate", t.dueDate != null ? t.dueDate : 0L);
                if (t.invoiceMonth != null) intent.putExtra("invoiceMonth", (int)t.invoiceMonth);
                if (t.invoiceYear != null) intent.putExtra("invoiceYear", (int)t.invoiceYear);
                startActivity(intent);
            }

            @Override
            public void onDelete(Transaction transaction) {
                String msg = "Deseja excluir este lançamento?";
                if (transaction.recurringId != null) {
                    msg = "Este é um lançamento recorrente/parcelado. Deseja excluir apenas este ou todos os futuros?";
                    new androidx.appcompat.app.AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setMessage(msg)
                            .setPositiveButton("Somente este", (dialog, which) -> deleteTransaction(transaction, false))
                            .setNeutralButton("Todos os futuros", (dialog, which) -> deleteTransaction(transaction, true))
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setMessage(msg)
                            .setPositiveButton("Sim", (dialog, which) -> deleteTransaction(transaction, false))
                            .setNegativeButton("Não", null)
                            .show();
                }
            }
        });
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey("tabIndex")) {
            int tabIndex = getArguments().getInt("tabIndex", 0);
            tabLayout.post(() -> {
                TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
                if (tab != null) {
                    tab.select();
                }
            });
            // Clear the argument so it doesn't persist
            getArguments().remove("tabIndex");
        }

        layoutFilter.setOnClickListener(v -> new FilterDialog().show(getChildFragmentManager(), "filter_dialog"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterAndDisplay(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_add_transaction);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
            startActivity(intent);
        });

        btnPayInvoice.setOnClickListener(v -> payCurrentInvoice());
        btnUndoPayment.setOnClickListener(v -> undoCurrentPayment());

        // Apply Window Insets for responsiveness
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            // Adjust header padding for status bar
            View header = view.findViewById(R.id.layout_filter_trigger).getParent() instanceof View ? (View)view.findViewById(R.id.layout_filter_trigger).getParent() : null;
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), top + 24, header.getPaddingRight(), header.getPaddingBottom());
            }

            // Adjust FAB margin for navigation bar
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            lp.bottomMargin = bottom + 64; // Base margin + insets
            fab.setLayoutParams(lp);

            return insets;
        });

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        filterViewModel = new ViewModelProvider(requireActivity()).get(SharedFilterViewModel.class);

        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            rawTransactions = transactions;
            filterAndDisplay();
        });

        AppDatabase.getDatabase(getContext()).categoryDao().getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.setCategories(categories);
            filterAndDisplay();
        });

        AppDatabase.getDatabase(getContext()).cardDao().getAllCards().observe(getViewLifecycleOwner(), cards -> {
            allCards = cards;
            adapter.setCards(cards);
            filterAndDisplay();
        });

        filterViewModel.getFilterState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                textFilterLabel.setText(state.label);
                filterAndDisplay();
            }
        });
    }

    private void deleteTransaction(Transaction t, boolean deleteAllRecurring) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            if (deleteAllRecurring && t.recurringId != null) {
                db.transactionDao().deleteRecurringTransactions(t.recurringId);
            } else {
                db.transactionDao().delete(t);
            }
        });
    }

    private void payCurrentInvoice() {
        if (filterViewModel.getFilterState().getValue() == null || allCards.isEmpty()) return;
        
        SharedFilterViewModel.FilterState state = filterViewModel.getFilterState().getValue();
        long start = state.start;
        
        Calendar fCal = Calendar.getInstance();
        fCal.setTimeInMillis(start);
        int selectedMonth = fCal.get(Calendar.MONTH);
        int selectedYear = fCal.get(Calendar.YEAR);

        // Find which cards have unpaid transactions for this month
        List<com.example.appfinancas.model.Card> cardsWithUnpaid = new ArrayList<>();
        for (com.example.appfinancas.model.Card card : allCards) {
            boolean hasUnpaid = false;
            for (Transaction t : rawTransactions) {
                if (t.isCard && !t.isPaid && t.cardId != null && t.cardId == card.id) {
                    int invM, invY;
                    if (t.invoiceMonth != null && t.invoiceYear != null) {
                        invM = t.invoiceMonth;
                        invY = t.invoiceYear;
                    } else {
                        Calendar tCal = Calendar.getInstance();
                        tCal.setTimeInMillis(t.date);
                        invM = tCal.get(Calendar.MONTH);
                        invY = tCal.get(Calendar.YEAR);
                        if (tCal.get(Calendar.DAY_OF_MONTH) > card.closingDay) {
                            invM++;
                            if (invM > 11) { invM = 0; invY++; }
                        }
                    }
                    if (invM == selectedMonth && invY == selectedYear) {
                        hasUnpaid = true;
                        break;
                    }
                }
            }
            if (hasUnpaid) cardsWithUnpaid.add(card);
        }

        if (cardsWithUnpaid.isEmpty()) return;

        if (cardsWithUnpaid.size() == 1) {
            confirmAndProcessPayment(cardsWithUnpaid.get(0), selectedMonth, selectedYear);
        } else {
            // Show selection dialog
            String[] cardNames = new String[cardsWithUnpaid.size()];
            for (int i = 0; i < cardsWithUnpaid.size(); i++) cardNames[i] = cardsWithUnpaid.get(i).name;

            new androidx.appcompat.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setItems(cardNames, (dialog, which) -> {
                        confirmAndProcessPayment(cardsWithUnpaid.get(which), selectedMonth, selectedYear);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void confirmAndProcessPayment(com.example.appfinancas.model.Card card, int month, int year) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Deseja marcar todas as transações de " + card.name + " deste mês como pagas?")
                .setPositiveButton("Sim, Pagar", (dialog, which) -> processPaymentForCard(card, month, year))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void undoCurrentPayment() {
        if (filterViewModel.getFilterState().getValue() == null || allCards.isEmpty()) return;
        
        SharedFilterViewModel.FilterState state = filterViewModel.getFilterState().getValue();
        long start = state.start;
        
        Calendar fCal = Calendar.getInstance();
        fCal.setTimeInMillis(start);
        int selectedMonth = fCal.get(Calendar.MONTH);
        int selectedYear = fCal.get(Calendar.YEAR);

        // Find which cards have ALL transactions paid for this month
        List<com.example.appfinancas.model.Card> cardsToUndo = new ArrayList<>();
        for (com.example.appfinancas.model.Card card : allCards) {
            boolean hasPaid = false;
            boolean hasUnpaid = false;
            for (Transaction t : rawTransactions) {
                if (t.isCard && t.cardId != null && t.cardId == card.id) {
                    int invM, invY;
                    if (t.invoiceMonth != null && t.invoiceYear != null) {
                        invM = t.invoiceMonth; invY = t.invoiceYear;
                    } else {
                        Calendar tCal = Calendar.getInstance();
                        tCal.setTimeInMillis(t.date);
                        invM = tCal.get(Calendar.MONTH); invY = tCal.get(Calendar.YEAR);
                        if (tCal.get(Calendar.DAY_OF_MONTH) > card.closingDay) {
                            invM++; if (invM > 11) { invM = 0; invY++; }
                        }
                    }
                    if (invM == selectedMonth && invY == selectedYear) {
                        if (t.isPaid) hasPaid = true;
                        else hasUnpaid = true;
                    }
                }
            }
            if (hasPaid && !hasUnpaid) cardsToUndo.add(card);
        }

        if (cardsToUndo.isEmpty()) return;

        if (cardsToUndo.size() == 1) {
            confirmAndUndoPayment(cardsToUndo.get(0), selectedMonth, selectedYear);
        } else {
            String[] cardNames = new String[cardsToUndo.size()];
            for (int i = 0; i < cardsToUndo.size(); i++) cardNames[i] = cardsToUndo.get(i).name;
            new androidx.appcompat.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setItems(cardNames, (dialog, which) -> confirmAndUndoPayment(cardsToUndo.get(which), selectedMonth, selectedYear))
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void confirmAndUndoPayment(com.example.appfinancas.model.Card card, int month, int year) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Deseja marcar todas as transações de " + card.name + " deste mês como pendentes?")
                .setPositiveButton("Sim, Desfazer", (dialog, which) -> revertPaymentForCard(card, month, year))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void revertPaymentForCard(com.example.appfinancas.model.Card card, int month, int year) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            List<Transaction> toUpdate = new ArrayList<>();
            for (Transaction t : rawTransactions) {
                if (t.isCard && t.isPaid && t.cardId != null && t.cardId == card.id) {
                    int invM, invY;
                    if (t.invoiceMonth != null && t.invoiceYear != null) {
                        invM = t.invoiceMonth; invY = t.invoiceYear;
                    } else {
                        Calendar tCal = Calendar.getInstance();
                        tCal.setTimeInMillis(t.date);
                        invM = tCal.get(Calendar.MONTH); invY = tCal.get(Calendar.YEAR);
                        if (tCal.get(Calendar.DAY_OF_MONTH) > card.closingDay) {
                            invM++; if (invM > 11) { invM = 0; invY++; }
                        }
                    }
                    if (invM == month && invY == year) {
                        t.isPaid = false;
                        toUpdate.add(t);
                    }
                }
            }
            if (!toUpdate.isEmpty()) {
                db.transactionDao().updateAll(toUpdate);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Fatura do " + card.name + " agora está pendente.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void processPaymentForCard(com.example.appfinancas.model.Card card, int month, int year) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            List<Transaction> toUpdate = new ArrayList<>();
            
            for (Transaction t : rawTransactions) {
                if (t.isCard && !t.isPaid && t.cardId != null && t.cardId == card.id) {
                    int invM, invY;
                    if (t.invoiceMonth != null && t.invoiceYear != null) {
                        invM = t.invoiceMonth;
                        invY = t.invoiceYear;
                    } else {
                        Calendar tCal = Calendar.getInstance();
                        tCal.setTimeInMillis(t.date);
                        invM = tCal.get(Calendar.MONTH);
                        invY = tCal.get(Calendar.YEAR);
                        if (tCal.get(Calendar.DAY_OF_MONTH) > card.closingDay) {
                            invM++;
                            if (invM > 11) { invM = 0; invY++; }
                        }
                    }

                    if (invM == month && invY == year) {
                        t.isPaid = true;
                        toUpdate.add(t);
                    }
                }
            }

            if (!toUpdate.isEmpty()) {
                db.transactionDao().updateAll(toUpdate);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Fatura do " + card.name + " paga com sucesso!", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void filterAndDisplay() {
        if (filterViewModel.getFilterState().getValue() == null) return;
        
        SharedFilterViewModel.FilterState state = filterViewModel.getFilterState().getValue();
        long start = state.start;
        long end = state.end;

        List<Transaction> filtered = new ArrayList<>();
        int selectedTab = tabLayout.getSelectedTabPosition(); // 0: Despesa, 1: Receita, 2: Cartão
        double total = 0;

        Calendar fCal = Calendar.getInstance();
        fCal.setTimeInMillis(start);
        int selectedMonth = fCal.get(Calendar.MONTH);
        int selectedYear = fCal.get(Calendar.YEAR);
        
        // Use the explicit flag from FilterState
        boolean isFullMonth = state.isFullMonth;

        for (Transaction t : rawTransactions) {
            boolean matchesTime = t.date >= start && t.date <= end;

            // Search Filter Logic
            boolean matchesSearch = searchQuery.isEmpty() || 
                                    (t.description != null && t.description.toLowerCase().contains(searchQuery)) ||
                                    (t.category != null && t.category.toLowerCase().contains(searchQuery)) ||
                                    String.valueOf(t.amount).contains(searchQuery);

            if (!matchesSearch) continue;

            boolean belongsToPeriod;
            if (t.isCard && isFullMonth && t.invoiceMonth != null && t.invoiceYear != null) {
                // For whole month filter, use invoice mapping
                belongsToPeriod = (t.invoiceMonth == selectedMonth && t.invoiceYear == selectedYear);
            } else {
                // Otherwise use strict purchase date
                belongsToPeriod = matchesTime;
            }

            if (belongsToPeriod) {
                if (selectedTab == 0 && "DESPESA".equals(t.type) && !t.isCard) {
                    filtered.add(t);
                    total += t.amount;
                } else if (selectedTab == 1 && "RECEITA".equals(t.type)) {
                    filtered.add(t);
                    total += t.amount;
                } else if (selectedTab == 2 && t.isCard) {
                    filtered.add(t);
                    total += t.amount;
                }
            }
        }
        
        adapter.setTransactions(filtered);
        
        String label = "Total do Período";
        if (selectedTab == 0) label = "Total de Despesas";
        else if (selectedTab == 1) label = "Total de Receitas";
        else if (selectedTab == 2) label = isFullMonth ? "Fatura do Mês" : "Gasto em Cartão";
        
        textSummaryLabel.setText(label);
        textSummaryValue.setText(CurrencyUtils.format(total));
        textSummaryValue.setTextColor(android.graphics.Color.WHITE);

        // Button logic for Card tab - ONLY show on Full Month filters to avoid confusion
        boolean hasUnpaid = false;
        boolean hasPaid = false;
        if (selectedTab == 2 && isFullMonth) {
            for (Transaction t : filtered) {
                if (!t.isPaid) hasUnpaid = true;
                else hasPaid = true;
            }
        }

        if (selectedTab == 2 && total > 0 && isFullMonth) {
            if (hasUnpaid) {
                btnPayInvoice.setVisibility(View.VISIBLE);
                btnUndoPayment.setVisibility(View.GONE);
            } else if (hasPaid) {
                btnPayInvoice.setVisibility(View.GONE);
                btnUndoPayment.setVisibility(View.VISIBLE);
            } else {
                btnPayInvoice.setVisibility(View.GONE);
                btnUndoPayment.setVisibility(View.GONE);
            }
        } else {
            btnPayInvoice.setVisibility(View.GONE);
            btnUndoPayment.setVisibility(View.GONE);
        }
    }

}
