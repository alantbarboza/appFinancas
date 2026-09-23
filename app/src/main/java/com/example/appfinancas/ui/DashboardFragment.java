package com.example.appfinancas.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.appfinancas.R;
import com.example.appfinancas.model.Category;
import com.example.appfinancas.model.Transaction;
import com.example.appfinancas.util.CurrencyUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {
    private DashboardViewModel viewModel;
    private SharedFilterViewModel filterViewModel;
    private TextView textFilterLabel;
    private LinearLayout layoutFilter;
    private RecyclerView recyclerView;
    private DashboardSectionAdapter adapter;
    
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private List<com.example.appfinancas.model.Card> allCards = new ArrayList<>();
    private List<DashboardSection> sections = new ArrayList<>();
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        textFilterLabel = root.findViewById(R.id.text_filter_label);
        layoutFilter = root.findViewById(R.id.layout_filter_trigger);
        recyclerView = root.findViewById(R.id.recycler_dashboard);

        loadSections();
        setupRecyclerView();
        setupClickListeners();

        // Apply Window Insets for responsiveness
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            
            // Adjust header padding for status bar
            View header = root.findViewById(R.id.layout_filter_trigger).getParent() instanceof View ? (View)root.findViewById(R.id.layout_filter_trigger).getParent() : null;
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), top + 24, header.getPaddingRight(), header.getPaddingBottom());
            }
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        filterViewModel = new ViewModelProvider(requireActivity()).get(SharedFilterViewModel.class);

        observeData();

        return root;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DashboardSectionAdapter(sections, new DashboardSectionAdapter.OnSectionInteractionListener() {
            @Override
            public void onNavigateToTab(int tabIndex) {
                BottomNavigationView navView = requireActivity().findViewById(R.id.bottom_nav);
                if (navView != null) {
                    Bundle args = new Bundle();
                    args.putInt("tabIndex", tabIndex);
                    navView.setSelectedItemId(R.id.navigation_transactions);
                    NavHostFragment.findNavController(DashboardFragment.this)
                            .navigate(R.id.navigation_transactions, args);
                }
            }

            @Override
            public void onShowSeeAllPending(List<Transaction> allPending) {
                showSeeAllDialog("Pagamentos Pendentes", allPending, null);
            }

            @Override
            public void onShowSeeAllCategories(Map<String, Double> allCategoriesMap) {
                showSeeAllDialog("Despesas por Categoria", null, allCategoriesMap);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void showSeeAllDialog(String title, List<Transaction> pending, Map<String, Double> categories) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_see_all, null);
        TextView textTitle = dialogView.findViewById(R.id.text_dialog_title);
        RecyclerView rv = dialogView.findViewById(R.id.recycler_see_all);
        ExpensePieChartView dialogChart = dialogView.findViewById(R.id.dialog_pie_chart);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen);
        dialog.setContentView(dialogView);

        textTitle.setText(title);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        if (pending != null) {
            dialogChart.setVisibility(View.GONE);
            rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_transaction, parent, false)) {};
                }
                @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    Transaction t = pending.get(position);
                    View itemView = holder.itemView;
                    
                    int statusColor = t.isPaid ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
                    String baseDesc = t.description != null ? t.description : "Cartão " + t.bankName;
                    
                    SpannableStringBuilder ssb = new SpannableStringBuilder(baseDesc);
                    String statusText = t.isPaid ? " (Pago)" : " (Pendente)";
                    int start = ssb.length();
                    ssb.append(statusText);
                    ssb.setSpan(new ForegroundColorSpan(statusColor), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    ((TextView)itemView.findViewById(R.id.text_description)).setText(ssb);
                    ((TextView)itemView.findViewById(R.id.text_description)).setTextColor(Color.WHITE);
                    ((TextView)itemView.findViewById(R.id.text_amount)).setText(CurrencyUtils.format(t.amount));
                    ((TextView)itemView.findViewById(R.id.text_amount)).setTextColor(Color.WHITE);
                    
                    TextView catText = itemView.findViewById(R.id.text_category);
                    catText.setVisibility(View.VISIBLE);
                    if ("GROUPED_CARD".equals(t.origin)) {
                        catText.setText("💳 Cartão");
                        catText.setTextColor(Color.parseColor("#FF9800"));
                        itemView.findViewById(R.id.text_bank).setVisibility(View.GONE);
                        itemView.findViewById(R.id.text_dot_separator).setVisibility(View.GONE);
                    } else {
                        catText.setText(t.category != null ? t.category : "Sem Categoria");
                        if (allCategories != null) {
                            for (Category c : allCategories) {
                                if (c.name.equals(t.category)) {
                                    catText.setTextColor(Color.parseColor(c.color));
                                    break;
                                }
                            }
                        }
                    }
                    
                    itemView.setOnClickListener(v -> {
                        dialog.dismiss();
                        if ("GROUPED_CARD".equals(t.origin)) {
                            BottomNavigationView navView = requireActivity().findViewById(R.id.bottom_nav);
                            if (navView != null) {
                                Bundle args = new Bundle();
                                args.putInt("tabIndex", 2);
                                navView.setSelectedItemId(R.id.navigation_transactions);
                                NavHostFragment.findNavController(DashboardFragment.this)
                                        .navigate(R.id.navigation_transactions, args);
                            }
                        } else {
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
                            startActivity(intent);
                        }
                    });
                }
                @Override public int getItemCount() { return pending.size(); }
            });
        } else if (categories != null) {
            dialogChart.setVisibility(View.VISIBLE);
            dialogChart.setData(categories, allCategories);
            
            double total = 0;
            for (double val : categories.values()) total += val;
            final double finalTotal = total;

            List<Map.Entry<String, Double>> list = new ArrayList<>(categories.entrySet());
            list.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
            rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_transaction, parent, false)) {};
                }
                @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    Map.Entry<String, Double> entry = list.get(position);
                    View itemView = holder.itemView;
                    
                    double percentage = (entry.getValue() / finalTotal) * 100;
                    String desc = String.format(Locale.getDefault(), "%s (%.0f%%)", entry.getKey(), percentage);

                    TextView textDesc = itemView.findViewById(R.id.text_description);
                    TextView textAmount = itemView.findViewById(R.id.text_amount);
                    View colorIndicator = itemView.findViewById(R.id.view_color_indicator);

                    textDesc.setText(desc);
                    textAmount.setText(CurrencyUtils.format(entry.getValue()));

                    textDesc.setTextColor(Color.WHITE);
                    textAmount.setTextColor(Color.WHITE);

                    itemView.findViewById(R.id.text_category).setVisibility(View.GONE);
                    itemView.findViewById(R.id.text_bank).setVisibility(View.GONE);
                    itemView.findViewById(R.id.text_dot_separator).setVisibility(View.GONE);

                    colorIndicator.setVisibility(View.VISIBLE);
                    int catColor = Color.parseColor("#888888"); // Neutral fallback

                    if (allCategories != null) {
                        for (Category c : allCategories) {
                            if (c.name.equals(entry.getKey())) {
                                catColor = Color.parseColor(c.color);
                                break;
                            }
                        }
                    }

                    // Tint the circle indicator
                    android.graphics.drawable.GradientDrawable background = (android.graphics.drawable.GradientDrawable) colorIndicator.getBackground();
                    background.setColor(catColor);
                }
                @Override public int getItemCount() { return list.size(); }
            });
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupClickListeners() {
        layoutFilter.setOnClickListener(v -> new FilterDialog().show(getChildFragmentManager(), "filter_dialog"));
    }

    private void observeData() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            allTransactions = transactions;
            updateUI();
        });

        com.example.appfinancas.database.AppDatabase.getDatabase(getContext())
                .categoryDao().getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            updateUI();
        });

        viewModel.getAllCards().observe(getViewLifecycleOwner(), cards -> {
            allCards = cards;
            updateUI();
        });

        filterViewModel.getFilterState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                textFilterLabel.setText(state.label);
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (getContext() == null || filterViewModel.getFilterState().getValue() == null) return;
        
        SharedFilterViewModel.FilterState state = filterViewModel.getFilterState().getValue();
        long start = state.start;
        long end = state.end;

        double income = 0;
        double expense = 0;
        double cardsTotal = 0;
        Map<String, Double> categoryMap = new HashMap<>();
        List<Transaction> pendingList = new ArrayList<>();
        Map<String, Double> pendingCardsMap = new HashMap<>();

        Calendar fCal = Calendar.getInstance();
        fCal.setTimeInMillis(start);
        int selectedMonth = fCal.get(Calendar.MONTH);
        int selectedYear = fCal.get(Calendar.YEAR);
        
        // Use the explicit flag from FilterState
        boolean isFullMonth = state.isFullMonth;

        for (Transaction t : allTransactions) {
            boolean matchesTime = t.date >= start && t.date <= end;

            // 1. Summary and Categories logic (Hybrid Logic)
            boolean belongsToPeriod;
            if (t.isCard && isFullMonth && t.invoiceMonth != null && t.invoiceYear != null) {
                // If it's a month filter, card items belong if they are in that month's invoice
                belongsToPeriod = (t.invoiceMonth == selectedMonth && t.invoiceYear == selectedYear);
            } else {
                // Otherwise (Today, Custom range), use strictly the purchase date
                belongsToPeriod = matchesTime;
            }

            if (belongsToPeriod) {
                if ("RECEITA".equals(t.type)) {
                    income += t.amount;
                } else if ("DESPESA".equals(t.type)) {
                    expense += t.amount;
                    if (t.isCard) cardsTotal += t.amount;
                    
                    String catName = (t.category != null) ? t.category : "Sem Categoria";
                    categoryMap.put(catName, categoryMap.getOrDefault(catName, 0.0) + t.amount);
                }
            }

            // 2. Pending List logic (Always stays relative to the selected Month/Year for planning)
            if ("DESPESA".equals(t.type) && !t.isPaid) {
                int invM, invY;
                if (t.isCard) {
                    if (t.invoiceMonth != null && t.invoiceYear != null) {
                        invM = t.invoiceMonth;
                        invY = t.invoiceYear;
                    } else {
                        Calendar tCal = Calendar.getInstance();
                        tCal.setTimeInMillis(t.date);
                        invM = tCal.get(Calendar.MONTH);
                        invY = tCal.get(Calendar.YEAR);
                        int closingDay = 10;
                        if (!allCards.isEmpty()) closingDay = allCards.get(0).closingDay;
                        if (tCal.get(Calendar.DAY_OF_MONTH) > closingDay) {
                            invM++;
                            if (invM > 11) { invM = 0; invY++; }
                        }
                    }

                    if (invM == selectedMonth && invY == selectedYear) {
                        String bankName = "Banco";
                        if (t.cardId != null) {
                            for (com.example.appfinancas.model.Card c : allCards) {
                                if (c.id == t.cardId) {
                                    bankName = c.name;
                                    break;
                                }
                            }
                        } else if (t.bankName != null && !t.bankName.isEmpty()) {
                            bankName = t.bankName;
                        }
                        pendingCardsMap.put(bankName, pendingCardsMap.getOrDefault(bankName, 0.0) + t.amount);
                    }
                } else if (matchesTime) {
                    pendingList.add(t);
                }
            }
        }

        // Add grouped cards to pending list
        for (Map.Entry<String, Double> entry : pendingCardsMap.entrySet()) {
            Transaction dummy = new Transaction();
            dummy.bankName = entry.getKey();
            dummy.amount = entry.getValue();
            dummy.isCard = true;
            dummy.origin = "GROUPED_CARD";
            dummy.type = "DESPESA";
            pendingList.add(dummy);
        }

        double balance = income - expense;
        double savings = income > 0 ? ((income - expense) / income) * 100 : 0;

        adapter.updateData(income, expense, cardsTotal, balance, savings, categoryMap, allCategories, allCards, pendingList);
    }


    private void loadSections() {
        SharedPreferences prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
        String order = prefs.getString("section_order", "PENDING,OVERVIEW,PIE_CHART");
        sections.clear();
        try {
            for (String s : order.split(",")) {
                if (s.isEmpty()) continue;
                sections.add(new DashboardSection(DashboardSection.Type.valueOf(s.trim())));
            }
        } catch (Exception e) {
            sections.clear();
            sections.add(new DashboardSection(DashboardSection.Type.PENDING));
            sections.add(new DashboardSection(DashboardSection.Type.OVERVIEW));
            sections.add(new DashboardSection(DashboardSection.Type.PIE_CHART));
            prefs.edit().remove("section_order").apply();
        }
        if (sections.isEmpty()) {
            sections.add(new DashboardSection(DashboardSection.Type.PENDING));
            sections.add(new DashboardSection(DashboardSection.Type.OVERVIEW));
            sections.add(new DashboardSection(DashboardSection.Type.PIE_CHART));
        }
    }
}
