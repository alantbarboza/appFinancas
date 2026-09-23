package com.example.appfinancas.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final double MAX_LIMIT = 999999999999.00;

    public static String format(double value) {
        if (Math.abs(value) > MAX_LIMIT) {
            value = 0;
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(PT_BR);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        DecimalFormat df = new DecimalFormat("R$ #,##0.00", symbols);
        return df.format(value);
    }

    public static String formatPlain(double value) {
        if (Math.abs(value) > MAX_LIMIT) {
            value = 0;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(PT_BR);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(value);
    }
}
