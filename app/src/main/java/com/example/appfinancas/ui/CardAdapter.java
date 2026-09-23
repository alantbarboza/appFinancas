package com.example.appfinancas.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private List<Card> cards = new ArrayList<>();
    private OnCardListener listener;

    public interface OnCardListener {
        void onEdit(Card card);
        void onDelete(Card card);
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
        notifyDataSetChanged();
    }

    public void setListener(OnCardListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.textName.setText(card.name);
        holder.textDates.setText(String.format(Locale.getDefault(), "Fecha dia %d - Vence dia %d", card.closingDay, card.dueDay));

        if (card.color != null && !card.color.isEmpty()) {
            int color = Color.parseColor(card.color);
            holder.cardView.setCardBackgroundColor(color);
            
            // Contrast check for text
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            int textColor = (darkness < 0.5) ? Color.BLACK : Color.WHITE;
            
            holder.textName.setTextColor(textColor);
            holder.textDates.setTextColor(textColor);
            holder.btnEdit.setColorFilter(textColor);
            holder.btnDelete.setColorFilter(textColor);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(card);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(card);
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textName, textDates;
        ImageButton btnEdit, btnDelete;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view_root);
            textName = itemView.findViewById(R.id.text_card_name);
            textDates = itemView.findViewById(R.id.text_card_dates);
            btnEdit = itemView.findViewById(R.id.btn_edit_card);
            btnDelete = itemView.findViewById(R.id.btn_delete_card);
        }
    }
}
