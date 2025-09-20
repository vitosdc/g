import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarehousePanel extends JPanel {
    private JTabbedPane tabbedPane;
    private JTable stockTable;
    private JTable movementsTable;
    private JTable notificationsTable;
    private DefaultTableModel stockModel;
    private DefaultTableModel movementsModel;
    private DefaultTableModel notificationsModel;
    private SimpleDateFormat dateFormat;
    
    public WarehousePanel() {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        setupPanel();
        initComponents();
        loadData();
    }
    
    private void setupPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
                WHERE n.stato != 'HANDLED'
                ORDER BY n.data DESC
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
            conn.setAutoCommit(false);
            
            try {
                String updateQuery = "UPDATE notifiche_magazzino SET stato = ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    for (int row : selectedRows) {
                        // Find notification ID by matching data
                        String dateStr = (String)notificationsModel.getValueAt(row, 0);
                        String product = (String)notificationsModel.getValueAt(row, 1);
                        String type = (String)notificationsModel.getValueAt(row, 2);
                        String message = (String)notificationsModel.getValueAt(row, 3);
                        
                        int notificationId = findNotificationId(dateStr, product, type, message);
                        if (notificationId > 0) {
                            pstmt.setString(1, newStatus);
                            pstmt.setInt(2, notificationId);
                            pstmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                loadNotificationsData();
                
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
    
    private void showMovementDialog(WarehouseMovement movement) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        WarehouseMovementDialog dialog;
        if (parentWindow instanceof JFrame) {
            dialog = new WarehouseMovementDialog((JFrame) parentWindow, movement);
        } else {
            dialog = new WarehouseMovementDialog((JDialog) parentWindow, movement);
        }
        
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
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            
            MinimumStockDialog dialog;
            if (parentWindow instanceof JFrame) {
                dialog = new MinimumStockDialog((JFrame) parentWindow, minStock);
            } else {
                dialog = new MinimumStockDialog((JDialog) parentWindow, minStock);
            }
            
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
}