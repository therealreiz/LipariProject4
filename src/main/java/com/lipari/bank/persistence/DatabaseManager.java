package com.lipari.bank.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestore della connessione al database H2 in-memory.
 */
public class DatabaseManager {

    // ─── Configurazione pool ─────────────────────────────────────────────────

    /** Dimensione massima del pool simulato. */
    public static final int MAX_POOL_SIZE = 5;

    private static final String JDBC_URL =
            "jdbc:h2:mem:liparibank;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String USER = "sa";
    private static final String PASS = "";

    // ─── Tracking connessioni ─────────────────────────────────────────────────

    /** Contatore connessioni attualmente aperte (non ancora rilasciate). */
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    // ─── Inizializzazione driver ──────────────────────────────────────────────

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("H2 Driver non trovato: " + e.getMessage());
        }
    }

    // ─── API pubblica ─────────────────────────────────────────────────────────

    /**
     * Restituisce una nuova connessione al database.
     *
     * <p>Incrementa il contatore delle connessioni attive.
     * Lancia {@link SQLException} se il pool simulato è esaurito.
     *
     * @throws SQLException se il pool è esaurito o la connessione fallisce
     */
    public static Connection getConnection() throws SQLException {
        int current = activeConnections.incrementAndGet();
        if (current > MAX_POOL_SIZE) {
            activeConnections.decrementAndGet();
            throw new SQLException(String.format(
                    "Pool esaurito! %d/%d connessioni già attive.%n",
                    MAX_POOL_SIZE, MAX_POOL_SIZE));
        }
        try {
            return DriverManager.getConnection(JDBC_URL, USER, PASS);
        } catch (SQLException e) {
            activeConnections.decrementAndGet();   // rollback counter se la connessione fallisce
            throw e;
        }
    }

    /**
     * Rilascia la connessione (chiude la connessione H2 e decrementa il contatore).
     *
     * <p>È null-safe: se {@code conn} è null, il metodo non fa nulla.
     *
     * @param conn connessione da rilasciare
     */
    public static void releaseConnection(Connection conn) {
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException e) {
            // ignore close errors — già chiuso o errore non bloccante
        } finally {
            activeConnections.decrementAndGet();
        }
    }

    /** @return numero di connessioni correntemente attive. */
    public static int getActiveConnections() {
        return activeConnections.get();
    }

    // ─── Schema ───────────────────────────────────────────────────────────────

    /**
     * Crea le tabelle del database se non esistono.
     */
    public static void initializeSchema() throws SQLException {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS customers (
                        id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                        fiscal_code   VARCHAR(16)   NOT NULL UNIQUE,
                        first_name    VARCHAR(50)   NOT NULL,
                        last_name     VARCHAR(50)   NOT NULL,
                        customer_type VARCHAR(10)   NOT NULL
                    )""");

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                        iban         VARCHAR(34)   NOT NULL UNIQUE,
                        balance      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                        account_type VARCHAR(10)   NOT NULL,
                        customer_id  BIGINT        NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(id)
                    )""");

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                        account_id       BIGINT        NOT NULL,
                        transaction_type VARCHAR(10)   NOT NULL,
                        amount           DECIMAL(15,2) NOT NULL,
                        description      VARCHAR(200),
                        created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (account_id) REFERENCES accounts(id)
                    )""");
        } finally {
            releaseConnection(conn);
        }
    }

    private DatabaseManager() { /* utility class */ }
}
