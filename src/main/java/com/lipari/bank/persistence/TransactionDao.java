package com.lipari.bank.persistence;

import com.lipari.bank.model.Transaction;
import com.lipari.bank.model.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per l'entità {@link Transaction}.
 */
public class TransactionDao {

    public Transaction save(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, description) "
                + "VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, tx.accountId());
            pstmt.setString(2, tx.transactionType().name());
            pstmt.setBigDecimal(3, tx.amount());
            pstmt.setString(4, tx.description());
            pstmt.executeUpdate();
            long generatedId;
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                generatedId = keys.next() ? keys.getLong(1) : 0L;
            }
            return new Transaction(generatedId, tx.accountId(), tx.transactionType(),
                    tx.amount(), tx.description(), tx.createdAt());
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    public List<Transaction> findByAccountId(long accountId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, account_id, transaction_type, amount, description, created_at "
                + "FROM transactions WHERE account_id = ? ORDER BY created_at DESC")) {
            pstmt.setLong(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    public List<Transaction> findAll() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, account_id, transaction_type, amount, description, created_at "
                + "FROM transactions ORDER BY created_at DESC")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } finally {
            DatabaseManager.releaseConnection(conn);
        }
    }

    private static Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                rs.getLong("account_id"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getBigDecimal("amount"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
