package com.example.appfinancas.ui;

public class DashboardSection {
    public enum Type { OVERVIEW, PENDING, PIE_CHART }
    
    public final Type type;
    public final String id;

    public DashboardSection(Type type) {
        this.type = type;
        this.id = type.name();
    }
}
