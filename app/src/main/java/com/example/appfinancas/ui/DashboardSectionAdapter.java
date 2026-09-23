package com.example.appfinancas.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.model.Category;
import com.example.appfinancas.model.Transaction;
import com.example.appfinancas.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<DashboardSection> sections;
    private OnSectionInteractionListener listener;
    
    private double income, expense, cards, balance, savings;
    private Map<String, Double> categoryMap;
    private List<Category> allCategories;
    private List<com.example.appfinancas.model.Card> allCards;
    private List<Transaction> pendingList;

    public interface OnSectionInteractionListener {
        void onNavigateToTab(int tabIndex);
        void onShowSeeAllPending(List<Transaction> allPending);
        void onShowSeeAllCategories(Map<String, Double> allCategoriesMap);
    }

    public DashboardSectionAdapter(List<DashboardSection> sections, OnSectionInteractionListener listener) {
        this.sections = sections;
        this.listener = listener;
    }

    public void updateData(double income, double expense, double cards, double balance, double savings,
                           Map<String, Double> categoryMap, List<Category> categories, 
                           List<com.example.appfinancas.model.Card> cardsList, List<Transaction> pending) {
        this.income = income;
        this.expense = expense;
        this.cards = cards;
        this.balance = balance;
        this.savings = savings;
        this.categoryMap = categoryMap;
        this.allCategories = categories;
        this.allCards = cardsList;
        this.pendingList = pending;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return sections.get(position).type.ordinal();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == DashboardSection.Type.OVERVIEW.ordinal()) {
            return new OverviewViewHolder(inflater.inflate(R.layout.section_overview, parent, false));
        } else if (viewType == DashboardSection.Type.PENDING.ordinal()) {
            return new PendingViewHolder(inflater.inflate(R.layout.section_pending, parent, false));
        } else {
            return new PieChartViewHolder(inflater.inflate(R.layout.section_pie_chart, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OverviewViewHolder) {
            bindOverview((OverviewViewHolder) holder);
        } else if (holder instanceof PendingViewHolder) {
            bindPending((PendingViewHolder) holder);
        } else if (holder instanceof PieChartViewHolder) {
            bindPieChart((PieChartViewHolder) holder);
        }
    }

    private void bindOverview(OverviewViewHolder vh) {
        vh.textIncome.setText(CurrencyUtils.format(income));
        vh.textExpense.setText(CurrencyUtils.format(expense - cards));
        vh.textCards.setText(CurrencyUtils.format(cards));
        vh.textBalance.setText(CurrencyUtils.format(balance));
        vh.textSavings.setText(String.format(Locale.getDefault(), "%.0f%%", savings));
        
        vh.rowIncome.setOnClickListener(v -> listener.onNavigateToTab(1));
        vh.rowExpense.setOnClickListener(v -> listener.onNavigateToTab(0));
        vh.rowCards.setOnClickListener(v -> listener.onNavigateToTab(2));
    }

    private void bindPending(PendingViewHolder vh) {
        vh.layoutPending.removeAllViews();
        if (pendingList == null) {
            vh.btnSeeAll.setVisibility(View.GONE);
            return;
        }

        int count = 0;
        for (Transaction t : pendingList) {
            if (count >= 5) break;
            View itemView = LayoutInflater.from(vh.itemView.getContext()).inflate(R.layout.item_transaction, vh.layoutPending, false);
            
            if ("GROUPED_CARD".equals(t.origin)) {
                int statusColor = t.isPaid ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
                String statusText = t.isPaid ? " (Pago)" : " (Pendente)";
                String baseDesc = "Cartão " + t.bankName;
                
                SpannableStringBuilder ssb = new SpannableStringBuilder(baseDesc);
                int start = ssb.length();
                ssb.append(statusText);
                ssb.setSpan(new ForegroundColorSpan(statusColor), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                ((TextView)itemView.findViewById(R.id.text_description)).setText(ssb);
                ((TextView)itemView.findViewById(R.id.text_description)).setTextColor(Color.WHITE);
                ((TextView)itemView.findViewById(R.id.text_amount)).setText(CurrencyUtils.format(t.amount));
                ((TextView)itemView.findViewById(R.id.text_amount)).setTextColor(Color.WHITE);
                TextView catText = itemView.findViewById(R.id.text_category);
                catText.setText("💳 Cartão");
                catText.setVisibility(View.VISIBLE);
                
                int orange = 0xFFFF9800;
                if (allCards != null) {
                    for (com.example.appfinancas.model.Card c : allCards) {
                        if (c.name.equals(t.bankName)) {
                            if (c.color != null) orange = Color.parseColor(c.color);
                            break;
                        }
                    }
                }
                catText.setTextColor(orange);
                
                itemView.findViewById(R.id.text_dot_separator).setVisibility(View.GONE);
                itemView.findViewById(R.id.text_bank).setVisibility(View.GONE);
                itemView.findViewById(R.id.text_card_flag).setVisibility(View.GONE);
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onNavigateToTab(2);
                });
            } else {
                int statusColor = t.isPaid ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
                String statusText = t.isPaid ? " (Pago)" : " (Pendente)";
                
                SpannableStringBuilder ssb = new SpannableStringBuilder(t.description);
                int start = ssb.length();
                ssb.append(statusText);
                ssb.setSpan(new ForegroundColorSpan(statusColor), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                ((TextView)itemView.findViewById(R.id.text_description)).setText(ssb);
                ((TextView)itemView.findViewById(R.id.text_description)).setTextColor(Color.WHITE);
                ((TextView)itemView.findViewById(R.id.text_amount)).setText(CurrencyUtils.format(t.amount));
                ((TextView)itemView.findViewById(R.id.text_amount)).setTextColor(Color.WHITE);
                
                TextView catText = itemView.findViewById(R.id.text_category);
                catText.setText(t.category != null ? t.category : "Sem Categoria");
                catText.setVisibility(View.VISIBLE);
                catText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                if (allCategories != null) {
                    for (Category c : allCategories) {
                        if (c.name.equals(t.category)) {
                            catText.setTextColor(Color.parseColor(c.color));
                            break;
                        }
                    }
                }
                
                if (t.isCard) itemView.findViewById(R.id.text_card_flag).setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.text_bank).setVisibility(View.GONE);
                itemView.findViewById(R.id.text_dot_separator).setVisibility(View.GONE);
                
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(vh.itemView.getContext(), AddTransactionActivity.class);
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
                    vh.itemView.getContext().startActivity(intent);
                });
            }
            vh.layoutPending.addView(itemView);
            count++;
        }

        if (pendingList.size() > 5) {
            vh.btnSeeAll.setVisibility(View.VISIBLE);
            vh.btnSeeAll.setOnClickListener(v -> {
                if (listener != null) listener.onShowSeeAllPending(pendingList);
            });
        } else {
            vh.btnSeeAll.setVisibility(View.GONE);
        }
    }

    private void bindPieChart(PieChartViewHolder vh) {
        if (categoryMap != null) {
            vh.pieChartView.setData(categoryMap, allCategories);
            updateLegend(vh, vh.itemView.getContext());
        }
    }

    private void updateLegend(PieChartViewHolder vh, Context context) {
        vh.layoutLegend.removeAllViews();
        if (categoryMap == null) {
            vh.btnSeeAll.setVisibility(View.GONE);
            return;
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryMap.entrySet());
        sortedCategories.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        double total = 0;
        for (double val : categoryMap.values()) total += val;

        int count = 0;
        for (Map.Entry<String, Double> entry : sortedCategories) {
            if (count >= 5) break;
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_transaction, vh.layoutLegend, false);
            
            double percentage = (entry.getValue() / total) * 100;
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

            vh.layoutLegend.addView(itemView);
            count++;
        }

        if (categoryMap.size() > 5) {
            vh.btnSeeAll.setVisibility(View.VISIBLE);
            vh.btnSeeAll.setOnClickListener(v -> {
                if (listener != null) listener.onShowSeeAllCategories(categoryMap);
            });
        } else {
            vh.btnSeeAll.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class OverviewViewHolder extends RecyclerView.ViewHolder {
        TextView textIncome, textExpense, textCards, textBalance, textSavings;
        View rowIncome, rowExpense, rowCards;
        public OverviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textIncome = itemView.findViewById(R.id.text_income);
            textExpense = itemView.findViewById(R.id.text_expense);
            textCards = itemView.findViewById(R.id.text_cards_total);
            textBalance = itemView.findViewById(R.id.text_balance);
            textSavings = itemView.findViewById(R.id.text_economy_percent);
            rowIncome = itemView.findViewById(R.id.row_income);
            rowExpense = itemView.findViewById(R.id.row_expense);
            rowCards = itemView.findViewById(R.id.row_cards);
        }
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutPending;
        Button btnSeeAll;
        public PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutPending = itemView.findViewById(R.id.layout_pending_payments);
            btnSeeAll = itemView.findViewById(R.id.btn_see_all_pending);
        }
    }

    static class PieChartViewHolder extends RecyclerView.ViewHolder {
        ExpensePieChartView pieChartView;
        LinearLayout layoutLegend;
        Button btnSeeAll;
        public PieChartViewHolder(@NonNull View itemView) {
            super(itemView);
            pieChartView = itemView.findViewById(R.id.pie_chart_view);
            layoutLegend = itemView.findViewById(R.id.layout_chart_legend);
            btnSeeAll = itemView.findViewById(R.id.btn_see_all_categories);
        }
    }
}
