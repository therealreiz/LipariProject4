package com.lipari.bank.model;

import java.util.Objects;

public class Customer {

    private long       id;
    private String     fiscalCode;
    private String     firstName;
    private String     lastName;
    private CustomerType customerType;

    /** Costruttore senza id (per INSERT). */
    public Customer(String fiscalCode, String firstName,
                    String lastName, CustomerType customerType) {
        this.fiscalCode   = fiscalCode;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.customerType = customerType;
    }

    /** Costruttore completo (per SELECT con id dal DB). */
    public Customer(long id, String fiscalCode, String firstName,
                    String lastName, CustomerType customerType) {
        this(fiscalCode, firstName, lastName, customerType);
        this.id = id;
    }

    public long         getId()           { return id; }
    public String       getFiscalCode()   { return fiscalCode; }
    public String       getFirstName()    { return firstName; }
    public String       getLastName()     { return lastName; }
    public CustomerType getCustomerType() { return customerType; }

    public void setId(long id)                         { this.id = id; }
    public void setFiscalCode(String v)                { this.fiscalCode = v; }
    public void setFirstName(String v)                 { this.firstName = v; }
    public void setLastName(String v)                  { this.lastName = v; }
    public void setCustomerType(CustomerType v)        { this.customerType = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer c)) return false;
        return Objects.equals(fiscalCode, c.fiscalCode);
    }

    @Override
    public int hashCode() { return Objects.hash(fiscalCode); }

    @Override
    public String toString() {
        return String.format("Customer{id=%d, cf='%s', nome='%s %s', tipo=%s}",
                id, fiscalCode, firstName, lastName, customerType.getLabel());
    }
}
