// File: WarehouseWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
        super(parent, "Warehouse Management", true);
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
        
        // Tab Stock Status
        JPanel stockPanel = createStockPanel();
        tabbedPane.addTab("Stock Status", stockPanel);
        
        // Tab Movements
        JPanel movementsPanel = createMovementsPanel();
        tabbedPane.addTab("Movements", movementsPanel);
        
        // Tab Notifications
        JPanel notificationsPanel = createNotificationsPanel();
        tabbedPane.addTab("Notifications", notificationsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Stock table
        String[] columns = {"Code", "Product", "Quantity", "Minimum Stock", "Status", "Preferred Supplier"};
        stockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stockTable = new JTable(stockModel);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newMovementButton = new JButton("New Movement");
        JButton setMinStockButton = new JButton("Set Minimum Stock");
        JButton refreshButton = new JButton("Refresh");
        
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
        
        // Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        
        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        
        // Movements table
        String[] columns = {"Date", "Product", "Type", "Quantity", "Reason", "Document", "Notes"};
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
        
        // Notifications table
        String[] columns = {"Date", "Product", "Type", "Message", "Status"};
        notificationsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        notificationsTable = new JTable(notificationsModel);
       // Notification buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton markReadButton = new JButton("Mark as Read");
        JButton markHandledButton = new JButton("Mark as Handled");
        JButton refreshButton = new JButton("Refresh");
        
        markReadButton.addActionListener(e -> markSelectedNotifications("READ"));
        markHandledButton.addActionListener(e -> markSelectedNotifications("HANDLED"));
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
                    int quantity = rs.getInt("quantita");
                    row.add(quantity);
                    int minQuantity = rs.getInt("quantita_minima");
                    row.add(minQuantity > 0 ? minQuantity : "-");
                    
                    // Determine stock status
                    String status;
                    if (minQuantity > 0) {
                        if (quantity <= 0) {
                            status = "OUT OF STOCK";
                        } else if (quantity < minQuantity) {
                            status = "LOW STOCK";
                        } else {
                            status = "OK";
                        }
                    } else {
                        status = quantity <= 0 ? "OUT OF STOCK" : "OK";
                    }
                    row.add(status);
                    
                    row.add(rs.getString("fornitore_nome"));
                    stockModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading warehouse data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
                    
                    String document = rs.getString("documento_tipo");
                    if (document != null && !document.isEmpty()) {
                        document += " " + rs.getString("documento_numero");
                    }
                    row.add(document);
                    
                    row.add(rs.getString("note"));
                    movementsModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading movements: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
                "Error loading notifications: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
                    int productId = rs.getInt("id");
                    String productName = rs.getString("nome");
                    int quantity = rs.getInt("quantita");
                    int minQuantity = rs.getInt("quantita_minima");
                    
                    // Create notification
                    String message = String.format(
                        "Stock is below minimum (%d). Current quantity: %d",
                        minQuantity, quantity
                    );
                    
                    createNotification(productId, "MIN_STOCK", message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error checking minimum stock: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createNotification(int productId, String type, String message) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                INSERT INTO notifiche_magazzino 
                (prodotto_id, data, tipo, messaggio, stato)
                VALUES (?, CURRENT_TIMESTAMP, ?, ?, 'NEW')
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, productId);
                pstmt.setString(2, type);
                pstmt.setString(3, message);
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
                        
                        String document = rs.getString("documento_tipo");
                        if (document != null && !document.isEmpty()) {
                            document += " " + rs.getString("documento_numero");
                        }
                        row.add(document);
                        
                        row.add(rs.getString("note"));
                        movementsModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error searching for movements: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markSelectedNotifications(String newStatus) {
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
                        String product = (String)notificationsModel.getValueAt(row, 1);
                        String type = (String)notificationsModel.getValueAt(row, 2);
                        
                        pstmt.setString(1, newStatus);
                        pstmt.setInt(2, getProdottoIdByNome(product));
                        pstmt.setTimestamp(3, new Timestamp(dateFormat.parse(data).getTime()));
                        pstmt.setString(4, type);
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
                "Error updating notifications: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
            throw new SQLException("Product not found: " + nome);
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
                "Select a product to set the minimum stock",
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String code = (String)stockModel.getValueAt(selectedRow, 0);
        try {
            MinimumStock minStock = loadMinimumStock(code);
            MinimumStockDialog dialog = new MinimumStockDialog(this, minStock);
            dialog.setVisible(true);
            if (dialog.isStockSaved()) {
                loadData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading minimum stock data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private MinimumStock loadMinimumStock(String code) throws SQLException {
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
            pstmt.setString(1, code);
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
        // Return a default object if the product has no minimum stock settings
        return new MinimumStock(
            0, "", 0, 0, 0, null, null, ""
        );
    }

    class MinimumStockDialog extends JDialog {
        private MinimumStock minStock;
        private boolean stockSaved = false;
        
        private JSpinner minQuantitySpinner;
        private JSpinner reorderQuantitySpinner;
        private JSpinner leadTimeSpinner;
        private JComboBox<SupplierComboItem> supplierCombo;
        private JTextArea notesArea;
        
        public MinimumStockDialog(JDialog parent, MinimumStock minStock) {
            super(parent, "Minimum Stock Management", true);
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
            
            // Product
            gbc.gridx = 0; gbc.gridy = 0;
            formPanel.add(new JLabel("Product:"), gbc);
            
            gbc.gridx = 1;
            JTextField productField = new JTextField(minStock.getProdottoNome());
            productField.setEditable(false);
            formPanel.add(productField, gbc);
            
            // Minimum quantity
            gbc.gridx = 0; gbc.gridy = 1;
            formPanel.add(new JLabel("Minimum quantity:"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 999999, 1);
            minQuantitySpinner = new JSpinner(minModel);
            formPanel.add(minQuantitySpinner, gbc);
            
            // Reorder quantity
            gbc.gridx = 0; gbc.gridy = 2;
            formPanel.add(new JLabel("Reorder quantity:"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel reorderModel = new SpinnerNumberModel(0, 0, 999999, 1);
            reorderQuantitySpinner = new JSpinner(reorderModel);
            formPanel.add(reorderQuantitySpinner, gbc);
            
            // Lead time
            gbc.gridx = 0; gbc.gridy = 3;
            formPanel.add(new JLabel("Lead time (days):"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel leadTimeModel = new SpinnerNumberModel(0, 0, 365, 1);
            leadTimeSpinner = new JSpinner(leadTimeModel);
            formPanel.add(leadTimeSpinner, gbc);
            
            // Preferred supplier
            gbc.gridx = 0; gbc.gridy = 4;
            formPanel.add(new JLabel("Preferred supplier:"), gbc);
            
            gbc.gridx = 1;
            supplierCombo = new JComboBox<>();
            loadSuppliers();
            formPanel.add(supplierCombo, gbc);
            
            // Notes
            gbc.gridx = 0; gbc.gridy = 5;
            formPanel.add(new JLabel("Notes:"), gbc);
            
            gbc.gridx = 1;
            notesArea = new JTextArea(4, 30);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            formPanel.add(new JScrollPane(notesArea), gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");
            
            saveButton.addActionListener(e -> saveMinStock());
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            // Main layout
            add(new JScrollPane(formPanel), BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
        }
        
        private void loadSuppliers() {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String query = "SELECT id, ragione_sociale FROM fornitori ORDER BY ragione_sociale";
                
                supplierCombo.addItem(new SupplierComboItem(0, "- None -"));
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        supplierCombo.addItem(new SupplierComboItem(
                            rs.getInt("id"),
                            rs.getString("ragione_sociale")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error loading suppliers: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void loadData() {
            minQuantitySpinner.setValue(minStock.getQuantitaMinima());
            reorderQuantitySpinner.setValue(minStock.getQuantitaRiordino());
            leadTimeSpinner.setValue(minStock.getLeadTimeGiorni());
            
            if (minStock.getFornitorePreferito() != null) {
                for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                    SupplierComboItem item = supplierCombo.getItemAt(i);
                    if (item.getId() == minStock.getFornitorePreferito()) {
                        supplierCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            notesArea.setText(minStock.getNote());
        }
        
        private void saveMinStock() {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                // Check if a configuration already exists
                String checkQuery = "SELECT id FROM scorte_minime WHERE prodotto_id = ?";
                boolean exists = false;
                
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setInt(1, minStock.getProdottoId());
                    ResultSet rs = pstmt.executeQuery();
                    exists = rs.next();
                }
                
                SupplierComboItem selectedSupplier = (SupplierComboItem)supplierCombo.getSelectedItem();
                Integer supplierId = selectedSupplier.getId() > 0 ? selectedSupplier.getId() : null;
                
                if (exists) {
                    // Update
                    String updateQuery = """
                        UPDATE scorte_minime SET
                            quantita_minima = ?, quantita_riordino = ?,
                            lead_time_giorni = ?, fornitore_preferito_id = ?,
                            note = ?
                        WHERE prodotto_id = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                        pstmt.setInt(1, (Integer)minQuantitySpinner.getValue());
                        pstmt.setInt(2, (Integer)reorderQuantitySpinner.getValue());
                        pstmt.setInt(3, (Integer)leadTimeSpinner.getValue());
                        if (supplierId != null) {
                            pstmt.setInt(4, supplierId);
                        } else {
                            pstmt.setNull(4, Types.INTEGER);
                        }
                        pstmt.setString(5, notesArea.getText().trim());
                        pstmt.setInt(6, minStock.getProdottoId());
                        pstmt.executeUpdate();
                    }
                } else {
                    // Insert
                    String insertQuery = """
                        INSERT INTO scorte_minime (
                            prodotto_id, quantita_minima, quantita_riordino,
                            lead_time_giorni, fornitore_preferito_id, note
                        ) VALUES (?, ?, ?, ?, ?, ?)
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                        pstmt.setInt(1, minStock.getProdottoId());
                        pstmt.setInt(2, (Integer)minQuantitySpinner.getValue());
                        pstmt.setInt(3, (Integer)reorderQuantitySpinner.getValue());
                        pstmt.setInt(4, (Integer)leadTimeSpinner.getValue());
                        if (supplierId != null) {
                            pstmt.setInt(5, supplierId);
                        } else {
                            pstmt.setNull(5, Types.INTEGER);
                        }
                        pstmt.setString(6, notesArea.getText().trim());
                        pstmt.executeUpdate();
                    }
                }
                
                stockSaved = true;
                dispose();
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error saving minimum stock: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
}