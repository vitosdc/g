import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarehousePanel extends BasePanel {
    private JTabbedPane warehouseTabs;
    private JTable stockTable;
    private JTable movementsTable;
    private JTable notificationsTable;
    private DefaultTableModel stockModel;
    private DefaultTableModel movementsModel;
    private DefaultTableModel notificationsModel;
    
    // Buttons for different tabs
    private JButton newMovementButton;
    private JButton setMinStockButton;
    private JButton markReadButton;
    private JButton markHandledButton;
    private JButton refreshButton;
    private JButton reportButton;
    
    private JTextField searchField;
    private SimpleDateFormat dateFormat;
    
    public WarehousePanel(MainWindow parent) {
        super(parent, "Warehouse Management");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        newMovementButton = createActionButton("New Movement", "📦", new Color(34, 139, 34));
        setMinStockButton = createActionButton("Min. Stock", "⚠️", new Color(255, 140, 0));
        markReadButton = createActionButton("Mark Read", "✓", new Color(70, 130, 180));
        markHandledButton = createActionButton("Mark Handled", "✓✓", new Color(50, 205, 50));
        reportButton = createActionButton("Report", "📊", new Color(138, 43, 226));
        refreshButton = createActionButton("Refresh", "🔄", new Color(105, 105, 105));
        
        // Initially disable some buttons
        setMinStockButton.setEnabled(false);
        markReadButton.setEnabled(false);
        markHandledButton.setEnabled(false);
        
        // Add action listeners
        newMovementButton.addActionListener(e -> showMovementDialog(null));
        setMinStockButton.addActionListener(e -> showMinStockDialog());
        markReadButton.addActionListener(e -> markSelectedNotifications("READ"));
        markHandledButton.addActionListener(e -> markSelectedNotifications("HANDLED"));
        reportButton.addActionListener(e -> showWarehouseReport());
        refreshButton.addActionListener(e -> refreshData());
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        
        // Add components to toolbar
        toolbarPanel.add(newMovementButton);
        toolbarPanel.add(setMinStockButton);
        toolbarPanel.add(Box.createHorizontalStrut(10)); // Small spacer
        toolbarPanel.add(markReadButton);
        toolbarPanel.add(markHandledButton);
        toolbarPanel.add(reportButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(searchPanel);
    }
    
    @Override
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        this.searchField = searchField;
        
        // Search functionality (applies to current tab)
        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshCurrentTab();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> performSearch());
        
        // Real-time search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private Timer searchTimer;
            
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            
            private void scheduleSearch() {
                if (searchTimer != null) searchTimer.stop();
                searchTimer = new Timer(500, evt -> performSearch());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create tabbed pane for different warehouse views
        warehouseTabs = new JTabbedPane();
        warehouseTabs.addChangeListener(e -> updateToolbarForCurrentTab());
        
        // Stock Status Tab
        JPanel stockPanel = createStockPanel();
        warehouseTabs.addTab("Stock Status", stockPanel);
        
        // Movements Tab  
        JPanel movementsPanel = createMovementsPanel();
        warehouseTabs.addTab("Movements", movementsPanel);
        
        // Notifications Tab
        JPanel notificationsPanel = createNotificationsPanel();
        warehouseTabs.addTab("Notifications", notificationsPanel);
        
        contentPanel.add(warehouseTabs, BorderLayout.CENTER);
        
        // Statistics panel
        JPanel statsPanel = createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Stock table
        String[] columns = {"Code", "Product", "Quantity", "Min. Stock", "Status", "Preferred Supplier"};
        stockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        stockTable = new JTable(stockModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for status column
        stockTable.getColumnModel().getColumn(4).setCellRenderer(new StockStatusCellRenderer());
        
        // Add selection listener
        stockTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to set minimum stock
        stockTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showMinStockDialog();
                }
            }
        });
        
        JScrollPane tableScrollPane = createStandardTable(stockTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMovementsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Movements table
        String[] columns = {"Date", "Product", "Type", "Quantity", "Reason", "Document", "Notes"};
        movementsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        movementsTable = new JTable(movementsModel);
        movementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for type column
        movementsTable.getColumnModel().getColumn(2).setCellRenderer(new MovementTypeCellRenderer());
        
        JScrollPane tableScrollPane = createStandardTable(movementsTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
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
        notificationsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Custom renderer for status column
        notificationsTable.getColumnModel().getColumn(4).setCellRenderer(new NotificationStatusCellRenderer());
        
        // Add selection listener
        notificationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        JScrollPane tableScrollPane = createStandardTable(notificationsTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        statsPanel.setBackground(new Color(248, 248, 252));
        
        JLabel totalProductsLabel = new JLabel("Total Products: 0", SwingConstants.CENTER);
        JLabel totalValueLabel = new JLabel("Total Value: € 0.00", SwingConstants.CENTER);
        JLabel lowStockLabel = new JLabel("Low Stock: 0", SwingConstants.CENTER);
        JLabel notificationsLabel = new JLabel("New Notifications: 0", SwingConstants.CENTER);
        
        Font statsFont = new Font("Arial", Font.BOLD, 12);
        totalProductsLabel.setFont(statsFont);
        totalValueLabel.setFont(statsFont);
        lowStockLabel.setFont(statsFont);
        notificationsLabel.setFont(statsFont);
        
        totalProductsLabel.setForeground(new Color(184, 134, 11));
        totalValueLabel.setForeground(new Color(34, 139, 34));
        lowStockLabel.setForeground(new Color(255, 140, 0));
        notificationsLabel.setForeground(new Color(220, 20, 60));
        
        statsPanel.add(totalProductsLabel);
        statsPanel.add(totalValueLabel);
        statsPanel.add(lowStockLabel);
        statsPanel.add(notificationsLabel);
        
        return statsPanel;
    }
    
    // Custom cell renderers
    private class StockStatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String status = value.toString();
                switch (status) {
                    case "OK":
                        c.setBackground(new Color(240, 255, 240));
                        setForeground(new Color(0, 120, 0));
                        break;
                    case "LOW STOCK":
                        c.setBackground(new Color(255, 248, 220));
                        setForeground(new Color(200, 120, 0));
                        break;
                    case "OUT OF STOCK":
                        c.setBackground(new Color(255, 240, 240));
                        setForeground(new Color(180, 50, 50));
                        break;
                    default:
                        if (row % 2 == 0) {
                            c.setBackground(Color.WHITE);
                        } else {
                            c.setBackground(new Color(248, 248, 252));
                        }
                        setForeground(Color.BLACK);
                }
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            
            return c;
        }
    }
    
    private class MovementTypeCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String type = value.toString();
                if ("INWARD".equals(type)) {
                    c.setBackground(new Color(240, 255, 240));
                    setForeground(new Color(0, 120, 0));
                } else if ("OUTWARD".equals(type)) {
                    c.setBackground(new Color(255, 240, 240));
                    setForeground(new Color(180, 50, 50));
                } else {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(248, 248, 252));
                    }
                    setForeground(Color.BLACK);
                }
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            
            return c;
        }
    }
    
    private class NotificationStatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String status = value.toString();
                switch (status) {
                    case "NEW":
                        c.setBackground(new Color(255, 240, 240));
                        setForeground(new Color(180, 50, 50));
                        break;
                    case "READ":
                        c.setBackground(new Color(255, 248, 220));
                        setForeground(new Color(200, 120, 0));
                        break;
                    case "HANDLED":
                        c.setBackground(new Color(240, 255, 240));
                        setForeground(new Color(0, 120, 0));
                        break;
                    default:
                        if (row % 2 == 0) {
                            c.setBackground(Color.WHITE);
                        } else {
                            c.setBackground(new Color(248, 248, 252));
                        }
                        setForeground(Color.BLACK);
                }
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            
            return c;
        }
    }
    
    @Override
    public void refreshData() {
        loadStockData();
        loadMovementsData();
        loadNotificationsData();
        checkLowStock();
        updateStats();
    }
    
    private void refreshCurrentTab() {
        int selectedTab = warehouseTabs.getSelectedIndex();
        switch (selectedTab) {
            case 0: loadStockData(); break;
            case 1: loadMovementsData(); break;
            case 2: loadNotificationsData(); break;
        }
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        int selectedTab = warehouseTabs.getSelectedIndex();
        
        switch (selectedTab) {
            case 0: searchStock(searchTerm); break;
            case 1: searchMovements(searchTerm); break;
            case 2: searchNotifications(searchTerm); break;
        }
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
            showErrorMessage("Error loading stock data: " + e.getMessage());
            e.printStackTrace();
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
                LIMIT 200
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    
                    Date movementDate = DateUtils.parseDate(rs, "data");
                    if (movementDate != null) {
                        row.add(DateUtils.formatDate(movementDate, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("tipo"));
                    row.add(rs.getInt("quantita"));
                    row.add(rs.getString("causale"));
                    
                    String document = rs.getString("documento_tipo");
                    if (document != null && !document.isEmpty()) {
                        String docNumber = rs.getString("documento_numero");
                        if (docNumber != null && !docNumber.isEmpty()) {
                            document += " " + docNumber;
                        }
                    } else {
                        document = "";
                    }
                    row.add(document);
                    
                    row.add(rs.getString("note"));
                    movementsModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading movements: " + e.getMessage());
            e.printStackTrace();
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
                WHERE n.stato != 'HANDLED'
                ORDER BY n.data DESC
                LIMIT 100
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    
                    Date notificationDate = DateUtils.parseDate(rs, "data");
                    if (notificationDate != null) {
                        row.add(DateUtils.formatDate(notificationDate, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("tipo"));
                    row.add(rs.getString("messaggio"));
                    row.add(rs.getString("stato"));
                    notificationsModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void searchStock(String searchTerm) {
        if (searchTerm.isEmpty()) {
            loadStockData();
            return;
        }
        
        stockModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, sm.quantita_minima, sm.quantita_riordino,
                        f.ragione_sociale as fornitore_nome
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                LEFT JOIN fornitori f ON sm.fornitore_preferito_id = f.id
                WHERE p.nome LIKE ? OR p.codice LIKE ?
                ORDER BY p.nome
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getString("codice"));
                        row.add(rs.getString("nome"));
                        int quantity = rs.getInt("quantita");
                        row.add(quantity);
                        int minQuantity = rs.getInt("quantita_minima");
                        row.add(minQuantity > 0 ? minQuantity : "-");
                        
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
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching stock: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void searchMovements(String searchTerm) {
        if (searchTerm.isEmpty()) {
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
                LIMIT 100
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        
                        Date movementDate = DateUtils.parseDate(rs, "data");
                        if (movementDate != null) {
                            row.add(DateUtils.formatDate(movementDate, dateFormat));
                        } else {
                            row.add("");
                        }
                        
                        row.add(rs.getString("prodotto_nome"));
                        row.add(rs.getString("tipo"));
                        row.add(rs.getInt("quantita"));
                        row.add(rs.getString("causale"));
                        
                        String document = rs.getString("documento_tipo");
                        if (document != null && !document.isEmpty()) {
                            String docNumber = rs.getString("documento_numero");
                            if (docNumber != null && !docNumber.isEmpty()) {
                                document += " " + docNumber;
                            }
                        } else {
                            document = "";
                        }
                        row.add(document);
                        
                        row.add(rs.getString("note"));
                        movementsModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching movements: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void searchNotifications(String searchTerm) {
        if (searchTerm.isEmpty()) {
            loadNotificationsData();
            return;
        }
        
        notificationsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT n.*, p.nome as prodotto_nome
                FROM notifiche_magazzino n
                JOIN prodotti p ON n.prodotto_id = p.id
                WHERE n.stato != 'HANDLED'
                AND (p.nome LIKE ? OR n.messaggio LIKE ?)
                ORDER BY n.data DESC
                LIMIT 50
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        
                        Date notificationDate = DateUtils.parseDate(rs, "data");
                        if (notificationDate != null) {
                            row.add(DateUtils.formatDate(notificationDate, dateFormat));
                        } else {
                            row.add("");
                        }
                        
                        row.add(rs.getString("prodotto_nome"));
                        row.add(rs.getString("tipo"));
                        row.add(rs.getString("messaggio"));
                        row.add(rs.getString("stato"));
                        notificationsModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching notifications: " + e.getMessage());
            e.printStackTrace();
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
                    AND n.tipo = 'MIN_STOCK'
                    AND n.stato != 'HANDLED'
                    AND DATE(n.data) = DATE('now')
                )
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    int productId = rs.getInt("id");
                    String productName = rs.getString("nome");
                    int quantity = rs.getInt("quantita");
                    int minQuantity = rs.getInt("quantita_minima");
                    
                    String message = String.format(
                        "Stock is below minimum (%d). Current quantity: %d",
                        minQuantity, quantity
                    );
                    
                    createNotification(productId, "MIN_STOCK", message);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking minimum stock: " + e.getMessage());
            e.printStackTrace();
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
    
    private void updateStats() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            // Get statistics
            int totalProducts = 0;
            double totalValue = 0;
            int lowStock = 0;
            int newNotifications = 0;
            
            // Count products and calculate value
            String productQuery = """
                SELECT COUNT(*) as total, 
                       SUM(quantita * prezzo) as value,
                       SUM(CASE WHEN sm.quantita_minima > 0 AND p.quantita < sm.quantita_minima THEN 1 ELSE 0 END) as low_stock
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(productQuery)) {
                if (rs.next()) {
                    totalProducts = rs.getInt("total");
                    totalValue = rs.getDouble("value");
                    lowStock = rs.getInt("low_stock");
                }
            }
            
            // Count new notifications
            String notificationQuery = "SELECT COUNT(*) as count FROM notifiche_magazzino WHERE stato = 'NEW'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(notificationQuery)) {
                if (rs.next()) {
                    newNotifications = rs.getInt("count");
                }
            }
            
            // Update stats panel
            updateStatsPanel(totalProducts, totalValue, lowStock, newNotifications);
            
        } catch (SQLException e) {
            System.err.println("Error updating statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateStatsPanel(int totalProducts, double totalValue, int lowStock, int newNotifications) {
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() == 4) { // Stats panel has 4 labels
                    ((JLabel) panel.getComponent(0)).setText("Total Products: " + totalProducts);
                    ((JLabel) panel.getComponent(1)).setText(String.format("Total Value: € %.2f", totalValue));
                    ((JLabel) panel.getComponent(2)).setText("Low Stock: " + lowStock);
                    ((JLabel) panel.getComponent(3)).setText("New Notifications: " + newNotifications);
                    break;
                }
            }
        }
    }
    
    private void updateToolbarForCurrentTab() {
        int selectedTab = warehouseTabs.getSelectedIndex();
        
        // Hide/show buttons based on current tab
        switch (selectedTab) {
            case 0: // Stock Status
                setMinStockButton.setVisible(true);
                markReadButton.setVisible(false);
                markHandledButton.setVisible(false);
                break;
            case 1: // Movements
                setMinStockButton.setVisible(false);
                markReadButton.setVisible(false);
                markHandledButton.setVisible(false);
                break;
            case 2: // Notifications
                setMinStockButton.setVisible(false);
                markReadButton.setVisible(true);
                markHandledButton.setVisible(true);
                break;
        }
        
        updateButtonStates();
        toolbarPanel.revalidate();
        toolbarPanel.repaint();
    }
    
    private void updateButtonStates() {
        int selectedTab = warehouseTabs.getSelectedIndex();
        
        switch (selectedTab) {
            case 0: // Stock Status
                boolean stockRowSelected = stockTable.getSelectedRow() != -1;
                setMinStockButton.setEnabled(stockRowSelected);
                break;
            case 1: // Movements
                // No specific buttons to enable/disable for movements
                break;
            case 2: // Notifications
                boolean notificationRowSelected = notificationsTable.getSelectedRowCount() > 0;
                markReadButton.setEnabled(notificationRowSelected);
                markHandledButton.setEnabled(notificationRowSelected);
                break;
        }
    }
    
    private void showMovementDialog(WarehouseMovement movement) {
        WarehouseMovementDialog dialog = new WarehouseMovementDialog(parentWindow, movement);
        dialog.setVisible(true);
        if (dialog.isMovementSaved()) {
            refreshData();
            showSuccessMessage("Movement " + (movement == null ? "created" : "updated") + " successfully!");
        }
    }
    
    private void showMinStockDialog() {
        int selectedRow = stockTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarningMessage("Please select a product to set minimum stock");
            return;
        }
        
        String code = (String)stockModel.getValueAt(selectedRow, 0);
        try {
            MinimumStock minStock = loadMinimumStock(code);
            MinimumStockDialog dialog = new MinimumStockDialog(parentWindow, minStock);
            dialog.setVisible(true);
            if (dialog.isStockSaved()) {
                refreshData();
                showSuccessMessage("Minimum stock settings saved successfully!");
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading minimum stock data: " + e.getMessage());
            e.printStackTrace();
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
        return new MinimumStock(0, "", 0, 0, 0, null, null, "");
    }
    
    private void markSelectedNotifications(String newStatus) {
        int[] selectedRows = notificationsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showWarningMessage("Please select one or more notifications");
            return;
        }
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            try {
                for (int row : selectedRows) {
                    String dateStr = (String)notificationsModel.getValueAt(row, 0);
                    String product = (String)notificationsModel.getValueAt(row, 1);
                    String type = (String)notificationsModel.getValueAt(row, 2);
                    String message = (String)notificationsModel.getValueAt(row, 3);
                    
                    int notificationId = findNotificationId(dateStr, product, type, message);
                    if (notificationId > 0) {
                        String updateQuery = "UPDATE notifiche_magazzino SET stato = ? WHERE id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                            pstmt.setString(1, newStatus);
                            pstmt.setInt(2, notificationId);
                            pstmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                loadNotificationsData();
                updateStats();
                showSuccessMessage("Notifications updated successfully!");
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            showErrorMessage("Error updating notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int findNotificationId(String dateStr, String productName, String type, String message) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT n.id 
                FROM notifiche_magazzino n
                JOIN prodotti p ON n.prodotto_id = p.id
                WHERE p.nome = ? AND n.tipo = ? AND n.messaggio = ?
                ORDER BY n.data DESC
                LIMIT 1
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, productName);
                pstmt.setString(2, type);
                pstmt.setString(3, message);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private void showWarehouseReport() {
        WarehouseReportWindow reportWindow = new WarehouseReportWindow(parentWindow);
        reportWindow.setVisible(true);
    }
    
    // Inner class for MinimumStockDialog
    private class MinimumStockDialog extends JDialog {
        private MinimumStock minStock;
        private boolean stockSaved = false;
        
        private JSpinner minQuantitySpinner;
        private JSpinner reorderQuantitySpinner;
        private JSpinner leadTimeSpinner;
        private JComboBox<SupplierComboItem> supplierCombo;
        private JTextArea notesArea;
        
        public MinimumStockDialog(JFrame parent, MinimumStock minStock) {
            super(parent, "Minimum Stock Management", true);
            this.minStock = minStock;
            
            setupWindow();
            initComponents();
            loadData();
        }
        
        private void setupWindow() {
            setSize(450, 550);
            setLocationRelativeTo(getOwner());
            setLayout(new BorderLayout(10, 10));
        }
        
        private void initComponents() {
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Product
            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Product:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            JTextField productField = new JTextField(minStock.getProdottoNome());
            productField.setEditable(false);
            productField.setBackground(new Color(240, 240, 240));
            formPanel.add(productField, gbc);
            
            // Minimum quantity
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Minimum Quantity:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            SpinnerNumberModel minModel = new SpinnerNumberModel(0, 0, 999999, 1);
            minQuantitySpinner = new JSpinner(minModel);
            formPanel.add(minQuantitySpinner, gbc);
            
            // Reorder quantity
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Reorder Quantity:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            SpinnerNumberModel reorderModel = new SpinnerNumberModel(0, 0, 999999, 1);
            reorderQuantitySpinner = new JSpinner(reorderModel);
            formPanel.add(reorderQuantitySpinner, gbc);
            
            // Lead time
            gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Lead Time (days):"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            SpinnerNumberModel leadTimeModel = new SpinnerNumberModel(0, 0, 365, 1);
            leadTimeSpinner = new JSpinner(leadTimeModel);
            formPanel.add(leadTimeSpinner, gbc);
            
            // Preferred supplier
            gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Preferred Supplier:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            supplierCombo = new JComboBox<>();
            loadSuppliers();
            formPanel.add(supplierCombo, gbc);
            
            // Notes
            gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            formPanel.add(new JLabel("Notes:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            notesArea = new JTextArea(4, 25);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            JScrollPane notesScroll = new JScrollPane(notesArea);
            formPanel.add(notesScroll, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveButton = createActionButton("Save", "💾", new Color(34, 139, 34));
            JButton cancelButton = createActionButton("Cancel", "✖", new Color(150, 150, 150));
            
            saveButton.addActionListener(e -> saveMinStock());
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            mainPanel.add(formPanel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            add(mainPanel);
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
            
            notesArea.setText(minStock.getNote() != null ? minStock.getNote() : "");
        }
        
        private void saveMinStock() {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                String checkQuery = "SELECT prodotto_id FROM scorte_minime WHERE prodotto_id = ?";
                boolean exists = false;
                
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setInt(1, minStock.getProdottoId());
                    ResultSet rs = pstmt.executeQuery();
                    exists = rs.next();
                }
                
                SupplierComboItem selectedSupplier = (SupplierComboItem)supplierCombo.getSelectedItem();
                Integer supplierId = selectedSupplier.getId() > 0 ? selectedSupplier.getId() : null;
                
                if (exists) {
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
                JOptionPane.showMessageDialog(this,
                    "Error saving minimum stock: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
        
        public boolean isStockSaved() {
            return stockSaved;
        }
    }
    
    // Inner class for SupplierComboItem
    private class SupplierComboItem {
        private int id;
        private String display;
        
        public SupplierComboItem(int id, String display) {
            this.id = id;
            this.display = display;
        }
        
        public int getId() { 
            return id; 
        }
        
        @Override
        public String toString() {
            return display;
        }
    }
}