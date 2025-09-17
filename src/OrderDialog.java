import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

public class OrderDialog extends JDialog {
    private Order order;
    private boolean orderSaved = false;
    private JButton selectCustomerButton;
    private Customer selectedCustomer;
    private JTextField dataField;
    private JComboBox<String> statoCombo;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JLabel totalLabel;
    private SimpleDateFormat dateFormat;
    private Map<Integer, Product> productsCache;
    private volatile boolean updatingTotals = false; // FIXED: Thread-safe flag
    private TableModelListener tableListener; // FIXED: Store listener reference
    
    public OrderDialog(JDialog parent, Order order) {
        super(parent, order == null ? "New Order" : "Edit Order", true);
        this.order = order;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.productsCache = new HashMap<>();
        
        setupWindow();
        initComponents();
        loadProducts();
        if (order != null) {
            loadOrderData();
        }
    }
    
    // FIXED: Clean up resources
    @Override
    public void dispose() {
        if (tableListener != null && itemsTableModel != null) {
            itemsTableModel.removeTableModelListener(tableListener);
        }
        super.dispose();
    }
    
    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for order data
        JPanel orderPanel = new JPanel(new GridBagLayout());
        orderPanel.setBorder(BorderFactory.createTitledBorder("Order Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Customer Selection - MODIFIED to use CustomerSelectionDialog
        gbc.gridx = 0; gbc.gridy = 0;
        orderPanel.add(new JLabel("* Customer:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        selectCustomerButton = new JButton("Click to select customer...");
        selectCustomerButton.setPreferredSize(new Dimension(300, 35));
        selectCustomerButton.setHorizontalAlignment(SwingConstants.LEFT);
        selectCustomerButton.addActionListener(e -> showCustomerSelectionDialog());
        orderPanel.add(selectCustomerButton, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        JButton newCustomerButton = new JButton("New Customer");
        newCustomerButton.addActionListener(e -> createNewCustomer());
        orderPanel.add(newCustomerButton, gbc);
        
        // Date
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        orderPanel.add(new JLabel("Date:"), gbc);
        
        gbc.gridx = 1;
        dataField = new JTextField(dateFormat.format(new Date()));
        orderPanel.add(dataField, gbc);
        
        // Status
        gbc.gridx = 2;
        orderPanel.add(new JLabel("Status:"), gbc);
        
        gbc.gridx = 3;
        statoCombo = new JComboBox<>(new String[]{"New", "In Progress", "Completed", "Cancelled"});
        orderPanel.add(statoCombo, gbc);
        
        // Products table
        String[] columns = {"ID", "Product", "Quantity", "Unit Price", "Total"};
        itemsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only quantity is editable
            }
        };
        itemsTable = new JTable(itemsTableModel);
        
        // Hide ID column
        itemsTable.getColumnModel().getColumn(0).setMinWidth(0);
        itemsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        // FIXED: Store listener reference and improve thread safety
        tableListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && 
                    e.getColumn() == 2 && !updatingTotals) {
                    synchronized (OrderDialog.this) {
                        if (!updatingTotals) {
                            SwingUtilities.invokeLater(() -> updateTotals());
                        }
                    }
                }
            }
        };
        itemsTableModel.addTableModelListener(tableListener);
        
        // Panel for table buttons
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemButton = new JButton("Add Product");
        JButton removeItemButton = new JButton("Remove Product");
        
        addItemButton.addActionListener(e -> showProductSelectionDialog());
        removeItemButton.addActionListener(e -> removeSelectedProduct());
        
        tableButtonPanel.add(addItemButton);
        tableButtonPanel.add(removeItemButton);
        
        // Panel for total
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLabel = new JLabel("Total: € 0.00");
        totalLabel.setFont(new Font(totalLabel.getFont().getName(), Font.BOLD, 14));
        totalPanel.add(totalLabel);
        
        // Panel for main buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveOrder());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Assembly
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tableButtonPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        centerPanel.add(totalPanel, BorderLayout.SOUTH);
        
