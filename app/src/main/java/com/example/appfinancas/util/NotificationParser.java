package com.example.appfinancas.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationParser {

    public static class TransactionSuggestion {
        public String type;
        public double amount;
        public String description;
        public String suggestedCategory;

        public TransactionSuggestion(String type, double amount, String description, String suggestedCategory) {
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.suggestedCategory = suggestedCategory;
        }
    }

    public static TransactionSuggestion parse(String packageName, String title, String text) {
        if (text == null) return null;

        String combined = (title + " " + text).toLowerCase();
        String type = null;

        // Identify Type using specific contextual phrases to avoid false positives
        if (containsAny(combined, 
                "transferência recebida", 
                "recebemos sua transferência", 
                "recebemos a sua transferência",
                "pix recebido", 
                "você recebeu um pix", 
                "crédito realizado com sucesso", 
                "depósito recebido",
                "recebemos seu pix")) {
            type = "RECEITA";
        } else if (containsAny(combined, 
                "pix realizado com sucesso", 
                "pix enviado com sucesso", 
                "transferência enviada", 
                "pagamento realizado", 
                "compra aprovada", 
                "pagamento de boleto realizado", 
                "transferência realizada com sucesso",
                "compra no cartão",
                "pix enviado",
                "foi realizado com sucesso",
                "você enviou um pix de",
                "compra no débito",
                "compra aprovada no débito",
                "débito realizado",
                "pagamento no débito",
                "compra no debito",
                "debito realizado",
                "utilizando seu cartão de débito")) {
            type = "DESPESA";
        }

        if (type == null) return null;

        // Extract Amount
        double amount = extractAmount(combined);
        if (amount == 0) {
            // Try extracting from original case just in case
            amount = extractAmount(title + " " + text);
        }
        
        // Return suggestion even if amount is 0 (as requested)
        // Simple Category Suggestion
        String category = suggestCategory(combined);
        String shortDesc = type.equals("RECEITA") ? "Receita" : "Despesa";

        return new TransactionSuggestion(type, amount, shortDesc, category);
    }

    private static String getShortBankName(String packageName) {
        if (packageName == null) return "Banco";
        if (packageName.contains("nubank")) return "Nubank";
        if (packageName.contains("itau")) return "Itaú";
        if (packageName.contains("bradesco")) return "Bradesco";
        if (packageName.contains("santander")) return "Santander";
        if (packageName.contains("inter")) return "Inter";
        if (packageName.contains("google.android.gm")) return "Gmail";
        return "Banco";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static double extractAmount(String text) {
        // More flexible regex: R$ optional, handles space, dots and commas
        // MUST have comma and 2 decimals to be considered currency (avoids IDs)
        // Matches: R$ 100,00 | R$100,00 | 100,00 | R$ 1.200,50 | R$0,01
        Pattern pattern = Pattern.compile("(?:R\\$?\\s?)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})");
        Matcher matcher = pattern.matcher(text);
        
        double lastAmount = 0;
        if (matcher.find()) {
            String val = matcher.group(1).replace(".", "").replace(",", ".");
            try {
                lastAmount = Double.parseDouble(val);
            } catch (NumberFormatException ignored) {}
        }
        return lastAmount;
    }

    private static String suggestCategory(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("uber") || lower.contains("99") || lower.contains("posto")) return "Transporte";
        if (lower.contains("ifood") || lower.contains("restaurante") || lower.contains("mercado")) return "Alimentação";
        if (lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema")) return "Lazer";
        if (lower.contains("farmacia") || lower.contains("hospital")) return "Saúde";
        return null; // Return null instead of "Outros" to match manual entry behavior
    }
}
