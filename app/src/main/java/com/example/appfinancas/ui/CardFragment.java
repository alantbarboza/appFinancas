package com.example.appfinancas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfinancas.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CardFragment extends Fragment {
    private CardViewModel viewModel;
    private CardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cards, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_cards);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CardAdapter();
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = root.findViewById(R.id.fab_add_card);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddCardActivity.class));
        });

        viewModel = new ViewModelProvider(this).get(CardViewModel.class);
        viewModel.getAllCards().observe(getViewLifecycleOwner(), cards -> {
            adapter.setCards(cards);
        });

        return root;
    }
}