        mainPanel.add(orderPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    // MODIFIED: New method to show CustomerSelectionDialog
    private void showCustomerSelectionDialog() {
        CustomerSelectionDialog dialog = new CustomerSelectionDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isCustomerSelected()) {
            selectedCustomer = dialog.getSelectedCustomer();
            updateCustomerButton();
        }
    }
    
    // MODIFIED: New method to update customer button text
    private void updateCustomerButton() {
        if (selectedCustomer != null) {
            String buttonText = String.format("%s %s", 
                selectedCustomer.getNome(), 
                selectedCustomer.getCognome());
            
            if (selectedCustomer.getEmail() != null && !selectedCustomer.getEmail().isEmpty()) {
                buttonText += " (" + selectedCustomer.getEmail() + ")";
            }
            
            selectCustomerButton.setText(buttonText);
            selectCustomerButton.setToolTipText("Customer: " + buttonText);
        }
    }
    
    // MODIFIED: New method to create new customer
    private void createNewCustomer() {
        CustomerDialog dialog = new CustomerDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            JOptionPane.showMessageDialog(this,
                "Customer created successfully. Please select it from the list.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void loadProducts() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM prodotti ORDER BY nome";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("codice"),
                        rs.getString("nome"),
                        rs.getString("descrizione"),
                        rs.getDouble("prezzo"),
                        rs.getInt("quantita")
                    );
                    productsCache.put(product.getId(), product);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadOrderData() {
        // Load customer data
        if (order.getClienteId() > 0) {
            try {
                selectedCustomer = loadCustomerById(order.getClienteId());
                if (selectedCustomer != null) {
                    updateCustomerButton();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Fallback: show customer name from order
                selectCustomerButton.setText(order.getClienteNome());
            }
        }
        
        dataField.setText(DateUtils.formatDate(order.getDataOrdine()));
        statoCombo.setSelectedItem(order.getStato());
        
        for (OrderItem item : order.getItems()) {
            Vector<Object> row = new Vector<>();
            row.add(item.getProdottoId());
            row.add(item.getProdottoNome());
            row.add(item.getQuantita());
            row.add(String.format("%.2f", item.getPrezzoUnitario()));
            row.add(String.format("%.2f", item.getTotale()));
            itemsTableModel.addRow(row);
        }
        
        updateTotals();
    }
    
    // MODIFIED: New method to load customer by ID
    private Customer loadCustomerById(int customerId) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = "SELECT * FROM clienti WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("indirizzo")
                    );
                }
            }
        }
        return null;
    }
    
    // MODIFIED: New method using ProductSelectionDialog
    private void showProductSelectionDialog() {
        ProductSelectionDialog dialog = new ProductSelectionDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isProductSelected()) {
            Product product = dialog.getSelectedProduct();
            int quantity = dialog.getSelectedQuantity();
            
            // Check if product already exists in the table
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                int existingId = (int) itemsTableModel.getValueAt(i, 0);
                if (existingId == product.getId()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "This product is already in the order.\n" +
                        "Do you want to increase the quantity instead?",
                        "Product Already Added",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        int currentQty = parseInteger(itemsTableModel.getValueAt(i, 2));
                        itemsTableModel.setValueAt(currentQty + quantity, i, 2);
                        updateTotals();
                    }
                    return;
                }
            }
            
            // Add new product to table
            Vector<Object> row = new Vector<>();
            row.add(product.getId());
            row.add(product.getNome());
            row.add(quantity);
            row.add(String.format("%.2f", product.getPrezzo()));
            row.add(String.format("%.2f", quantity * product.getPrezzo()));
            itemsTableModel.addRow(row);
            
            updateTotals();
        }
    }
    
    private void removeSelectedProduct() {
        int selectedRow = itemsTable.getSelectedRow();
        if (selectedRow != -1) {
            String productName = (String) itemsTableModel.getValueAt(selectedRow, 1);
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove " + productName + " from the order?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
                
            if (result == JOptionPane.YES_OPTION) {
                itemsTableModel.removeRow(selectedRow);
                updateTotals();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select a product to remove",
                "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    // FIXED: Improved thread-safe method
    private synchronized void updateTotals() {
        if (updatingTotals) return;
        
        updatingTotals = true;
        try {
            double total = 0;
            
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                Object quantityObj = itemsTableModel.getValueAt(i, 2);
                Object priceObj = itemsTableModel.getValueAt(i, 3);
                
                int quantity = parseInteger(quantityObj);
                double price = parseDouble(priceObj);
                
                double itemTotal = quantity * price;
                total += itemTotal;
                
                // Update row total
                String newTotal = String.format("%.2f", itemTotal);
                String currentTotal = (String) itemsTableModel.getValueAt(i, 4);
                if (!newTotal.equals(currentTotal)) {
                    itemsTableModel.setValueAt(newTotal, i, 4);
                }
            }
            
            totalLabel.setText(String.format("Total: € %.2f", total));
            
        } finally {
            updatingTotals = false;
        }
    }
    
    private int parseInteger(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double parseDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        }
        try {
            String str = obj.toString().replace(",", ".");
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void saveOrder() {
        try {
            if (selectedCustomer == null) {
                JOptionPane.showMessageDialog(this,
                    "Please select a customer",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (itemsTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "Add at least one product to the order",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            try {
                Date orderDate = DateUtils.parseDate(dataField.getText(), dateFormat);
                String stato = (String)statoCombo.getSelectedItem();
                double totale = Double.parseDouble(totalLabel.getText().replace("Total: € ", "").replace(",", "."));
                
                if (order == null) {
                    String orderQuery = """
                        INSERT INTO ordini (cliente_id, data_ordine, stato, totale)
                        VALUES (?, ?, ?, ?)
                    """;
                    int orderId;
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, selectedCustomer.getId());
                        pstmt.setTimestamp(2, DateUtils.toSqlTimestamp(orderDate));
                        pstmt.setString(3, stato);
                        pstmt.setDouble(4, totale);
                        pstmt.executeUpdate();
                        
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                orderId = rs.getInt(1);
                            } else { 
                                throw new SQLException("Failed to get order ID");
                            }
                        }
                    }
                    
                    String detailQuery = """
                        INSERT INTO dettagli_ordine (ordine_id, prodotto_id, quantita, prezzo_unitario)
                        VALUES (?, ?, ?, ?)
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(detailQuery)) {
                        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                            pstmt.setInt(1, orderId);
                            pstmt.setInt(2, (int)itemsTableModel.getValueAt(i, 0));
                            pstmt.setInt(3, parseInteger(itemsTableModel.getValueAt(i, 2)));
                            pstmt.setDouble(4, parseDouble(itemsTableModel.getValueAt(i, 3)));
                            pstmt.executeUpdate();
                        }
                    }
                    
                } else {
                    String orderQuery = """
                        UPDATE ordini
                        SET cliente_id = ?, data_ordine = ?, stato = ?, totale = ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery)) {
                        pstmt.setInt(1, selectedCustomer.getId());
                        pstmt.setTimestamp(2, DateUtils.toSqlTimestamp(orderDate));
                        pstmt.setString(3, stato);
                        pstmt.setDouble(4, totale);
                        pstmt.setInt(5, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    String deleteDetailsQuery = "DELETE FROM dettagli_ordine WHERE ordine_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    String detailQuery = """
                        INSERT INTO dettagli_ordine (ordine_id, prodotto_id, quantita, prezzo_unitario)
                        VALUES (?, ?, ?, ?)
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(detailQuery)) {
                        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                            pstmt.setInt(1, order.getId());
                            pstmt.setInt(2, (int)itemsTableModel.getValueAt(i, 0));
                            pstmt.setInt(3, parseInteger(itemsTableModel.getValueAt(i, 2)));
                            pstmt.setDouble(4, parseDouble(itemsTableModel.getValueAt(i, 3)));
                            pstmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                orderSaved = true;
                dispose();
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error saving the order: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isOrderSaved() {
        return orderSaved;
    }
}