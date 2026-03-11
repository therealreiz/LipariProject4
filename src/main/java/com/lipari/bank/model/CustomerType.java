package com.lipari.bank.model;

public enum CustomerType {
    PRIVATE("Privato"),
    BUSINESS("Azienda");

    private final String label;

    CustomerType(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
