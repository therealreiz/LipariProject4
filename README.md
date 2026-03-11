# 🔧 LipariBank Broken Project — Day 4

Progetto Java 17+ con database **H2 in-memory** e **Maven**.
Contiene **3 bug logici** su temi di persistenza: SQL Injection, query SQL errata, resource leak.
Il codice **compila senza errori** e avvia correttamente lo schema, ma si comporta in modo scorretto a runtime.

---

## Struttura del progetto

```
liparibank-broken-day4/
├── pom.xml                         (H2 + maven-shade-plugin)
├── src/main/java/com/lipari/bank/
│   ├── model/
│   │   ├── CustomerType.java
│   │   ├── TransactionType.java
│   │   ├── Customer.java
│   │   ├── Account.java
│   │   └── Transaction.java        (record)
│   ├── persistence/
│   │   ├── DatabaseManager.java    (H2 connection + pool tracker)
│   │   ├── CustomerDao.java
│   │   ├── AccountDao.java
│   │   └── TransactionDao.java
│   └── cli/
│       └── BankConsoleDayFour.java (3 scenari dimostrativi)
├── README.md
└── run.sh
```

---

## Prerequisiti

- **Java 17+** (o 21)
- **Maven 3.6+**

---

## Compilazione e avvio

### Metodo 1 — Script automatico

```bash
chmod +x run.sh
./run.sh
```

### Metodo 2 — Manuale

```bash
# Build fat-jar con H2 incluso
mvn clean package -DskipTests

# Avvio
java -jar target/liparibank-broken-day4-1.0-SNAPSHOT.jar
```

### Metodo 3 — Maven exec

```bash
mvn compile exec:java -Dexec.mainClass="com.lipari.bank.cli.BankConsoleDayFour"
```

---

## 🕵️ Le tue 3 missioni

---

### MISSIONE 1 — La ricerca per codice fiscale restituisce dati sbagliati

**Sintomo:** Il metodo `findByFiscalCode()` con un input normale funziona
correttamente. Ma passando un input che contiene caratteri speciali SQL
(come un apice `'`), la ricerca restituisce **tutti i clienti** nel database
invece di zero risultati, bypassando completamente il filtro.

---

### MISSIONE 2 — Il riepilogo saldi mostra più righe del previsto

**Sintomo:** Il metodo che restituisce i clienti con il loro saldo totale
produce **una riga per ogni conto** bancario associato al cliente, invece
di una singola riga con la somma. Un cliente con due conti compare due
volte nella lista, con i saldi separati anziché sommati.

---

### MISSIONE 3 — Il pool di connessioni si esaurisce dopo poche operazioni

**Sintomo:** Dopo aver eseguito alcune operazioni di lettura sul database,
l'applicazione smette di funzionare con un errore **"Pool esaurito"**.
Il contatore di connessioni attive sale ad ogni chiamata e non scende mai.
Le operazioni di scrittura (save, delete) funzionano correttamente.

---

## ✅ Obiettivo finale

Quando hai trovato e corretto tutti e 3 i bug, hai completato la missione!

- `findByFiscalCode()` deve restituire 0 risultati per un CF inesistente, anche con caratteri speciali
- `findCustomersWithTotalBalance()` deve restituire 3 righe (una per cliente) con la somma dei saldi
- Il pool di connessioni non deve esaurirsi: le connessioni devono essere sempre rilasciate
