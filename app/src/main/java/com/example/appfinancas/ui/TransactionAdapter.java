package com.example.appfinancas.ui;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.model.Category;
import com.example.appfinancas.model.Transaction;
import com.example.appfinancas.util.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<com.example.appfinancas.model.Card> cards = new ArrayList<>();
    private OnTransactionListener listener;

    public interface OnTransactionListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setCards(List<com.example.appfinancas.model.Card> cards) {
        this.cards = cards;
        notifyDataSetChanged();
    }

    public void setListener(OnTransactionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        holder.textDescription.setText(transaction.description);
        holder.textCategory.setText(transaction.category != null ? transaction.category : "Sem Categoria");
        holder.textCategory.setTextColor(Color.WHITE); // Default
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.textBank.setVisibility(View.VISIBLE);
        holder.textBank.setText(sdf.format(transaction.date));
        
        holder.textAmount.setText(CurrencyUtils.format(transaction.amount));
        holder.textAmount.setTextColor(Color.WHITE);
        holder.textDescription.setTextColor(Color.WHITE);

        // Append colored status to description
        int statusColor = transaction.isPaid ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        String statusText = transaction.isPaid ? " (Pago)" : " (Pendente)";
        
        SpannableStringBuilder ssb = new SpannableStringBuilder(transaction.description);
        int start = ssb.length();
        ssb.append(statusText);
        ssb.setSpan(new ForegroundColorSpan(statusColor), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        holder.textDescription.setText(ssb);

        // Set category color
        for (Category c : categories) {
            if (c.name.equals(transaction.category)) {
                holder.textCategory.setTextColor(Color.parseColor(c.color));
                break;
            }
        }

        if (!transaction.isPaid) {
            holder.textAmount.setAlpha(0.8f); // Slightly dimmed but still red
        } else {
            holder.textAmount.setAlpha(1.0f);
        }

        holder.textCardFlag.setVisibility(transaction.isCard ? View.VISIBLE : View.GONE);
        if (transaction.isCard && !transaction.isPaid) {
            Calendar now = Calendar.getInstance();
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            int currentMonth = now.get(Calendar.MONTH);
            int currentYear = now.get(Calendar.YEAR);

            int closeDay = 5; 
            int dueDay = 10;  
            
            if (cards != null) {
                for (com.example.appfinancas.model.Card c : cards) {
                    if (transaction.cardId != null && c.id == transaction.cardId) {
                        closeDay = c.closingDay;
                        dueDay = c.dueDay;
                        break;
                    }
                }
            }

            Integer invM = transaction.invoiceMonth;
            Integer invY = transaction.invoiceYear;

            if (invM != null && invY != null) {
                boolean isPastMonth = (invY < currentYear) || (invY == currentYear && invM < currentMonth);
                boolean isCurrentMonthOverdue = (invY == currentYear && invM == currentMonth && currentDay > dueDay);
                boolean isCurrentMonthClosed = (invY == currentYear && invM == currentMonth && currentDay > closeDay);

                if (isPastMonth || isCurrentMonthOverdue) {
                    holder.textCardFlag.setText("⚠️ Vencida (" + (transaction.bankName != null ? transaction.bankName : "Cartão") + ")");
                    holder.textCardFlag.setTextColor(Color.RED);
                } else if (isCurrentMonthClosed) {
                    holder.textCardFlag.setText("🚩 Fechada (" + (transaction.bankName != null ? transaction.bankName : "Cartão") + ")");
                    holder.textCardFlag.setTextColor(Color.parseColor("#FF9800"));
                } else {
                    holder.textCardFlag.setText("💳 " + (transaction.bankName != null ? transaction.bankName : "Cartão"));
                    holder.textCardFlag.setTextColor(Color.parseColor("#FF9800"));
                }
            } else {
                holder.textCardFlag.setText("💳 " + (transaction.bankName != null ? transaction.bankName : "Cartão"));
                holder.textCardFlag.setTextColor(Color.parseColor("#FF9800"));
            }
        } else if (transaction.isCard) {
            holder.textCardFlag.setText("💳 " + (transaction.bankName != null ? transaction.bankName : "Cartão"));
            holder.textCardFlag.setTextColor(Color.parseColor("#FF9800"));
        } else if (!transaction.isCard && !transaction.isPaid && "DESPESA".equals(transaction.type)) {
            if (transaction.dueDate != null) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                Calendar dueDay = Calendar.getInstance();
                dueDay.setTimeInMillis(transaction.dueDate);
                dueDay.set(Calendar.HOUR_OF_DAY, 0);
                dueDay.set(Calendar.MINUTE, 0);
                dueDay.set(Calendar.SECOND, 0);
                dueDay.set(Calendar.MILLISECOND, 0);

                if (dueDay.before(today)) {
                    holder.textCardFlag.setVisibility(View.VISIBLE);
                    holder.textCardFlag.setText("⚠️ Vencida");
                    holder.textCardFlag.setTextColor(Color.RED);
                } else if (dueDay.equals(today)) {
                    holder.textCardFlag.setVisibility(View.VISIBLE);
                    holder.textCardFlag.setText("🚨 Vence Hoje");
                    holder.textCardFlag.setTextColor(Color.parseColor("#FF9800"));
                } else {
                    holder.textCardFlag.setVisibility(View.GONE);
                }
            } else {
                holder.textCardFlag.setVisibility(View.GONE);
            }
        } else {
            holder.textCardFlag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(transaction);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(transaction);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textDescription, textCategory, textAmount, textBank, textCardFlag;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textDescription = itemView.findViewById(R.id.text_description);
            textCategory = itemView.findViewById(R.id.text_category);
            textAmount = itemView.findViewById(R.id.text_amount);
            textBank = itemView.findViewById(R.id.text_bank);
            textCardFlag = itemView.findViewById(R.id.text_card_flag);
        }
    }
}
