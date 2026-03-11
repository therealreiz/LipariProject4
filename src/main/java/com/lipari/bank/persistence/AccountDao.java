package com.lipari.bank.persistence;

import com.lipari.bank.model.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per l'entità {@link Account}.
 */
public class AccountDao {

    /**
     * Salva un nuovo conto e aggiorna il suo id generato dal DB.
     */
    public Account save(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (iban, balance, account_type, customer_id) "
                + "VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, account.getIban());
            pstmt.setBigDecimal(2, account.getBalance());
            pstmt.setString(3, account.getAccountType());
            pstmt.setLong(4, account.getCustomerId());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) account.setId(keys.getLong(1));
            }
            return account;
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Aggiorna il saldo di un conto.
     */
    public void updateBalance(long accountId, java.math.BigDecimal newBalance)
            throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setLong(2, accountId);
            pstmt.executeUpdate();
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Elimina un conto per id.
     */
    public void delete(long id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    /**
     * Cerca un conto per id.
     */
    public Optional<Account> findById(long id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT id, iban, balance, account_type, customer_id "
                    + "FROM accounts WHERE id = ?");
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
     * Restituisce tutti i conti associati a un cliente.
     */
    public List<Account> findByCustomerId(long customerId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT id, iban, balance, account_type, customer_id "
                    + "FROM accounts WHERE customer_id = ?");
            pstmt.setLong(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            List<Account> result = new ArrayList<>();
            while (rs.next()) result.add(mapRow(rs));
            return result;
        } catch (SQLException e) {
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapping helper
    // ─────────────────────────────────────────────────────────────────────────

    private static Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("iban"),
                rs.getBigDecimal("balance"),
                rs.getString("account_type"),
                rs.getLong("customer_id"));
    }
}
