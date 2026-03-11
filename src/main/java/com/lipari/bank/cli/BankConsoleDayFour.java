package com.lipari.bank.cli;

import com.lipari.bank.model.*;
import com.lipari.bank.persistence.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Entry point del progetto LipariBank — Day 4.
 */
public class BankConsoleDayFour {

    private final CustomerDao    customerDao    = new CustomerDao();
    private final AccountDao     accountDao     = new AccountDao();
    private final TransactionDao transactionDao = new TransactionDao();

    // ─── Entry point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            new BankConsoleDayFour().run();
        } catch (Exception e) {
            System.err.println("ERRORE FATALE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void run() throws SQLException {
        printBanner();
        setupDatabase();
        scenario1_sqlInjection();
        scenario2_joinWithoutGroupBy();
        scenario3_connectionLeak();
        printFooter();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Setup — inizializza schema e inserisce dati di test
    // ══════════════════════════════════════════════════════════════════════════

    private Customer mario, anna, roberto;
    private Account  acc_mario_cc, acc_mario_rs, acc_anna_cc, acc_rob_rs, acc_rob_cc;

    private void setupDatabase() throws SQLException {
        sep("SETUP — Inizializzazione schema e dati di test");

        DatabaseManager.initializeSchema();
        System.out.println("  Schema H2 creato. Connessioni attive: "
                + DatabaseManager.getActiveConnections() + "/" + DatabaseManager.MAX_POOL_SIZE);

        // ── 3 clienti ────────────────────────────────────────────────────────
        mario   = customerDao.save(new Customer("RSSMRA85M01H501Z", "Mario",   "Rossi",   CustomerType.PRIVATE));
        anna    = customerDao.save(new Customer("BNCNNA90L50C351X", "Anna",    "Bianchi", CustomerType.BUSINESS));
        roberto = customerDao.save(new Customer("VRDBRT75L10C351Y", "Roberto", "Verdi",   CustomerType.PRIVATE));
        System.out.println("  Inseriti 3 clienti: " + mario.getFirstName()
                + ", " + anna.getFirstName() + ", " + roberto.getFirstName());

        // ── 5 conti (Mario=2, Anna=1, Roberto=2) ─────────────────────────────
        acc_mario_cc = accountDao.save(new Account("IT60A0542811101000000001001",
                new BigDecimal("5000.00"), "CHECKING", mario.getId()));
        acc_mario_rs = accountDao.save(new Account("IT60A0542811101000000002002",
                new BigDecimal("3000.00"), "SAVINGS",  mario.getId()));
        acc_anna_cc  = accountDao.save(new Account("IT60A0542811101000000003003",
                new BigDecimal("10000.00"), "CHECKING", anna.getId()));
        acc_rob_rs   = accountDao.save(new Account("IT60A0542811101000000004004",
                new BigDecimal("1500.00"),  "SAVINGS",  roberto.getId()));
        acc_rob_cc   = accountDao.save(new Account("IT60A0542811101000000005005",
                new BigDecimal("2500.00"),  "CHECKING", roberto.getId()));
        System.out.printf("  Inseriti 5 conti — saldi: Mario=%s€, Anna=%s€, Roberto=%s€%n",
                "8000.00", "10000.00", "4000.00");

        // ── 4 transazioni ─────────────────────────────────────────────────────
        transactionDao.save(new Transaction(acc_mario_cc.getId(), TransactionType.DEPOSIT,
                new BigDecimal("500.00"), "Stipendio luglio"));
        transactionDao.save(new Transaction(acc_mario_rs.getId(), TransactionType.WITHDRAWAL,
                new BigDecimal("200.00"), "Prelievo ATM"));
        transactionDao.save(new Transaction(acc_anna_cc.getId(),  TransactionType.DEPOSIT,
                new BigDecimal("1500.00"), "Incasso fattura #42"));
        transactionDao.save(new Transaction(acc_rob_cc.getId(),   TransactionType.TRANSFER,
                new BigDecimal("300.00"), "Bonifico affitto"));
        System.out.println("  Inserite 4 transazioni.");
        System.out.println("  Connessioni attive dopo setup: "
                + DatabaseManager.getActiveConnections() + "/" + DatabaseManager.MAX_POOL_SIZE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO 1
    // ══════════════════════════════════════════════════════════════════════════

    private void scenario1_sqlInjection() throws SQLException {
        sep("SCENARIO 1 — Ricerca clienti per codice fiscale");

        // Ricerca normale — CF valido
        String validCf = "RSSMRA85M01H501Z";
        List<Customer> normal = customerDao.findByFiscalCode(validCf);
        System.out.printf("  Ricerca normale  [%s]: %d risultato/i  ✓%n",
                validCf, normal.size());
        normal.forEach(c -> System.out.println("    → " + c));

        System.out.printf("  Connessioni attive: %d/%d%n",
                DatabaseManager.getActiveConnections(), DatabaseManager.MAX_POOL_SIZE);

        System.out.println();

        // Ricerca con CF inesistente — normale risultato vuoto
        String fakeCf = "XXXXXX00X00X000X";
        List<Customer> noResult = customerDao.findByFiscalCode(fakeCf);
        System.out.printf("  Ricerca CF inesistente [%s]: %d risultati  ✓%n",
                fakeCf, noResult.size());

        System.out.printf("  Connessioni attive: %d/%d%n",
                DatabaseManager.getActiveConnections(), DatabaseManager.MAX_POOL_SIZE);

        System.out.println();

        // SQL INJECTION — il parametro contiene ' OR '1'='1
        String injectedInput = "XXXXXX00X00X000X' OR '1'='1";
        System.out.printf("  Ricerca con SQL Injection [%s]:%n", injectedInput);
        List<Customer> injected = customerDao.findByFiscalCode(injectedInput);
        System.out.printf("  → %d risultati restituiti%n", injected.size());
        injected.forEach(c -> System.out.println("      " + c));

        System.out.printf("  Connessioni attive: %d/%d%n",
                DatabaseManager.getActiveConnections(), DatabaseManager.MAX_POOL_SIZE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO 2
    // ══════════════════════════════════════════════════════════════════════════

    private void scenario2_joinWithoutGroupBy() throws SQLException {
        sep("SCENARIO 2 — Riepilogo saldi clienti");

        System.out.println("  Clienti nel DB  : 3");
        System.out.println("  Conti nel DB    : 5");
        System.out.println("  Saldi attesi    : Mario=8.000€, Anna=10.000€, Roberto=4.000€");
        System.out.println();

        List<CustomerDao.CustomerBalanceRow> rows = customerDao.findCustomersWithTotalBalance();

        System.out.printf("  Righe restituite: %d  %s%n",
                rows.size(),
                rows.size() == 3 ? "✓" : "← inatteso (atteso 3)");
        System.out.println();
        System.out.printf("  %-20s %-25s %10s%n", "Cod. Fiscale", "Nome", "Saldo");
        System.out.println("  " + "─".repeat(57));
        rows.forEach(r -> System.out.println("  " + r));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO 3
    // ══════════════════════════════════════════════════════════════════════════

    private void scenario3_connectionLeak() {
        sep("SCENARIO 3 — Operazioni ripetute su database");

        System.out.printf("  Connessioni attive all'avvio: %d/%d%n",
                DatabaseManager.getActiveConnections(), DatabaseManager.MAX_POOL_SIZE);
        System.out.println();
        System.out.println("  Esecuzione chiamate a findById() senza chiudere le connessioni...");
        System.out.println();

        int call = 0;
        boolean poolExhausted = false;

        // Alterna tra CustomerDao.findById e AccountDao.findById
        // per mostrare che il leak riguarda ENTRAMBI i DAO
        while (!poolExhausted && call < 10) {
            call++;
            try {
                if (call % 2 != 0) {
                    customerDao.findById((call % 3) + 1L);
                    System.out.printf("  Chiamata %2d CustomerDao.findById()  : OK  | attive: %d/%d%n",
                            call,
                            DatabaseManager.getActiveConnections(),
                            DatabaseManager.MAX_POOL_SIZE);
                } else {
                    accountDao.findById((call % 5) + 1L);
                    System.out.printf("  Chiamata %2d AccountDao.findById()   : OK  | attive: %d/%d%n",
                            call,
                            DatabaseManager.getActiveConnections(),
                            DatabaseManager.MAX_POOL_SIZE);
                }
            } catch (SQLException e) {
                poolExhausted = true;
                System.out.printf("  Chiamata %2d findById()               : ERRORE%n", call);
                System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
                for (String line : e.getMessage().split("\n")) {
                    System.out.printf("  │  %-61s│%n", line.trim());
                }
                System.out.println("  └─────────────────────────────────────────────────────────────┘");
            }
        }

        if (!poolExhausted) {
            System.out.println("  (Pool non esaurito — riesegui per verificare)");
        }

        System.out.println();
        System.out.println("  Connessioni attive al termine: "
                + DatabaseManager.getActiveConnections() + "/" + DatabaseManager.MAX_POOL_SIZE);
    }

    // ─── Utility di stampa ───────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println("""
                ╔══════════════════════════════════════════════════════════════╗
                ║       LIPARIBANK — Broken Project Day 4                      ║
                ║       Java + H2 Database · SQL Injection · Connection Leak   ║
                ╚══════════════════════════════════════════════════════════════╝
                """);
    }

    private static void sep(String title) {
        int pad = Math.max(0, 60 - title.length());
        System.out.println("\n══ " + title + " " + "═".repeat(pad));
    }

    private static void printFooter() {
        System.out.println("""

                ════════════════════════════════════════════════════════════════
                  Hai trovato e fixato tutti e 3 i bug? Riesegui per verificare!
                ════════════════════════════════════════════════════════════════
                """);
    }
}
