package com.lipari.bank.model;

import java.math.BigDecimal;

public class Account {

    private long       id;
    private String     iban;
    private BigDecimal balance;
    private String     accountType;   // "CHECKING" | "SAVINGS"
    private long       customerId;

    /** Costruttore per INSERT. */
    public Account(String iban, BigDecimal balance, String accountType, long customerId) {
        this.iban        = iban;
        this.balance     = balance;
        this.accountType = accountType;
        this.customerId  = customerId;
    }

    /** Costruttore completo per SELECT. */
    public Account(long id, String iban, BigDecimal balance,
                   String accountType, long customerId) {
        this(iban, balance, accountType, customerId);
        this.id = id;
    }

    public long       getId()          { return id; }
    public String     getIban()        { return iban; }
    public BigDecimal getBalance()     { return balance; }
    public String     getAccountType() { return accountType; }
    public long       getCustomerId()  { return customerId; }

    public void setId(long id)               { this.id = id; }
    public void setBalance(BigDecimal b)     { this.balance = b; }

    @Override
    public String toString() {
        return String.format("Account{id=%d, iban='%s', balance=%.2f€, type=%s, customerId=%d}",
                id, iban, balance, accountType, customerId);
    }
}
