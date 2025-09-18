import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;

public class CustomersPanel extends BasePanel {
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    
    public CustomersPanel(MainWindow parent) {
        super(parent, "Customer Management");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        addButton = createActionButton("New Customer", "👥", new Color(34, 139, 34));
        editButton = createActionButton("Edit", "✏️", new Color(255, 140, 0));
        deleteButton = createActionButton("Delete", "🗑️", new Color(220, 20, 60));
        refreshButton = createActionButton("Refresh", "🔄", new Color(70, 130, 180));
        
        // Initially disable edit and delete buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        // Add action listeners
        addButton.addActionListener(e -> showCustomerDialog(null));
        editButton.addActionListener(e -> editSelectedCustomer());
        deleteButton.addActionListener(e -> deleteSelectedCustomer());
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
        searchButton.addActionListener(e -> searchCustomers());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> searchCustomers());
        
        // Real-time search (optional)
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
                searchTimer = new Timer(500, evt -> searchCustomers());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create customers table
        String[] columns = {"ID", "Name", "Surname", "Email", "Phone", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        customersTable = new JTable(tableModel);
        customersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener to enable/disable buttons
        customersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to edit
        customersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCustomer();
                }
            }
        });
        
        // Add table to content panel
        JScrollPane tableScrollPane = createStandardTable(customersTable);
        contentPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Statistics panel
        JPanel statsPanel = createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        statsPanel.setBackground(new Color(248, 248, 252));
        
        JLabel statsLabel = new JLabel("Total Customers: 0");
        statsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statsLabel.setForeground(new Color(70, 70, 70));
        statsPanel.add(statsLabel);
        
        return statsPanel;
    }
    
    @Override
    public void refreshData() {
        loadCustomers();
    }
    
    private void loadCustomers() {
        tableModel.setRowCount(0);
        int customerCount = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM clienti ORDER BY cognome, nome";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("nome"));
                    row.add(rs.getString("cognome"));
                    row.add(rs.getString("email"));
                    row.add(rs.getString("telefono"));
                    row.add(rs.getString("indirizzo"));
                    tableModel.addRow(row);
                    customerCount++;
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading customers: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(customerCount);
    }
    
    private void searchCustomers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshData();
            return;
        }
        
        tableModel.setRowCount(0);
        int customerCount = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT * FROM clienti 
                WHERE nome LIKE ? OR cognome LIKE ? OR email LIKE ? OR telefono LIKE ?
                ORDER BY cognome, nome
            """;
            String searchPattern = "%" + searchTerm + "%";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getInt("id"));
                        row.add(rs.getString("nome"));
                        row.add(rs.getString("cognome"));
                        row.add(rs.getString("email"));
                        row.add(rs.getString("telefono"));
                        row.add(rs.getString("indirizzo"));
                        tableModel.addRow(row);
                        customerCount++;
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching customers: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(customerCount);
    }
    
    private void updateStatsPanel(int count) {
        // Find stats panel and update label
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JLabel) {
                    JLabel statsLabel = (JLabel) panel.getComponent(0);
                    if (statsLabel.getText().startsWith("Total Customers:")) {
                        statsLabel.setText("Total Customers: " + count);
                        break;
                    }
                }
            }
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = customersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void showCustomerDialog(Customer customer) {
        CustomerDialog dialog = new CustomerDialog(parentWindow, customer);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            refreshData();
            showSuccessMessage("Customer " + (customer == null ? "created" : "updated") + " successfully!");
        }
    }
    
    private void editSelectedCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow != -1) {
            Customer customer = new Customer(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                (String)tableModel.getValueAt(selectedRow, 3),
                (String)tableModel.getValueAt(selectedRow, 4),
                (String)tableModel.getValueAt(selectedRow, 5)
            );
            showCustomerDialog(customer);
        }
    }
    
    private void deleteSelectedCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 1);
            String cognome = (String)tableModel.getValueAt(selectedRow, 2);
            String fullName = nome + " " + cognome;
            
            if (!showConfirmDialog(
                "Are you sure you want to delete customer '" + fullName + "'?\n" +
                "This action cannot be undone.", 
                "Confirm Deletion")) {
                return;
            }
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                
                // Check for existing dependencies
                boolean hasOrders = hasCustomerOrders(conn, id);
                boolean hasInvoices = hasCustomerInvoices(conn, id);
                
                if (hasOrders || hasInvoices) {
                    StringBuilder message = new StringBuilder();
                    message.append("Cannot delete customer '").append(fullName).append("' because they have:\n");
                    
                    if (hasOrders) message.append("- Existing orders\n");
                    if (hasInvoices) message.append("- Invoices\n");
                    
                    message.append("\nOptions:\n");
                    message.append("1. Delete/reassign related records first\n");
                    message.append("2. Use 'Force Delete' to remove all related data");
                    
                    String[] options = {"Cancel", "Force Delete (All Data)"};
                    int choice = JOptionPane.showOptionDialog(this,
                        message.toString(),
                        "Cannot Delete Customer",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, options, options[0]);
                    
                    if (choice == 1) { // Force Delete
                        performCascadeDelete(conn, id, fullName);
                    }
                    return;
                }
                
                // Safe to delete - no foreign key references
                String query = "DELETE FROM clienti WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    refreshData();
                    showSuccessMessage("Customer deleted successfully");
                }
            } catch (SQLException e) {
                showErrorMessage("Error deleting customer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void performCascadeDelete(Connection conn, int id, String fullName) {
        if (!showConfirmDialog(
            "WARNING: This will permanently delete customer '" + fullName + "' and ALL related data:\n" +
            "- All orders from this customer\n" +
            "- All invoices\n" +
            "- All order and invoice details\n\n" +
            "This action CANNOT be undone!\n\n" +
            "Are you absolutely sure?",
            "FORCE DELETE - Final Confirmation")) {
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            try {
                // Delete in order to respect foreign key constraints
                
                // 1. Delete order details for this customer's orders
                String deleteOrderDetails = """
                    DELETE FROM dettagli_ordine 
                    WHERE ordine_id IN (
                        SELECT id FROM ordini WHERE cliente_id = ?
                    )
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderDetails)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                
                // 2. Delete customer orders
                String deleteOrders = "DELETE FROM ordini WHERE cliente_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteOrders)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                
                // 3. Delete invoice details for this customer's invoices
                String deleteInvoiceDetails = """
                    DELETE FROM dettagli_fattura 
                    WHERE fattura_id IN (
                        SELECT id FROM fatture WHERE cliente_id = ?
                    )
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(deleteInvoiceDetails)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                
                // 4. Delete customer invoices
                String deleteInvoices = "DELETE FROM fatture WHERE cliente_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteInvoices)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                
                // 5. Finally delete the customer
                String deleteCustomer = "DELETE FROM clienti WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCustomer)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                refreshData();
                showSuccessMessage("Customer '" + fullName + "' and all related records deleted successfully");
                
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
    
    private boolean hasCustomerOrders(Connection conn, int customerId) throws SQLException {
        String query = "SELECT COUNT(*) FROM ordini WHERE cliente_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean hasCustomerInvoices(Connection conn, int customerId) throws SQLException {
        String query = "SELECT COUNT(*) FROM fatture WHERE cliente_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}