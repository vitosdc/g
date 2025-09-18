import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;

public class ProductsPanel extends BasePanel {
    private JTable productsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    
    public ProductsPanel(MainWindow parent) {
        super(parent, "Product Management");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        addButton = createActionButton("New Product", "📦", new Color(255, 140, 0));
        editButton = createActionButton("Edit", "✏️", new Color(70, 130, 180));
        deleteButton = createActionButton("Delete", "🗑️", new Color(220, 20, 60));
        refreshButton = createActionButton("Refresh", "🔄", new Color(34, 139, 34));
        
        // Initially disable edit and delete buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        // Add action listeners
        addButton.addActionListener(e -> showProductDialog(null));
        editButton.addActionListener(e -> editSelectedProduct());
        deleteButton.addActionListener(e -> deleteSelectedProduct());
        refreshButton.addActionListener(e -> refreshData());
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        
        // Add components to toolbar
        toolbarPanel.add(addButton);
        toolbarPanel.add(editButton);
        toolbarPanel.add(deleteButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(searchPanel);
    }
    
    @Override
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        this.searchField = searchField;
        
        // Search functionality
        searchButton.addActionListener(e -> searchProducts());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> searchProducts());
        
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
                searchTimer = new Timer(500, evt -> searchProducts());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create products table
        String[] columns = {"ID", "Code", "Name", "Description", "Price €", "Quantity", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        productsTable = new JTable(tableModel);
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for status column
        productsTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        
        // Add selection listener
        productsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to edit
        productsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedProduct();
                }
            }
        });
        
        // Add table to content panel
        JScrollPane tableScrollPane = createStandardTable(productsTable);
        contentPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Statistics panel
        JPanel statsPanel = createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.SOUTH);
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
        JLabel outOfStockLabel = new JLabel("Out of Stock: 0", SwingConstants.CENTER);
        
        Font statsFont = new Font("Arial", Font.BOLD, 12);
        totalProductsLabel.setFont(statsFont);
        totalValueLabel.setFont(statsFont);
        lowStockLabel.setFont(statsFont);
        outOfStockLabel.setFont(statsFont);
        
        totalProductsLabel.setForeground(new Color(70, 130, 180));
        totalValueLabel.setForeground(new Color(34, 139, 34));
        lowStockLabel.setForeground(new Color(255, 140, 0));
        outOfStockLabel.setForeground(new Color(220, 20, 60));
        
        statsPanel.add(totalProductsLabel);
        statsPanel.add(totalValueLabel);
        statsPanel.add(lowStockLabel);
        statsPanel.add(outOfStockLabel);
        
        return statsPanel;
    }
    
    // Custom cell renderer for status column
    private class StatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
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
    
    @Override
    public void refreshData() {
        loadProducts();
    }
    
    private void loadProducts() {
        tableModel.setRowCount(0);
        int totalProducts = 0;
        double totalValue = 0;
        int lowStock = 0;
        int outOfStock = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, COALESCE(sm.quantita_minima, 0) as quantita_minima
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                ORDER BY p.nome
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("codice"));
                    row.add(rs.getString("nome"));
                    row.add(rs.getString("descrizione"));
                    
                    double price = rs.getDouble("prezzo");
                    int quantity = rs.getInt("quantita");
                    int minQuantity = rs.getInt("quantita_minima");
                    
                    row.add(String.format("%.2f", price));
                    row.add(quantity);
                    
                    // Determine status
                    String status;
                    if (quantity <= 0) {
                        status = "OUT OF STOCK";
                        outOfStock++;
                    } else if (minQuantity > 0 && quantity < minQuantity) {
                        status = "LOW STOCK";
                        lowStock++;
                    } else {
                        status = "OK";
                    }
                    row.add(status);
                    
                    tableModel.addRow(row);
                    totalProducts++;
                    totalValue += quantity * price;
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading products: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalProducts, totalValue, lowStock, outOfStock);
    }
    
    private void searchProducts() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshData();
            return;
        }
        
        tableModel.setRowCount(0);
        int totalProducts = 0;
        double totalValue = 0;
        int lowStock = 0;
        int outOfStock = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, COALESCE(sm.quantita_minima, 0) as quantita_minima
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                WHERE p.codice LIKE ? OR p.nome LIKE ? OR p.descrizione LIKE ?
                ORDER BY p.nome
            """;
            String searchPattern = "%" + searchTerm + "%";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getInt("id"));
                        row.add(rs.getString("codice"));
                        row.add(rs.getString("nome"));
                        row.add(rs.getString("descrizione"));
                        
                        double price = rs.getDouble("prezzo");
                        int quantity = rs.getInt("quantita");
                        int minQuantity = rs.getInt("quantita_minima");
                        
                        row.add(String.format("%.2f", price));
                        row.add(quantity);
                        
                        // Determine status
                        String status;
                        if (quantity <= 0) {
                            status = "OUT OF STOCK";
                            outOfStock++;
                        } else if (minQuantity > 0 && quantity < minQuantity) {
                            status = "LOW STOCK";
                            lowStock++;
                        } else {
                            status = "OK";
                        }
                        row.add(status);
                        
                        tableModel.addRow(row);
                        totalProducts++;
                        totalValue += quantity * price;
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching products: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalProducts, totalValue, lowStock, outOfStock);
    }
    
    private void updateStatsPanel(int totalProducts, double totalValue, int lowStock, int outOfStock) {
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() == 4) { // Stats panel has 4 labels
                    ((JLabel) panel.getComponent(0)).setText("Total Products: " + totalProducts);
                    ((JLabel) panel.getComponent(1)).setText(String.format("Total Value: € %.2f", totalValue));
                    ((JLabel) panel.getComponent(2)).setText("Low Stock: " + lowStock);
                    ((JLabel) panel.getComponent(3)).setText("Out of Stock: " + outOfStock);
                    break;
                }
            }
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = productsTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void showProductDialog(Product product) {
        ProductDialog dialog = new ProductDialog(parentWindow, product);
        dialog.setVisible(true);
        if (dialog.isProductSaved()) {
            refreshData();
            showSuccessMessage("Product " + (product == null ? "created" : "updated") + " successfully!");
        }
    }
    
    private void editSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            Product product = new Product(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                (String)tableModel.getValueAt(selectedRow, 3),
                Double.parseDouble(((String)tableModel.getValueAt(selectedRow, 4)).replace(",", ".")),
                (int)tableModel.getValueAt(selectedRow, 5)
            );
            showProductDialog(product);
        }
    }
    
    private void deleteSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 2);
            
            if (!showConfirmDialog(
                "Are you sure you want to delete product '" + nome + "'?\n" +
                "This action cannot be undone.", 
                "Confirm Deletion")) {
                return;
            }
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                // Check for existing dependencies
                boolean hasOrders = hasProductInOrders(conn, id);
                boolean hasInvoices = hasProductInInvoices(conn, id);
                boolean hasSupplierOrders = hasProductInSupplierOrders(conn, id);
                boolean hasPriceLists = hasProductInPriceLists(conn, id);
                boolean hasWarehouseMovements = hasProductInWarehouseMovements(conn, id);
                boolean hasMinStock = hasProductInMinStock(conn, id);
                
                if (hasOrders || hasInvoices || hasSupplierOrders || hasPriceLists || hasWarehouseMovements || hasMinStock) {
                    StringBuilder message = new StringBuilder();
                    message.append("Cannot delete product '").append(nome).append("' because it has:\n");
                    
                    if (hasOrders) message.append("- Customer orders\n");
                    if (hasInvoices) message.append("- Invoice entries\n");
                    if (hasSupplierOrders) message.append("- Supplier orders\n");
                    if (hasPriceLists) message.append("- Price list entries\n");
                    if (hasWarehouseMovements) message.append("- Warehouse movements\n");
                    if (hasMinStock) message.append("- Minimum stock settings\n");
                    
                    message.append("\nOptions:\n");
                    message.append("1. Delete/reassign related records first\n");
                    message.append("2. Use 'Force Delete' to remove all related data");
                    
                    String[] options = {"Cancel", "Force Delete (All Data)"};
                    int choice = JOptionPane.showOptionDialog(this,
                        message.toString(),
                        "Cannot Delete Product",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, options, options[0]);
                    
                    if (choice == 1) { // Force Delete
                        performCascadeDelete(conn, id, nome);
                    }
                    return;
                }
                
                // Safe to delete - no foreign key references
                String query = "DELETE FROM prodotti WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    refreshData();
                    showSuccessMessage("Product deleted successfully");
                }
            } catch (SQLException e) {
                showErrorMessage("Error deleting product: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // Helper methods for dependency checking (same as in original ProductsWindow)
    private boolean hasProductInOrders(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM dettagli_ordine WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasProductInInvoices(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM dettagli_fattura WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasProductInSupplierOrders(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM dettagli_ordini_fornitori WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasProductInPriceLists(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM listini_fornitori WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasProductInWarehouseMovements(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM movimenti_magazzino WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasProductInMinStock(Connection conn, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM scorte_minime WHERE prodotto_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
    private void performCascadeDelete(Connection conn, int id, String nome) {
        // Implementation similar to ProductsWindow but with new UI messages
        if (!showConfirmDialog(
            "WARNING: This will permanently delete product '" + nome + "' and ALL related data:\n" +
            "- All customer orders containing this product\n" +
            "- All invoice entries\n" +
            "- All supplier orders\n" +
            "- All price list entries\n" +
            "- All warehouse movements\n" +
            "- All minimum stock settings\n\n" +
            "This action CANNOT be undone!\n\n" +
            "Are you absolutely sure?",
            "FORCE DELETE - Final Confirmation")) {
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            try {
                // Delete in order to respect foreign key constraints
                String[] deleteQueries = {
                    "DELETE FROM dettagli_ordine WHERE prodotto_id = ?",
                    "DELETE FROM dettagli_fattura WHERE prodotto_id = ?",
                    "DELETE FROM dettagli_ordini_fornitori WHERE prodotto_id = ?",
                    "DELETE FROM listini_fornitori WHERE prodotto_id = ?",
                    "DELETE FROM movimenti_magazzino WHERE prodotto_id = ?",
                    "DELETE FROM notifiche_magazzino WHERE prodotto_id = ?",
                    "DELETE FROM scorte_minime WHERE prodotto_id = ?",
                    "DELETE FROM prodotti WHERE id = ?"
                };
                
                for (String query : deleteQueries) {
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                    }
                }
                
                conn.commit();
                refreshData();
                showSuccessMessage("Product '" + nome + "' and all related records deleted successfully");
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            showErrorMessage("Error during force delete: " + e.getMessage());
            e.printStackTrace();
        }
    }
}