package com.lipari.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rappresenta un singolo movimento contabile.
 * Immutabile per design (record Java 17+).
 */
public record Transaction(
        long            id,
        long            accountId,
        TransactionType transactionType,
        BigDecimal      amount,
        String          description,
        LocalDateTime   createdAt) {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Costruttore compatto per nuove transazioni (id e createdAt assegnati dal DB). */
    public Transaction(long accountId, TransactionType transactionType,
                       BigDecimal amount, String description) {
        this(0L, accountId, transactionType, amount, description, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("[%s] %-10s %10.2f€  %s",
                createdAt.format(FMT), transactionType.getLabel(), amount, description);
    }
}
