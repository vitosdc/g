import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ProductsWindow extends JDialog {
    private JTable productsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JButton stockMovementButton;
    private Timer searchTimer;
    private static final int SEARCH_DELAY = 300;
    
    public ProductsWindow(JFrame parent) {
        super(parent, "Product Management", true);
        setupWindow();
        initComponents();
        loadProducts();
    }
    
    private void setupWindow() {
        setSize(1000, 650);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.PLAIN, 14f));
        
        searchField = new JTextField(25);
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN, 14f));
        searchField.setToolTipText("Search by code, name, or description");
        
        setupRealTimeSearch();
        
        JButton clearSearchButton = new JButton("✕");
        clearSearchButton.setFont(clearSearchButton.getFont().deriveFont(Font.PLAIN, 12f));
        clearSearchButton.setPreferredSize(new Dimension(30, 30));
        clearSearchButton.setToolTipText("Clear search");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadProducts();
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);
        
        // Products table with stock status indicators
        String[] columns = {"ID", "Code", "Name", "Description", "Price €", "Stock", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 4: return Double.class; // Price
                    case 5: return Integer.class; // Stock
                    default: return String.class;
                }
            }
        };
        
        productsTable = new JTable(tableModel);
        productsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productsTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Hide ID column
        productsTable.getColumnModel().getColumn(0).setMinWidth(0);
        productsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        productsTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Set column widths
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Code
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(180); // Name
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Description
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Price
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // Stock
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Status
        
        // Custom cell renderer for stock status
        productsTable.getColumnModel().getColumn(6).setCellRenderer(new StockStatusRenderer());
        
        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        productsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Price
        productsTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Stock
        
        // Double-click to edit
        productsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && productsTable.getSelectedRow() != -1) {
                    editSelectedProduct();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        
        addButton = createStyledButton("➕ Add Product", new Color(76, 175, 80));
        editButton = createStyledButton("✏️ Edit", new Color(33, 150, 243));
        deleteButton = createStyledButton("🗑️ Delete", new Color(244, 67, 54));
        stockMovementButton = createStyledButton("📦 Stock Movement", new Color(255, 152, 0));
        refreshButton = createStyledButton("🔄 Refresh", new Color(158, 158, 158));
        
        addButton.addActionListener(e -> showProductDialog(null));
        editButton.addActionListener(e -> editSelectedProduct());
        deleteButton.addActionListener(e -> deleteSelectedProduct());
        stockMovementButton.addActionListener(e -> showStockMovement());
        refreshButton.addActionListener(e -> loadProducts());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(stockMovementButton);
        buttonPanel.add(refreshButton);
        
        // Main layout
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    // Custom renderer for stock status
    private class StockStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String status = (String) value;
                switch (status) {
                    case "✅ In Stock":
                        c.setBackground(new Color(232, 245, 233));
                        c.setForeground(new Color(27, 94, 32));
                        break;
                    case "⚠️ Low Stock":
                        c.setBackground(new Color(255, 243, 224));
                        c.setForeground(new Color(230, 81, 0));
                        break;
                    case "❌ Out of Stock":
                        c.setBackground(new Color(255, 235, 238));
                        c.setForeground(new Color(198, 40, 40));
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                        break;
                }
            }
            
            return c;
        }
    }
    
    private void setupRealTimeSearch() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { scheduleSearch(); }
            
            @Override
            public void removeUpdate(DocumentEvent e) { scheduleSearch(); }
            
            @Override
            public void changedUpdate(DocumentEvent e) { scheduleSearch(); }
            
            private void scheduleSearch() {
                if (searchTimer != null) {
                    searchTimer.stop();
                }
                searchTimer = new Timer(SEARCH_DELAY, e -> performSearch());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
        
        searchField.addActionListener(e -> performSearch());
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadProducts();
        } else {
            searchProductsImproved(searchTerm);
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = productsTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        stockMovementButton.setEnabled(isRowSelected);
        
        if (isRowSelected) {
            int selectedRow = productsTable.getSelectedRow();
            String productName = (String)tableModel.getValueAt(selectedRow, 2);
            editButton.setToolTipText("Edit " + productName);
            deleteButton.setToolTipText("Delete " + productName);
            stockMovementButton.setToolTipText("Manage stock for " + productName);
        } else {
            editButton.setToolTipText("Select a product to edit");
            deleteButton.setToolTipText("Select a product to delete");
            stockMovementButton.setToolTipText("Select a product for stock movement");
        }
    }
    
    private void loadProducts() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Loading products...");
                
                tableModel.setRowCount(0);
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    String query = """
                        SELECT p.*, 
                               COALESCE(sm.quantita_minima, 0) as min_stock
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
                            row.add(rs.getDouble("prezzo"));
                            
                            int stock = rs.getInt("quantita");
                            int minStock = rs.getInt("min_stock");
                            row.add(stock);
                            
                            // Determine status
                            String status;
                            if (stock <= 0) {
                                status = "❌ Out of Stock";
                            } else if (minStock > 0 && stock < minStock) {
                                status = "⚠️ Low Stock";
                            } else {
                                status = "✅ In Stock";
                            }
                            row.add(status);
                            
                            tableModel.addRow(row);
                        }
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> 
                        NotificationManager.showDatabaseError("loading products")
                    );
                    e.printStackTrace();
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                SwingUtilities.invokeLater(() -> {
                    int count = tableModel.getRowCount();
                    NotificationManager.showInfo("Loaded " + count + " products");
                });
            }
        };
        
        worker.execute();
    }
    
    private void searchProductsImproved(String searchTerm) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Searching products...");
                
                List<Product> results = SmartSearchUtil.searchProductsImproved(searchTerm);
                
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    
                    for (Product product : results) {
                        // Get min stock for status
                        int minStock = getMinStock(product.getId());
                        
                        Vector<Object> row = new Vector<>();
                        row.add(product.getId());
                        row.add(product.getCodice());
                        row.add(product.getNome());
                        row.add(product.getDescrizione());
                        row.add(product.getPrezzo());
                        row.add(product.getQuantita());
                        
                        // Determine status
                        String status;
                        int stock = product.getQuantita();
                        if (stock <= 0) {
                            status = "❌ Out of Stock";
                        } else if (minStock > 0 && stock < minStock) {
                            status = "⚠️ Low Stock";
                        } else {
                            status = "✅ In Stock";
                        }
                        row.add(status);
                        
                        tableModel.addRow(row);
                    }
                });
                
                return null;
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                SwingUtilities.invokeLater(() -> {
                    int count = tableModel.getRowCount();
                    if (count == 0) {
                        NotificationManager.showWarning("No products found for: " + searchTerm);
                    } else {
                        NotificationManager.showInfo("Found " + count + " products");
                    }
                });
            }
        };
        
        worker.execute();
    }
    
    private int getMinStock(int productId) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT quantita_minima FROM scorte_minime WHERE prodotto_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("quantita_minima");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting minimum stock: " + e.getMessage());
        }
        return 0;
    }
    
    private void showProductDialog(Product product) {
        ProductDialog dialog = new ProductDialog(this, product);
        dialog.setVisible(true);
        if (dialog.isProductSaved()) {
            if (product == null) {
                NotificationManager.showSaveSuccess("Product");
            } else {
                NotificationManager.showSuccess("Product updated successfully");
            }
            loadProducts();
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
                (double)tableModel.getValueAt(selectedRow, 4),
                (int)tableModel.getValueAt(selectedRow, 5)
            );
            showProductDialog(product);
        }
    }
    
    private void deleteSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String name = (String)tableModel.getValueAt(selectedRow, 2);
            String code = (String)tableModel.getValueAt(selectedRow, 1);
            
            // Enhanced confirmation dialog
            Object[] options = {"Yes, Delete", "Cancel"};
            int result = JOptionPane.showOptionDialog(this,
                "Are you sure you want to delete product:\n\n" + 
                name + " [" + code + "]?\n\n" +
                "This action cannot be undone and may affect related orders and invoices.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
                
            if (result == 0) { // Yes, Delete
                deleteProductAsync(id, name);
            }
        }
    }
    
    private void deleteProductAsync(int id, String productName) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Deleting product...");
                
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    
                    // Check for related records
                    String checkQuery = """
                        SELECT 
                            (SELECT COUNT(*) FROM dettagli_ordine WHERE prodotto_id = ?) as order_count,
                            (SELECT COUNT(*) FROM dettagli_fattura WHERE prodotto_id = ?) as invoice_count,
                            (SELECT COUNT(*) FROM movimenti_magazzino WHERE prodotto_id = ?) as movement_count
                    """;
                    
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                        checkStmt.setInt(1, id);
                        checkStmt.setInt(2, id);
                        checkStmt.setInt(3, id);
                        ResultSet rs = checkStmt.executeQuery();
                        
                        if (rs.next()) {
                            int orderCount = rs.getInt("order_count");
                            int invoiceCount = rs.getInt("invoice_count");
                            int movementCount = rs.getInt("movement_count");
                            
                            if (orderCount > 0 || invoiceCount > 0 || movementCount > 0) {
                                SwingUtilities.invokeLater(() -> {
                                    NotificationManager.showError(
                                        "Cannot delete product: " + orderCount + " orders, " + 
                                        invoiceCount + " invoices, and " + movementCount + 
                                        " stock movements are linked to this product"
                                    );
                                });
                                return false;
                            }
                        }
                    }
                    
                    conn.setAutoCommit(false);
                    try {
                        // Delete related minimum stock settings
                        String deleteMinStockQuery = "DELETE FROM scorte_minime WHERE prodotto_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteMinStockQuery)) {
                            pstmt.setInt(1, id);
                            pstmt.executeUpdate();
                        }
                        
                        // Delete notifications
                        String deleteNotificationsQuery = "DELETE FROM notifiche_magazzino WHERE prodotto_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteNotificationsQuery)) {
                            pstmt.setInt(1, id);
                            pstmt.executeUpdate();
                        }
                        
                        // Delete product
                        String deleteProductQuery = "DELETE FROM prodotti WHERE id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteProductQuery)) {
                            pstmt.setInt(1, id);
                            int rowsAffected = pstmt.executeUpdate();
                            
                            if (rowsAffected > 0) {
                                conn.commit();
                                return true;
                            } else {
                                conn.rollback();
                                return false;
                            }
                        }
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                    
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> 
                        NotificationManager.showDatabaseError("deleting product")
                    );
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                try {
                    Boolean success = get();
                    if (success) {
                        NotificationManager.showDeleteSuccess("Product " + productName);
                        loadProducts();
                    }
                } catch (Exception e) {
                    NotificationManager.showError("Error deleting product: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void showStockMovement() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            int productId = (int)tableModel.getValueAt(selectedRow, 0);
            String productName = (String)tableModel.getValueAt(selectedRow, 2);
            int currentStock = (int)tableModel.getValueAt(selectedRow, 5);
            
            showStockMovementDialog(productId, productName, currentStock);
        }
    }
    
    private void showStockMovementDialog(int productId, String productName, int currentStock) {
        JDialog dialog = new JDialog(this, "Stock Movement - " + productName, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Current stock info
        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.add(new JLabel("Current Stock: " + currentStock + " pieces"));
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Movement type
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Movement Type:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"INWARD", "OUTWARD"});
        formPanel.add(typeCombo, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Quantity:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 9999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitySpinner, gbc);
        
        // Reason
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Reason:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> reasonCombo = new JComboBox<>(new String[]{
            "PURCHASE", "SALE", "CUSTOMER_RETURN", "SUPPLIER_RETURN", 
            "INVENTORY", "GIFT", "THEFT_LOSS", "OTHER"
        });
        formPanel.add(reasonCombo, gbc);
        
        // Notes
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        JTextArea notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(notesArea), gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("✓ Save Movement");
        JButton cancelButton = new JButton("✕ Cancel");
        
        saveButton.addActionListener(e -> {
            String type = (String)typeCombo.getSelectedItem();
            int quantity = (int)quantitySpinner.getValue();
            
            // Validate stock movement
            StockValidator.ValidationResult validation = 
                StockValidator.validateStockOperation(productId, quantity, type);
            
            if (!validation.isValid()) {
                NotificationManager.showValidationError(validation.getErrorMessage());
                
                if (!validation.getSuggestions().isEmpty()) {
                    StringBuilder suggestions = new StringBuilder("Suggestions:\n");
                    for (String suggestion : validation.getSuggestions()) {
                        suggestions.append("• ").append(suggestion).append("\n");
                    }
                    
                    JOptionPane.showMessageDialog(dialog, suggestions.toString(), 
                        "Suggestions", JOptionPane.INFORMATION_MESSAGE);
                }
                return;
            }
            
            if (validation.getWarningMessage() != null) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    validation.getWarningMessage() + "\n\nProceed anyway?",
                    "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Save movement
            saveStockMovement(productId, type, quantity, 
                (String)reasonCombo.getSelectedItem(), notesArea.getText().trim());
            
            dialog.dispose();
            loadProducts(); // Refresh table
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void saveStockMovement(int productId, String type, int quantity, String reason, String notes) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Saving stock movement...");
                
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    conn.setAutoCommit(false);
                    
                    try {
                        // Insert movement record
                        String movementQuery = """
                            INSERT INTO movimenti_magazzino 
                            (prodotto_id, data, tipo, quantita, causale, note)
                            VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?)
                        """;
                        
                        try (PreparedStatement pstmt = conn.prepareStatement(movementQuery)) {
                            pstmt.setInt(1, productId);
                            pstmt.setString(2, type);
                            pstmt.setInt(3, quantity);
                            pstmt.setString(4, reason);
                            pstmt.setString(5, notes);
                            pstmt.executeUpdate();
                        }
                        
                        // Update product stock
                        String updateStockQuery = """
                            UPDATE prodotti 
                            SET quantita = quantita + ?
                            WHERE id = ?
                        """;
                        
                        try (PreparedStatement pstmt = conn.prepareStatement(updateStockQuery)) {
                            int quantityDelta = type.equals("INWARD") ? quantity : -quantity;
                            pstmt.setInt(1, quantityDelta);
                            pstmt.setInt(2, productId);
                            pstmt.executeUpdate();
                        }
                        
                        conn.commit();
                        return true;
                        
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                    
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> 
                        NotificationManager.showDatabaseError("saving stock movement")
                    );
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                try {
                    Boolean success = get();
                    if (success) {
                        NotificationManager.showSuccess("Stock movement saved successfully");
                    }
                } catch (Exception e) {
                    NotificationManager.showError("Error saving stock movement: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
}