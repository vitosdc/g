import java.sql.*;
import javax.swing.JOptionPane;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:gestionale.db";
    
    private DatabaseManager() {
        // Private constructor for the Singleton pattern
    }
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    public void initDatabase() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create the connection
            connection = DriverManager.getConnection(DB_URL);
            
            // Enable foreign keys and set SQLite optimizations
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }
            
            // Create tables if they do not exist
            createTables();
            
            System.out.println("Database initialized successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error during database initialization: " + e.getMessage(),
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createTables() throws SQLException {
        // Customers Table
        String createClientiTable = """
            CREATE TABLE IF NOT EXISTS clienti (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cognome TEXT NOT NULL,
                email TEXT,
                telefono TEXT,
                indirizzo TEXT
            )
        """;
        
        // Products Table
        String createProdottiTable = """
            CREATE TABLE IF NOT EXISTS prodotti (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codice TEXT UNIQUE NOT NULL,
                nome TEXT NOT NULL,
                descrizione TEXT,
                prezzo REAL NOT NULL,
                quantita INTEGER DEFAULT 0
            )
        """;
        
        // Orders Table - FIXED: Changed to DATETIME
        String createOrdiniTable = """
            CREATE TABLE IF NOT EXISTS ordini (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_id INTEGER,
                data_ordine DATETIME NOT NULL,
                stato TEXT NOT NULL,
                totale REAL NOT NULL,
                FOREIGN KEY (cliente_id) REFERENCES clienti (id)
            )
        """;
        
        // Order Details Table
        String createDettagliOrdineTable = """
            CREATE TABLE IF NOT EXISTS dettagli_ordine (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ordine_id INTEGER,
                prodotto_id INTEGER,
                quantita INTEGER NOT NULL,
                prezzo_unitario REAL NOT NULL,
                FOREIGN KEY (ordine_id) REFERENCES ordini (id),
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;
        
        // Invoices Table - FIXED: Changed to DATETIME
        String createFattureTable = """
            CREATE TABLE IF NOT EXISTS fatture (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero TEXT UNIQUE NOT NULL,
                data DATETIME NOT NULL,
                cliente_id INTEGER,
                imponibile REAL NOT NULL,
                iva REAL NOT NULL,
                totale REAL NOT NULL,
                stato TEXT NOT NULL,
                FOREIGN KEY (cliente_id) REFERENCES clienti (id)
            )
        """;
        
        // Invoice Details Table
        String createDettagliFatturaTable = """
            CREATE TABLE IF NOT EXISTS dettagli_fattura (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fattura_id INTEGER,
                prodotto_id INTEGER,
                quantita INTEGER NOT NULL,
                prezzo_unitario REAL NOT NULL,
                aliquota_iva REAL NOT NULL,
                totale REAL NOT NULL,
                FOREIGN KEY (fattura_id) REFERENCES fatture (id),
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;
        
        // Invoice Numbering Table
        String createNumerazioneFattureTable = """
            CREATE TABLE IF NOT EXISTS numerazione_fatture (
                anno INTEGER PRIMARY KEY,
                ultimo_numero INTEGER NOT NULL
            )
        """;
        
        // Suppliers Table
        String createFornitoriTable = """
            CREATE TABLE IF NOT EXISTS fornitori (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ragione_sociale TEXT NOT NULL,
                partita_iva TEXT UNIQUE NOT NULL,
                codice_fiscale TEXT,
                indirizzo TEXT,
                telefono TEXT,
                email TEXT,
                pec TEXT,
                sito_web TEXT,
                note TEXT
            )
        """;
        
        // Supplier Orders Table - FIXED: Changed to DATETIME
        String createOrdiniFornitoriTable = """
            CREATE TABLE IF NOT EXISTS ordini_fornitori (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fornitore_id INTEGER NOT NULL,
                numero TEXT NOT NULL,
                data_ordine DATETIME NOT NULL,
                data_consegna_prevista DATETIME,
                stato TEXT NOT NULL,
                totale REAL NOT NULL,
                note TEXT,
                FOREIGN KEY (fornitore_id) REFERENCES fornitori (id)
            )
        """;
        
        // Supplier Order Details Table
        String createDettagliOrdiniFornitoriTable = """
            CREATE TABLE IF NOT EXISTS dettagli_ordini_fornitori (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ordine_id INTEGER NOT NULL,
                prodotto_id INTEGER NOT NULL,
                quantita INTEGER NOT NULL,
                prezzo_unitario REAL NOT NULL,
                totale REAL NOT NULL,
                note TEXT,
                FOREIGN KEY (ordine_id) REFERENCES ordini_fornitori (id),
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;
        
        // Supplier Price Lists Table - FIXED: Changed to DATETIME
        String createListiniFornitoriTable = """
            CREATE TABLE IF NOT EXISTS listini_fornitori (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fornitore_id INTEGER NOT NULL,
                prodotto_id INTEGER NOT NULL,
                codice_prodotto_fornitore TEXT,
                prezzo REAL NOT NULL,
                quantita_minima INTEGER DEFAULT 1,
                data_validita_inizio DATETIME NOT NULL,
                data_validita_fine DATETIME,
                note TEXT,
                FOREIGN KEY (fornitore_id) REFERENCES fornitori (id),
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;

        // Warehouse Movements Table - FIXED: Changed to DATETIME
        String createMovimentiMagazzinoTable = """
            CREATE TABLE IF NOT EXISTS movimenti_magazzino (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                prodotto_id INTEGER NOT NULL,
                data DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                tipo TEXT NOT NULL,
                quantita INTEGER NOT NULL,
                causale TEXT NOT NULL,
                documento_numero TEXT,
                documento_tipo TEXT,
                note TEXT,
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;

        // Minimum Stock Table
        String createScorteMinimaTable = """
            CREATE TABLE IF NOT EXISTS scorte_minime (
                prodotto_id INTEGER PRIMARY KEY,
                quantita_minima INTEGER NOT NULL,
                quantita_riordino INTEGER NOT NULL,
                lead_time_giorni INTEGER,
                fornitore_preferito_id INTEGER,
                note TEXT,
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id),
                FOREIGN KEY (fornitore_preferito_id) REFERENCES fornitori (id)
            )
        """;

        // Warehouse Notifications Table - FIXED: Changed to DATETIME
        String createNotificheMagazzinoTable = """
            CREATE TABLE IF NOT EXISTS notifiche_magazzino (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                prodotto_id INTEGER NOT NULL,
                data DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                tipo TEXT NOT NULL,
                messaggio TEXT NOT NULL,
                stato TEXT NOT NULL DEFAULT 'NEW',
                FOREIGN KEY (prodotto_id) REFERENCES prodotti (id)
            )
        """;

        // Company Data Table
        String createCompanyDataTable = """
            CREATE TABLE IF NOT EXISTS company_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                company_name TEXT NOT NULL,
                vat_number TEXT,
                tax_code TEXT,
                address TEXT,
                city TEXT,
                postal_code TEXT,
                country TEXT,
                phone TEXT,
                email TEXT,
                website TEXT,
                logo_path TEXT
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createClientiTable);
            stmt.execute(createProdottiTable);
            stmt.execute(createOrdiniTable);
            stmt.execute(createDettagliOrdineTable);
            stmt.execute(createFattureTable);
            stmt.execute(createDettagliFatturaTable);
            stmt.execute(createNumerazioneFattureTable);
            stmt.execute(createFornitoriTable);
            stmt.execute(createOrdiniFornitoriTable);
            stmt.execute(createDettagliOrdiniFornitoriTable);
            stmt.execute(createListiniFornitoriTable);
            stmt.execute(createMovimentiMagazzinoTable);
            stmt.execute(createScorteMinimaTable);
            stmt.execute(createNotificheMagazzinoTable);
            stmt.execute(createCompanyDataTable);
        }
    }
    
    public Connection getConnection() throws SQLException {
        // Check if connection is valid, if not recreate it
        if (connection == null || connection.isClosed() || !connection.isValid(5)) {
            initDatabase();
        }
        return connection;
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public String getNextInvoiceNumber(int year) throws SQLException {
        String numero;
        Connection conn = getConnection(); // Use the safe getConnection method
        conn.setAutoCommit(false);
        try {
            // Check if a record for the current year already exists
            String checkQuery = "SELECT ultimo_numero FROM numerazione_fatture WHERE anno = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                pstmt.setInt(1, year);
                ResultSet rs = pstmt.executeQuery();
                
                int nextNumber;
                if (rs.next()) {
                    // Increment the last number
                    nextNumber = rs.getInt("ultimo_numero") + 1;
                    String updateQuery = "UPDATE numerazione_fatture SET ultimo_numero = ? WHERE anno = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, nextNumber);
                        updateStmt.setInt(2, year);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Create a new record for the year
                    nextNumber = 1;
                    String insertQuery = "INSERT INTO numerazione_fatture (anno, ultimo_numero) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, year);
                        insertStmt.setInt(2, nextNumber);
                        insertStmt.executeUpdate();
                    }
                }
                
                // Format the invoice number (e.g., 2024/0001)
                numero = String.format("%d/%04d", year, nextNumber);
            }
            
            conn.commit();
            return numero;
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}