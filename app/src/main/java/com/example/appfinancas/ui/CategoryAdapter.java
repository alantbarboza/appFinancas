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
import com.example.appfinancas.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories = new ArrayList<>();
    private OnCategoryListener listener;

    public interface OnCategoryListener {
        void onEdit(Category category);
        void onDelete(Category category);
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setListener(OnCategoryListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.textName.setText(category.name);
        
        int color = Color.parseColor(category.color != null ? category.color : "#2196F3");
        holder.card.setCardBackgroundColor(color);
        
        // Accessibility check: black text for light backgrounds, white for dark
        if (isColorLight(color)) {
            holder.textName.setTextColor(Color.BLACK);
        } else {
            holder.textName.setTextColor(Color.WHITE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(category);
        });
        holder.btnEdit.setColorFilter(Color.BLACK);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(category);
        });
        holder.btnDelete.setColorFilter(Color.BLACK);
    }

    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        CardView card;
        ImageButton btnEdit, btnDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_category_name);
            card = itemView.findViewById(R.id.card_category);
            btnEdit = itemView.findViewById(R.id.btn_edit_category);
            btnDelete = itemView.findViewById(R.id.btn_delete_category);
        }
    }
}
