
// File: WarehouseWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class WarehouseWindow extends JDialog {
    private JTabbedPane tabbedPane;
    private JTable stockTable;
    private JTable movementsTable;
    private JTable notificationsTable;
    private DefaultTableModel stockModel;
    private DefaultTableModel movementsModel;
    private DefaultTableModel notificationsModel;
    private SimpleDateFormat dateFormat;
    
    public WarehouseWindow(JFrame parent) {
        super(parent, "Gestione Magazzino", true);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        setupWindow();
        initComponents();
        loadData();
    }
    
    private void setupWindow() {
        setSize(1000, 700);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        
        // Tab Situazione Magazzino
        JPanel stockPanel = createStockPanel();
        tabbedPane.addTab("Situazione Magazzino", stockPanel);
        
        // Tab Movimenti
        JPanel movementsPanel = createMovementsPanel();
        tabbedPane.addTab("Movimenti", movementsPanel);
        
        // Tab Notifiche
        JPanel notificationsPanel = createNotificationsPanel();
        tabbedPane.addTab("Notifiche", notificationsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Tabella stock
        String[] columns = {"Codice", "Prodotto", "Quantità", "Scorta Minima", "Stato", "Fornitore Preferito"};
        stockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stockTable = new JTable(stockModel);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newMovementButton = new JButton("Nuovo Movimento");
        JButton setMinStockButton = new JButton("Imposta Scorta Minima");
        JButton refreshButton = new JButton("Aggiorna");
        
        newMovementButton.addActionListener(e -> showMovementDialog(null));
        setMinStockButton.addActionListener(e -> showMinStockDialog());
        refreshButton.addActionListener(e -> loadStockData());
        
        buttonPanel.add(newMovementButton);
        buttonPanel.add(setMinStockButton);
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMovementsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Filtri
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Cerca");
        
        filterPanel.add(new JLabel("Cerca:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        
        // Tabella movimenti
        String[] columns = {"Data", "Prodotto", "Tipo", "Quantità", "Causale", "Documento", "Note"};
        movementsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movementsTable = new JTable(movementsModel);
        
        searchButton.addActionListener(e -> searchMovements(searchField.getText()));
        
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(movementsTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createNotificationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Tabella notifiche
        String[] columns = {"Data", "Prodotto", "Tipo", "Messaggio", "Stato"};
        notificationsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        notificationsTable = new JTable(notificationsModel);
     // Pulsanti notifiche
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton markReadButton = new JButton("Segna come Letta");
        JButton markHandledButton = new JButton("Segna come Gestita");
        JButton refreshButton = new JButton("Aggiorna");
        
        markReadButton.addActionListener(e -> markSelectedNotifications("LETTA"));
        markHandledButton.addActionListener(e -> markSelectedNotifications("GESTITA"));
        refreshButton.addActionListener(e -> loadNotificationsData());
        
        buttonPanel.add(markReadButton);
        buttonPanel.add(markHandledButton);
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(notificationsTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadData() {
        loadStockData();
        loadMovementsData();
        loadNotificationsData();
        checkLowStock();
    }
    
    private void loadStockData() {
        stockModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, sm.quantita_minima, sm.quantita_riordino,
                       f.ragione_sociale as fornitore_nome
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                LEFT JOIN fornitori f ON sm.fornitore_preferito_id = f.id
                ORDER BY p.nome
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("codice"));
                    row.add(rs.getString("nome"));
                    int quantita = rs.getInt("quantita");
                    row.add(quantita);
                    int quantitaMinima = rs.getInt("quantita_minima");
                    row.add(quantitaMinima > 0 ? quantitaMinima : "-");
                    
                    // Determina lo stato delle scorte
                    String stato;
                    if (quantitaMinima > 0) {
                        if (quantita <= 0) {
                            stato = "ESAURITO";
                        } else if (quantita < quantitaMinima) {
                            stato = "SOTTO SCORTA";
                        } else {
                            stato = "OK";
                        }
                    } else {
                        stato = quantita <= 0 ? "ESAURITO" : "OK";
                    }
                    row.add(stato);
                    
                    row.add(rs.getString("fornitore_nome"));
                    stockModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei dati di magazzino: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadMovementsData() {
        movementsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT m.*, p.nome as prodotto_nome
                FROM movimenti_magazzino m
                JOIN prodotti p ON m.prodotto_id = p.id
                ORDER BY m.data DESC
                LIMIT 100
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(dateFormat.format(rs.getTimestamp("data")));
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("tipo"));
                    row.add(rs.getInt("quantita"));
                    row.add(rs.getString("causale"));
                    
                    String documento = rs.getString("documento_tipo");
                    if (documento != null && !documento.isEmpty()) {
                        documento += " " + rs.getString("documento_numero");
                    }
                    row.add(documento);
                    
                    row.add(rs.getString("note"));
                    movementsModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei movimenti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadNotificationsData() {
        notificationsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT n.*, p.nome as prodotto_nome
                FROM notifiche_magazzino n
                JOIN prodotti p ON n.prodotto_id = p.id
                WHERE n.stato != 'GESTITA'
                ORDER BY n.data DESC
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(dateFormat.format(rs.getTimestamp("data")));
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("tipo"));
                    row.add(rs.getString("messaggio"));
                    row.add(rs.getString("stato"));
                    notificationsModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento delle notifiche: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void checkLowStock() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.id, p.nome, p.quantita, sm.quantita_minima, sm.quantita_riordino
                FROM prodotti p
                JOIN scorte_minime sm ON p.id = sm.prodotto_id
                WHERE p.quantita <= sm.quantita_minima
                AND NOT EXISTS (
                    SELECT 1 FROM notifiche_magazzino n
                    WHERE n.prodotto_id = p.id
                    AND n.tipo = 'SCORTA_MINIMA'
                    AND n.stato != 'GESTITA'
                )
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    int prodottoId = rs.getInt("id");
                    String prodottoNome = rs.getString("nome");
                    int quantita = rs.getInt("quantita");
                    int quantitaMinima = rs.getInt("quantita_minima");
                    
                    // Crea notifica
                    String messaggio = String.format(
                        "Scorta sotto il minimo (%d). Quantità attuale: %d",
                        quantitaMinima, quantita
                    );
                    
                    createNotification(prodottoId, "SCORTA_MINIMA", messaggio);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il controllo delle scorte minime: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createNotification(int prodottoId, String tipo, String messaggio) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                INSERT INTO notifiche_magazzino 
                (prodotto_id, data, tipo, messaggio, stato)
                VALUES (?, CURRENT_TIMESTAMP, ?, ?, 'NUOVA')
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, prodottoId);
                pstmt.setString(2, tipo);
                pstmt.setString(3, messaggio);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void searchMovements(String searchTerm) {
        if (searchTerm.trim().isEmpty()) {
            loadMovementsData();
            return;
        }
        
        movementsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT m.*, p.nome as prodotto_nome
                FROM movimenti_magazzino m
                JOIN prodotti p ON m.prodotto_id = p.id
                WHERE p.nome LIKE ? 
                   OR m.causale LIKE ?
                   OR m.documento_numero LIKE ?
                ORDER BY m.data DESC
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(dateFormat.format(rs.getTimestamp("data")));
                        row.add(rs.getString("prodotto_nome"));
                        row.add(rs.getString("tipo"));
                        row.add(rs.getInt("quantita"));
                        row.add(rs.getString("causale"));
                        
                        String documento = rs.getString("documento_tipo");
                        if (documento != null && !documento.isEmpty()) {
                            documento += " " + rs.getString("documento_numero");
                        }
                        row.add(documento);
                        
                        row.add(rs.getString("note"));
                        movementsModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la ricerca dei movimenti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markSelectedNotifications(String newStato) {
        int[] selectedRows = notificationsTable.getSelectedRows();
        if (selectedRows.length == 0) return;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            StringBuilder query = new StringBuilder(
                "UPDATE notifiche_magazzino SET stato = ? " +
                "WHERE prodotto_id = ? AND data = ? AND tipo = ?"
            );
            
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
                    for (int row : selectedRows) {
                        String data = (String)notificationsModel.getValueAt(row, 0);
                        String prodotto = (String)notificationsModel.getValueAt(row, 1);
                        String tipo = (String)notificationsModel.getValueAt(row, 2);
                        
                        pstmt.setString(1, newStato);
                        // Nota: qui dovresti usare l'ID del prodotto invece del nome
                        // Ho semplificato per brevità
                        pstmt.setInt(2, getProdottoIdByNome(prodotto));
                        pstmt.setTimestamp(3, new Timestamp(dateFormat.parse(data).getTime()));
                        pstmt.setString(4, tipo);
                        pstmt.executeUpdate();
                    }
                    
                    conn.commit();
                    loadNotificationsData();
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante l'aggiornamento delle notifiche: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int getProdottoIdByNome(String nome) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = "SELECT id FROM prodotti WHERE nome = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nome);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Prodotto non trovato: " + nome);
        }
    }
    
    private void showMovementDialog(WarehouseMovement movement) {
        WarehouseMovementDialog dialog = new WarehouseMovementDialog(this, movement);
        dialog.setVisible(true);
        if (dialog.isMovementSaved()) {
            loadData();
        }
    }
    
    private void showMinStockDialog() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleziona un prodotto per impostare la scorta minima",
                "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String codice = (String)stockModel.getValueAt(selectedRow, 0);
        try {
            MinimumStock minStock = loadMinimumStock(codice);
            MinimumStockDialog dialog = new MinimumStockDialog(this, minStock);
            dialog.setVisible(true);
            if (dialog.isStockSaved()) {
                loadData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento delle scorte minime: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private MinimumStock loadMinimumStock(String codice) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = """
            SELECT p.id, p.nome, sm.quantita_minima, sm.quantita_riordino,
                   sm.lead_time_giorni, sm.fornitore_preferito_id,
                   f.ragione_sociale as fornitore_nome, sm.note
            FROM prodotti p
            LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
            LEFT JOIN fornitori f ON sm.fornitore_preferito_id = f.id
            WHERE p.codice = ?
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, codice);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
            	return new MinimumStock(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getInt("quantita_minima"),
                        rs.getInt("quantita_riordino"),
                        rs.getInt("lead_time_giorni"),
                        rs.getObject("fornitore_preferito_id") != null ? 
                            rs.getInt("fornitore_preferito_id") : null,
                        rs.getString("fornitore_nome"),
                        rs.getString("note")
                    );
                }
            }
            return new MinimumStock(
                0, "", 0, 0, 0, null, null, ""
            );
        }
    }

    class MinimumStockDialog extends JDialog {
        private MinimumStock minStock;
        private boolean stockSaved = false;
        
        private JSpinner quantitaMinimaSpinner;
        private JSpinner quantitaRiordinoSpinner;
        private JSpinner leadTimeSpinner;
        private JComboBox<SupplierComboItem> fornitoreCombo;
        private JTextArea noteArea;
        
        public MinimumStockDialog(JDialog parent, MinimumStock minStock) {
            super(parent, "Gestione Scorta Minima", true);
            this.minStock = minStock;
            
            setupWindow();
            initComponents();
            loadData();
        }
        
        private void setupWindow() {
            setSize(400, 500);
            setLocationRelativeTo(getOwner());
            setLayout(new BorderLayout(10, 10));
        }
        
        private void initComponents() {
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Prodotto
            gbc.gridx = 0; gbc.gridy = 0;
            formPanel.add(new JLabel("Prodotto:"), gbc);
            
            gbc.gridx = 1;
            JTextField prodottoField = new JTextField(minStock.getProdottoNome());
            prodottoField.setEditable(false);
            formPanel.add(prodottoField, gbc);
            
            // Quantità minima
            gbc.gridx = 0; gbc.gridy = 1;
            formPanel.add(new JLabel("Quantità minima:"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 999999, 1);
            quantitaMinimaSpinner = new JSpinner(minModel);
            formPanel.add(quantitaMinimaSpinner, gbc);
            
            // Quantità riordino
            gbc.gridx = 0; gbc.gridy = 2;
            formPanel.add(new JLabel("Quantità riordino:"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel reorderModel = new SpinnerNumberModel(0, 0, 999999, 1);
            quantitaRiordinoSpinner = new JSpinner(reorderModel);
            formPanel.add(quantitaRiordinoSpinner, gbc);
            
            // Lead time
            gbc.gridx = 0; gbc.gridy = 3;
            formPanel.add(new JLabel("Lead time (giorni):"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel leadTimeModel = new SpinnerNumberModel(0, 0, 365, 1);
            leadTimeSpinner = new JSpinner(leadTimeModel);
            formPanel.add(leadTimeSpinner, gbc);
            
            // Fornitore preferito
            gbc.gridx = 0; gbc.gridy = 4;
            formPanel.add(new JLabel("Fornitore preferito:"), gbc);
            
            gbc.gridx = 1;
            fornitoreCombo = new JComboBox<>();
            loadFornitori();
            formPanel.add(fornitoreCombo, gbc);
            
            // Note
            gbc.gridx = 0; gbc.gridy = 5;
            formPanel.add(new JLabel("Note:"), gbc);
            
            gbc.gridx = 1;
            noteArea = new JTextArea(4, 30);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);
            formPanel.add(new JScrollPane(noteArea), gbc);
            
            // Pulsanti
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("Salva");
            JButton cancelButton = new JButton("Annulla");
            
            saveButton.addActionListener(e -> saveMinStock());
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            // Layout principale
            add(new JScrollPane(formPanel), BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
        }
        
        private void loadFornitori() {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String query = "SELECT id, ragione_sociale FROM fornitori ORDER BY ragione_sociale";
                
                fornitoreCombo.addItem(new SupplierComboItem(0, "- Nessuno -"));
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        fornitoreCombo.addItem(new SupplierComboItem(
                            rs.getInt("id"),
                            rs.getString("ragione_sociale")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante il caricamento dei fornitori: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void loadData() {
            quantitaMinimaSpinner.setValue(minStock.getQuantitaMinima());
            quantitaRiordinoSpinner.setValue(minStock.getQuantitaRiordino());
            leadTimeSpinner.setValue(minStock.getLeadTimeGiorni());
            
            if (minStock.getFornitorePreferito() != null) {
                for (int i = 0; i < fornitoreCombo.getItemCount(); i++) {
                    SupplierComboItem item = fornitoreCombo.getItemAt(i);
                    if (item.getId() == minStock.getFornitorePreferito()) {
                        fornitoreCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            noteArea.setText(minStock.getNote());
        }
        
        private void saveMinStock() {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                // Verifica se esiste già una configurazione
                String checkQuery = "SELECT id FROM scorte_minime WHERE prodotto_id = ?";
                boolean exists = false;
                
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setInt(1, minStock.getProdottoId());
                    ResultSet rs = pstmt.executeQuery();
                    exists = rs.next();
                }
                
                SupplierComboItem selectedSupplier = (SupplierComboItem)fornitoreCombo.getSelectedItem();
                Integer fornitoreId = selectedSupplier.getId() > 0 ? selectedSupplier.getId() : null;
                
                if (exists) {
                    // Aggiorna
                    String updateQuery = """
                        UPDATE scorte_minime SET
                            quantita_minima = ?, quantita_riordino = ?,
                            lead_time_giorni = ?, fornitore_preferito_id = ?,
                            note = ?
                        WHERE prodotto_id = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                        pstmt.setInt(1, (Integer)quantitaMinimaSpinner.getValue());
                        pstmt.setInt(2, (Integer)quantitaRiordinoSpinner.getValue());
                        pstmt.setInt(3, (Integer)leadTimeSpinner.getValue());
                        if (fornitoreId != null) {
                            pstmt.setInt(4, fornitoreId);
                        } else {
                            pstmt.setNull(4, Types.INTEGER);
                        }
                        pstmt.setString(5, noteArea.getText().trim());
                        pstmt.setInt(6, minStock.getProdottoId());
                        pstmt.executeUpdate();
                    }
                } else {
                    // Inserisci
                    String insertQuery = """
                        INSERT INTO scorte_minime (
                            prodotto_id, quantita_minima, quantita_riordino,
                            lead_time_giorni, fornitore_preferito_id, note
                        ) VALUES (?, ?, ?, ?, ?, ?)
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                        pstmt.setInt(1, minStock.getProdottoId());
                        pstmt.setInt(2, (Integer)quantitaMinimaSpinner.getValue());
                        pstmt.setInt(3, (Integer)quantitaRiordinoSpinner.getValue());
                        pstmt.setInt(4, (Integer)leadTimeSpinner.getValue());
                        if (fornitoreId != null) {
                            pstmt.setInt(5, fornitoreId);
                        } else {
                            pstmt.setNull(5, Types.INTEGER);
                        }
                        pstmt.setString(6, noteArea.getText().trim());
                        pstmt.executeUpdate();
                    }
                }
                
                stockSaved = true;
                dispose();
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante il salvataggio della scorta minima: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        public boolean isStockSaved() {
            return stockSaved;
        }
    }

    class SupplierComboItem {
        private int id;
        private String display;
        
        public SupplierComboItem(int id, String display) {
            this.id = id;
            this.display = display;
        }
        
        public int getId() { return id; }
        
        @Override
        public String toString() {
            return display;
        }
    }
