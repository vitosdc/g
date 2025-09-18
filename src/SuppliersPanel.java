import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;

public class SuppliersPanel extends BasePanel {
    private JTable suppliersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton ordersButton;
    private JButton priceListButton;
    private JButton refreshButton;
    
    public SuppliersPanel(MainWindow parent) {
        super(parent, "Suppliers Management");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        addButton = createActionButton("New Supplier", "🏭", new Color(138, 43, 226));
        editButton = createActionButton("Edit", "✏️", new Color(255, 140, 0));
        deleteButton = createActionButton("Delete", "🗑️", new Color(220, 20, 60));
        ordersButton = createActionButton("Orders", "📦", new Color(70, 130, 180));
        priceListButton = createActionButton("Price List", "💰", new Color(34, 139, 34));
        refreshButton = createActionButton("Refresh", "🔄", new Color(105, 105, 105));
        
        // Initially disable edit, delete, orders and price list buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        ordersButton.setEnabled(false);
        priceListButton.setEnabled(false);
        
        // Add action listeners
        addButton.addActionListener(e -> showSupplierDialog(null));
        editButton.addActionListener(e -> editSelectedSupplier());
        deleteButton.addActionListener(e -> deleteSelectedSupplier());
        ordersButton.addActionListener(e -> showSupplierOrders());
        priceListButton.addActionListener(e -> showSupplierPriceList());
        refreshButton.addActionListener(e -> refreshData());
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        
        // Add components to toolbar
        toolbarPanel.add(addButton);
        toolbarPanel.add(editButton);
        toolbarPanel.add(deleteButton);
        toolbarPanel.add(Box.createHorizontalStrut(10)); // Small spacer
        toolbarPanel.add(ordersButton);
        toolbarPanel.add(priceListButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(searchPanel);
    }
    
    @Override
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        this.searchField = searchField;
        
        // Search functionality
        searchButton.addActionListener(e -> searchSuppliers());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> searchSuppliers());
        
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
                searchTimer = new Timer(500, evt -> searchSuppliers());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create suppliers table
        String[] columns = {"ID", "Company Name", "VAT Number", "Email", "Phone", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        suppliersTable = new JTable(tableModel);
        suppliersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener
        suppliersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to edit
        suppliersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedSupplier();
                }
            }
        });
        
        // Add table to content panel
        JScrollPane tableScrollPane = createStandardTable(suppliersTable);
        contentPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Statistics panel
        JPanel statsPanel = createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        statsPanel.setBackground(new Color(248, 248, 252));
        
        JLabel totalSuppliersLabel = new JLabel("Total Suppliers: 0", SwingConstants.CENTER);
        JLabel activeSuppliersLabel = new JLabel("Active Suppliers: 0", SwingConstants.CENTER);
        JLabel totalOrdersLabel = new JLabel("Total Orders: 0", SwingConstants.CENTER);
        
        Font statsFont = new Font("Arial", Font.BOLD, 12);
        totalSuppliersLabel.setFont(statsFont);
        activeSuppliersLabel.setFont(statsFont);
        totalOrdersLabel.setFont(statsFont);
        
        totalSuppliersLabel.setForeground(new Color(138, 43, 226));
        activeSuppliersLabel.setForeground(new Color(34, 139, 34));
        totalOrdersLabel.setForeground(new Color(70, 130, 180));
        
        statsPanel.add(totalSuppliersLabel);
        statsPanel.add(activeSuppliersLabel);
        statsPanel.add(totalOrdersLabel);
        
        return statsPanel;
    }
    
    @Override
    public void refreshData() {
        loadSuppliers();
    }
    
    private void loadSuppliers() {
        tableModel.setRowCount(0);
        int totalSuppliers = 0;
        int activeSuppliers = 0;
        int totalOrders = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT f.*, 
                       COUNT(DISTINCT of.id) as numero_ordini
                FROM fornitori f
                LEFT JOIN ordini_fornitori of ON f.id = of.fornitore_id
                GROUP BY f.id
                ORDER BY f.ragione_sociale
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("ragione_sociale"));
                    row.add(rs.getString("partita_iva"));
                    row.add(rs.getString("email"));
                    row.add(rs.getString("telefono"));
                    row.add(rs.getString("indirizzo"));
                    
                    tableModel.addRow(row);
                    totalSuppliers++;
                    
                    int supplierOrders = rs.getInt("numero_ordini");
                    if (supplierOrders > 0) {
                        activeSuppliers++;
                        totalOrders += supplierOrders;
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading suppliers: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalSuppliers, activeSuppliers, totalOrders);
    }
    
    private void searchSuppliers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshData();
            return;
        }
        
        tableModel.setRowCount(0);
        int totalSuppliers = 0;
        int activeSuppliers = 0;
        int totalOrders = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT f.*, 
                       COUNT(DISTINCT of.id) as numero_ordini
                FROM fornitori f
                LEFT JOIN ordini_fornitori of ON f.id = of.fornitore_id
                WHERE f.ragione_sociale LIKE ? 
                   OR f.partita_iva LIKE ? 
                   OR f.email LIKE ?
                GROUP BY f.id
                ORDER BY f.ragione_sociale
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
                        row.add(rs.getString("ragione_sociale"));
                        row.add(rs.getString("partita_iva"));
                        row.add(rs.getString("email"));
                        row.add(rs.getString("telefono"));
                        row.add(rs.getString("indirizzo"));
                        
                        tableModel.addRow(row);
                        totalSuppliers++;
                        
                        int supplierOrders = rs.getInt("numero_ordini");
                        if (supplierOrders > 0) {
                            activeSuppliers++;
                            totalOrders += supplierOrders;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching suppliers: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalSuppliers, activeSuppliers, totalOrders);
    }
    
    private void updateStatsPanel(int totalSuppliers, int activeSuppliers, int totalOrders) {
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() == 3) { // Stats panel has 3 labels
                    ((JLabel) panel.getComponent(0)).setText("Total Suppliers: " + totalSuppliers);
                    ((JLabel) panel.getComponent(1)).setText("Active Suppliers: " + activeSuppliers);
                    ((JLabel) panel.getComponent(2)).setText("Total Orders: " + totalOrders);
                    break;
                }
            }
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = suppliersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        ordersButton.setEnabled(isRowSelected);
        priceListButton.setEnabled(isRowSelected);
    }
    
    private void showSupplierDialog(Supplier supplier) {
        SupplierDialog dialog = new SupplierDialog(parentWindow, supplier);
        dialog.setVisible(true);
        if (dialog.isSupplierSaved()) {
            refreshData();
            showSuccessMessage("Supplier " + (supplier == null ? "created" : "updated") + " successfully!");
        }
    }
    
    private void editSelectedSupplier() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            Supplier supplier = new Supplier(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                "", // codice fiscale - not displayed in table
                (String)tableModel.getValueAt(selectedRow, 5), // indirizzo
                (String)tableModel.getValueAt(selectedRow, 4), // telefono
                (String)tableModel.getValueAt(selectedRow, 3), // email
                "", // pec
                "", // sito web
                ""  // note
            );
            showSupplierDialog(supplier);
        }
    }
    
    private void deleteSelectedSupplier() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 1);
            
            if (!showConfirmDialog(
                "Are you sure you want to delete supplier '" + nome + "'?\n" +
                "This action cannot be undone.", 
                "Confirm Deletion")) {
                return;
            }
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                // Check for existing dependencies
                boolean hasOrders = hasSupplierOrders(conn, id);
                boolean hasPriceList = hasSupplierPriceList(conn, id);
                boolean hasMinStockReferences = hasMinStockReferences(conn, id);
                
                if (hasOrders || hasPriceList || hasMinStockReferences) {
                    StringBuilder message = new StringBuilder();
                    message.append("Cannot delete supplier '").append(nome).append("' because it has:\n");
                    
                    if (hasOrders) message.append("- Existing orders\n");
                    if (hasPriceList) message.append("- Price list entries\n");  
                    if (hasMinStockReferences) message.append("- Minimum stock references\n");
                    
                    message.append("\nOptions:\n");
                    message.append("1. Delete/reassign related records first\n");
                    message.append("2. Use 'Force Delete' to remove all related data");
                    
                    String[] options = {"Cancel", "Force Delete (All Data)"};
                    int choice = JOptionPane.showOptionDialog(this,
                        message.toString(),
                        "Cannot Delete Supplier",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, options, options[0]);
                    
                    if (choice == 1) { // Force Delete
                        performCascadeDelete(conn, id, nome);
                    }
                    return;
                }
                
                // Safe to delete - no foreign key references
                String query = "DELETE FROM fornitori WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    refreshData();
                    showSuccessMessage("Supplier deleted successfully");
                }
            } catch (SQLException e) {
                showErrorMessage("Error deleting supplier: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void performCascadeDelete(Connection conn, int id, String nome) {
        if (!showConfirmDialog(
            "WARNING: This will permanently delete supplier '" + nome + "' and ALL related data:\n" +
            "- All orders from this supplier\n" +
            "- All price list entries\n" +
            "- All references in minimum stock settings\n\n" +
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
                    // Delete supplier order details
                    """
                    DELETE FROM dettagli_ordini_fornitori 
                    WHERE ordine_id IN (
                        SELECT id FROM ordini_fornitori WHERE fornitore_id = ?
                    )
                    """,
                    // Delete supplier orders
                    "DELETE FROM ordini_fornitori WHERE fornitore_id = ?",
                    // Delete price lists
                    "DELETE FROM listini_fornitori WHERE fornitore_id = ?",
                    // Remove supplier references from minimum stock (set to NULL)
                    "UPDATE scorte_minime SET fornitore_preferito_id = NULL WHERE fornitore_preferito_id = ?",
                    // Finally delete the supplier
                    "DELETE FROM fornitori WHERE id = ?"
                };
                
                for (String query : deleteQueries) {
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                    }
                }
                
                conn.commit();
                refreshData();
                showSuccessMessage("Supplier '" + nome + "' and all related records deleted successfully");
                
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
    
    // Helper methods for dependency checking
    private boolean hasSupplierOrders(Connection conn, int supplierId) throws SQLException {
        String query = "SELECT COUNT(*) FROM ordini_fornitori WHERE fornitore_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, supplierId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasSupplierPriceList(Connection conn, int supplierId) throws SQLException {
        String query = "SELECT COUNT(*) FROM listini_fornitori WHERE fornitore_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, supplierId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasMinStockReferences(Connection conn, int supplierId) throws SQLException {
        String query = "SELECT COUNT(*) FROM scorte_minime WHERE fornitore_preferito_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, supplierId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
    private void showSupplierOrders() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int supplierId = (int)tableModel.getValueAt(selectedRow, 0);
            String supplierName = (String)tableModel.getValueAt(selectedRow, 1);
            
            SupplierOrdersWindow ordersWindow = new SupplierOrdersWindow(parentWindow, supplierId, supplierName);
            ordersWindow.setVisible(true);
        }
    }
    
    private void showSupplierPriceList() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int supplierId = (int)tableModel.getValueAt(selectedRow, 0);
            String supplierName = (String)tableModel.getValueAt(selectedRow, 1);
            
            SupplierPriceListWindow priceListWindow = new SupplierPriceListWindow(parentWindow, supplierId, supplierName);
            priceListWindow.setVisible(true);
        }
    }
}