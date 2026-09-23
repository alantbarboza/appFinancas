package com.example.appfinancas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String type; // RECEITA or DESPESA
    public double amount;
    public String description;
    public String category;
    public long date; // timestamp
    public String paymentMethod;
    public String origin; // MANUAL or NOTIFICACAO
    public Integer cardId; // null if not a card payment
    public String bankName;
    public boolean isCard;
    public int installments = 1;
    public boolean isPaid = false;
    public boolean isFixed = false;
    public String recurringId; // UUID to link monthly repetitions
    public Long dueDate; // Specific due date for manual/fixed expenses
    public Integer invoiceMonth; // Starting from 0 (January)
    public Integer invoiceYear;

    public Transaction() {}

    public Transaction(String type, double amount, String description, String category, long date, String paymentMethod, String origin, Integer cardId, String bankName, boolean isCard, int installments, boolean isPaid, boolean isFixed, String recurringId, Integer invoiceMonth, Integer invoiceYear, Long dueDate) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.origin = origin;
        this.cardId = cardId;
        this.bankName = bankName;
        this.isCard = isCard;
        this.installments = installments;
        this.isPaid = isPaid;
        this.isFixed = isFixed;
        this.recurringId = recurringId;
        this.invoiceMonth = invoiceMonth;
        this.invoiceYear = invoiceYear;
        this.dueDate = dueDate;
    }
}
