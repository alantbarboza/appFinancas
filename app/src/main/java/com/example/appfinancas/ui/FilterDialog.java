package com.example.appfinancas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.appfinancas.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class FilterDialog extends BottomSheetDialogFragment {
    private SharedFilterViewModel filterViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        filterViewModel = new ViewModelProvider(requireActivity()).get(SharedFilterViewModel.class);

        Button btnPrev = view.findViewById(R.id.btn_filter_prev_month);
        Button btnNext = view.findViewById(R.id.btn_filter_next_month);
        Button btnYesterday = view.findViewById(R.id.btn_filter_yesterday);
        Button btnToday = view.findViewById(R.id.btn_filter_today);
        Button btnTomorrow = view.findViewById(R.id.btn_filter_tomorrow);
        Button btnMonth = view.findViewById(R.id.btn_filter_month);
        Button btnYear = view.findViewById(R.id.btn_filter_year);
        Button btnCalendar = view.findViewById(R.id.btn_open_calendar);
        Button btnApply = view.findViewById(R.id.btn_apply_filter);

        btnPrev.setOnClickListener(v -> { filterViewModel.changeMonth(-1); dismiss(); });
        btnNext.setOnClickListener(v -> { filterViewModel.changeMonth(1); dismiss(); });
        btnYesterday.setOnClickListener(v -> { filterViewModel.changeDay(-1); dismiss(); });
        btnToday.setOnClickListener(v -> { filterViewModel.setToToday(); dismiss(); });
        btnTomorrow.setOnClickListener(v -> { filterViewModel.changeDay(1); dismiss(); });
        btnMonth.setOnClickListener(v -> { filterViewModel.setToCurrentMonth(); dismiss(); });
        btnYear.setOnClickListener(v -> { filterViewModel.setToCurrentYear(); dismiss(); });

        btnCalendar.setOnClickListener(v -> showRangePicker());
        btnApply.setOnClickListener(v -> dismiss());
    }

    private void showRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Selecione o período")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                // selection timestamps are UTC midnight.
                // Convert to Local timezone correctly:
                Calendar calUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                Calendar calLocal = Calendar.getInstance();
                
                // Start Date
                calUTC.setTimeInMillis(selection.first);
                calLocal.set(calUTC.get(Calendar.YEAR), calUTC.get(Calendar.MONTH), calUTC.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                calLocal.set(Calendar.MILLISECOND, 0);
                long localStart = calLocal.getTimeInMillis();

                // End Date
                calUTC.setTimeInMillis(selection.second);
                calLocal.set(calUTC.get(Calendar.YEAR), calUTC.get(Calendar.MONTH), calUTC.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
                calLocal.set(Calendar.MILLISECOND, 999);
                long localEnd = calLocal.getTimeInMillis();
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String label = sdf.format(localStart) + " - " + sdf.format(localEnd);
                filterViewModel.setRange(localStart, localEnd, label, false);
                dismiss();
            }
        });
        picker.show(getChildFragmentManager(), "range_picker");
    }
}
