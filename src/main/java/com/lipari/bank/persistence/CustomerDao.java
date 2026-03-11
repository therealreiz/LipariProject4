package com.lipari.bank.persistence;

import com.lipari.bank.model.Customer;
import com.lipari.bank.model.CustomerType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per l'entità {@link Customer}.
 */
public class CustomerDao {

    // ─────────────────────────────────────────────────────────────────────────
    // Nested record per il risultato della query di riepilogo saldi
    // ─────────────────────────────────────────────────────────────────────────

    /** Riga di risultato per {@link #findCustomersWithTotalBalance()}. */
    public record CustomerBalanceRow(String fiscalCode, String fullName, BigDecimal balance) {
        @Override
        public String toString() {
            return String.format("%-20s %-25s %10.2f€", fiscalCode, fullName, balance);
        }
    }

    /**
     * Salva un nuovo cliente e aggiorna il suo id con quello generato dal DB.
     */
    public Customer save(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (fiscal_code, first_name, last_name, customer_type) "
                + "VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customer.getFiscalCode());
            pstmt.setString(2, customer.getFirstName());
            pstmt.setString(3, customer.getLastName());
            pstmt.setString(4, customer.getCustomerType().name());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getLong(1));
                }
            }
            return customer;
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Restituisce tutti i clienti.
     */
    public List<Customer> findAll() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, fiscal_code, first_name, last_name, customer_type FROM customers")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Customer> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Elimina un cliente per id.
     */
    public void delete(long id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Cerca clienti per codice fiscale.
     */
    public List<Customer> findByFiscalCode(String fiscalCode) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String sql = "SELECT id, fiscal_code, first_name, last_name, customer_type "
                + "FROM customers WHERE fiscal_code = '" + fiscalCode + "'";
        Statement stmt = conn.createStatement();
        ResultSet rs   = stmt.executeQuery(sql);

        List<Customer> result = new ArrayList<>();
        while (rs.next()) {
            result.add(mapRow(rs));
        }
        return result;
    }

    /**
     * Cerca un cliente per id.
     */
    public Optional<Customer> findById(long id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT id, fiscal_code, first_name, last_name, customer_type "
                    + "FROM customers WHERE id = ?");
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Restituisce la lista dei clienti con il loro saldo totale.
     */
    public List<CustomerBalanceRow> findCustomersWithTotalBalance() throws SQLException {
        String sql = """
                SELECT c.fiscal_code,
                       c.first_name,
                       c.last_name,
                       a.balance
                FROM customers c
                INNER JOIN accounts a ON c.id = a.customer_id
                ORDER BY c.last_name, c.first_name
                """;
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            List<CustomerBalanceRow> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new CustomerBalanceRow(
                        rs.getString("fiscal_code"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getBigDecimal("balance")));
            }
            return result;
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapping helper
    // ─────────────────────────────────────────────────────────────────────────

    private static Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getLong("id"),
                rs.getString("fiscal_code"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                CustomerType.valueOf(rs.getString("customer_type")));
    }
}
