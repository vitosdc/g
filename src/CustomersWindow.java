import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CustomersWindow extends JDialog {
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private Timer searchTimer;
    private static final int SEARCH_DELAY = 300; // milliseconds
    
    public CustomersWindow(JFrame parent) {
        super(parent, "Customers Management", true);
        setupWindow();
        initComponents();
        loadCustomers();
    }
    
    private void setupWindow() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Search Panel with improved UX
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.PLAIN, 14f));
        
        searchField = new JTextField(25);
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN, 14f));
        searchField.setToolTipText("Search by name, surname, email, or phone number");
        
        // Real-time search with debouncing
        setupRealTimeSearch();
        
        // Clear search button
        JButton clearSearchButton = new JButton("✕");
        clearSearchButton.setFont(clearSearchButton.getFont().deriveFont(Font.PLAIN, 12f));
        clearSearchButton.setPreferredSize(new Dimension(30, 30));
        clearSearchButton.setToolTipText("Clear search");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadCustomers();
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);
        
        // Customers table with better formatting
        String[] columns = {"ID", "Name", "Surname", "Email", "Phone", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customersTable = new JTable(tableModel);
        customersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Hide ID column but keep it for reference
        customersTable.getColumnModel().getColumn(0).setMinWidth(0);
        customersTable.getColumnModel().getColumn(0).setMaxWidth(0);
        customersTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Set better column widths
        customersTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        customersTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        customersTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        customersTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        customersTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        
        // Double-click to edit
        customersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && customersTable.getSelectedRow() != -1) {
                    editSelectedCustomer();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(customersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        // Buttons panel with better layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        
        addButton = createStyledButton("➕ Add Customer", new Color(76, 175, 80));
        editButton = createStyledButton("✏️ Modify", new Color(33, 150, 243));
        deleteButton = createStyledButton("🗑️ Delete", new Color(244, 67, 54));
        refreshButton = createStyledButton("🔄 Refresh", new Color(158, 158, 158));
        
        addButton.addActionListener(e -> showCustomerDialog(null));
        editButton.addActionListener(e -> editSelectedCustomer());
        deleteButton.addActionListener(e -> deleteSelectedCustomer());
        refreshButton.addActionListener(e -> loadCustomers());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 10));
        
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
    
    private void setupRealTimeSearch() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSearch();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSearch();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSearch();
            }
            
            private void scheduleSearch() {
                if (searchTimer != null) {
                    searchTimer.stop();
                }
                searchTimer = new Timer(SEARCH_DELAY, e -> performSearch());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
        
        // Also search on Enter
        searchField.addActionListener(e -> performSearch());
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadCustomers();
        } else {
            searchCustomersImproved(searchTerm);
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = customersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        
        // Update button text based on selection
        if (isRowSelected) {
            int selectedRow = customersTable.getSelectedRow();
            String customerName = (String)tableModel.getValueAt(selectedRow, 1) + " " + 
                                 (String)tableModel.getValueAt(selectedRow, 2);
            editButton.setToolTipText("Edit " + customerName);
            deleteButton.setToolTipText("Delete " + customerName);
        } else {
            editButton.setToolTipText("Select a customer to edit");
            deleteButton.setToolTipText("Select a customer to delete");
        }
    }
    
    private void loadCustomers() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Loading customers...");
                
                tableModel.setRowCount(0);
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
                        }
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> 
                        NotificationManager.showDatabaseError("loading customers")
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
                    NotificationManager.showInfo("Loaded " + count + " customers");
                });
            }
        };
        
        worker.execute();
    }
    
    private void searchCustomersImproved(String searchTerm) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Searching customers...");
                
                // Use the improved search utility
                List<Customer> results = SmartSearchUtil.searchCustomersImproved(searchTerm);
                
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Customer customer : results) {
                        Vector<Object> row = new Vector<>();
                        row.add(customer.getId());
                        row.add(customer.getNome());
                        row.add(customer.getCognome());
                        row.add(customer.getEmail());
                        row.add(customer.getTelefono());
                        row.add(customer.getIndirizzo());
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
                        NotificationManager.showWarning("No customers found for: " + searchTerm);
                    } else {
                        NotificationManager.showInfo("Found " + count + " customers");
                    }
                });
            }
        };
        
        worker.execute();
    }
    
    private void showCustomerDialog(Customer customer) {
        CustomerDialog dialog = new CustomerDialog(this, customer);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            if (customer == null) {
                NotificationManager.showSaveSuccess("Customer");
            } else {
                NotificationManager.showSuccess("Customer updated successfully");
            }
            loadCustomers();
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
            
            // Enhanced confirmation dialog
            Object[] options = {"Yes, Delete", "Cancel"};
            int result = JOptionPane.showOptionDialog(this,
                "Are you sure you want to delete customer:\n\n" + fullName + "?\n\n" +
                "This action cannot be undone and may affect related orders and invoices.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
                
            if (result == 0) { // Yes, Delete
                deleteCustomerAsync(id, fullName);
            }
        }
    }
    
    private void deleteCustomerAsync(int id, String customerName) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Deleting customer...");
                
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    
                    // Check for related records first
                    String checkQuery = """
                        SELECT 
                            (SELECT COUNT(*) FROM ordini WHERE cliente_id = ?) as order_count,
                            (SELECT COUNT(*) FROM fatture WHERE cliente_id = ?) as invoice_count
                    """;
                    
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                        checkStmt.setInt(1, id);
                        checkStmt.setInt(2, id);
                        ResultSet rs = checkStmt.executeQuery();
                        
                        if (rs.next()) {
                            int orderCount = rs.getInt("order_count");
                            int invoiceCount = rs.getInt("invoice_count");
                            
                            if (orderCount > 0 || invoiceCount > 0) {
                                SwingUtilities.invokeLater(() -> {
                                    NotificationManager.showError(
                                        "Cannot delete customer: " + orderCount + " orders and " + 
                                        invoiceCount + " invoices are linked to this customer"
                                    );
                                });
                                return false;
                            }
                        }
                    }
                    
                    // Proceed with deletion
                    String deleteQuery = "DELETE FROM clienti WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                        pstmt.setInt(1, id);
                        int rowsAffected = pstmt.executeUpdate();
                        return rowsAffected > 0;
                    }
                    
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> 
                        NotificationManager.showDatabaseError("deleting customer")
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
                        NotificationManager.showDeleteSuccess("Customer " + customerName);
                        loadCustomers();
                    }
                } catch (Exception e) {
                    NotificationManager.showError("Error deleting customer: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
}