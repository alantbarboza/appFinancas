package com.example.appfinancas.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.example.appfinancas.model.NotificationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationEventAdapter extends RecyclerView.Adapter<NotificationEventAdapter.EventViewHolder> {
    private List<NotificationEvent> events = new ArrayList<>();
    private OnEventListener listener;

    public interface OnEventListener {
        void onIgnore(NotificationEvent event);
    }

    public void setEvents(List<NotificationEvent> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setListener(OnEventListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        NotificationEvent event = events.get(position);
        holder.textTitle.setText(event.title);
        holder.textText.setText(event.text);
        holder.textDetection.setText(String.format(Locale.getDefault(), "Identificado: %s - R$ %.2f", event.detectedType, event.detectedAmount));

        if (event.processed) {
            holder.btnIgnore.setVisibility(View.GONE);
            holder.textDetection.append(" (Lançado)");
        } else {
            holder.btnIgnore.setVisibility(View.VISIBLE);
            holder.btnIgnore.setOnClickListener(v -> {
                if (listener != null) listener.onIgnore(event);
            });
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textText, textDetection;
        Button btnIgnore;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_event_title);
            textText = itemView.findViewById(R.id.text_event_text);
            textDetection = itemView.findViewById(R.id.text_event_detection);
            btnIgnore = itemView.findViewById(R.id.btn_ignore_event);
        }
    }
}
