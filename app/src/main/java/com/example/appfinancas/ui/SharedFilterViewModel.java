package com.example.appfinancas.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SharedFilterViewModel extends ViewModel {
    
    public static class FilterState {
        public final long start;
        public final long end;
        public final String label;
        public final boolean isFullMonth;

        public FilterState(long start, long end, String label, boolean isFullMonth) {
            this.start = start;
            this.end = end;
            this.label = label;
            this.isFullMonth = isFullMonth;
        }
    }

    private final MutableLiveData<FilterState> filterState = new MutableLiveData<>();

    public SharedFilterViewModel() {
        setToCurrentMonth();
    }

    public LiveData<FilterState> getFilterState() {
        return filterState;
    }

    public void setRange(long start, long end, String labelStr, boolean isFullMonth) {
        filterState.setValue(new FilterState(start, end, labelStr, isFullMonth));
    }

    public void setToToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM", new Locale("pt", "BR"));
        setRange(start, end, "Hoje: " + sdf.format(start), false);
    }

    public void setToCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        setRange(start, end, getMonthName(cal), true);
    }

    public void setToCurrentYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        setRange(start, end, "Ano " + cal.get(Calendar.YEAR), false);
    }

    public void changeMonth(int offset) {
        Calendar cal = Calendar.getInstance();
        if (filterState.getValue() != null) {
            cal.setTimeInMillis(filterState.getValue().start);
        }
        cal.add(Calendar.MONTH, offset);
        
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        
        setRange(start, end, getMonthName(cal), true);
    }

    public void changeDay(int offset) {
        Calendar cal = Calendar.getInstance();
        if (filterState.getValue() != null && !filterState.getValue().isFullMonth && !filterState.getValue().label.startsWith("Ano")) {
            // Se já estivermos em um filtro de dia (Hoje/Ontem/Amanhã), navegamos a partir dele
            cal.setTimeInMillis(filterState.getValue().start);
        }
        // Se estivermos em um mês completo ou ano, o "Amanhã" ou "Ontem" deve ser relativo a HOJE real
        
        cal.add(Calendar.DAY_OF_YEAR, offset);
        
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        // Check if it's today to use a friendly label
        Calendar today = Calendar.getInstance();
        if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            setToToday();
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM", new Locale("pt", "BR"));
            setRange(start, end, sdf.format(start), false);
        }
    }

    private String getMonthName(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("pt", "BR"));
        String name = sdf.format(cal.getTime());
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
