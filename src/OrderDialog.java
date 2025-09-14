import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OrderDialog extends JDialog {
    private Order order;
    private boolean orderSaved = false;
    private JComboBox<CustomerComboItem> clienteCombo;
    private JTextField dataField;
    private JComboBox<String> statoCombo;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JLabel totalLabel;
    private SimpleDateFormat dateFormat;
    private Map<Integer, Product> productsCache;
    private boolean updatingTotals = false; // Flag per prevenire la ricorsione
    
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
    
    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for order data
        JPanel orderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Customer
        gbc.gridx = 0; gbc.gridy = 0;
        orderPanel.add(new JLabel("Customer:"), gbc);
        
        gbc.gridx = 1;
        clienteCombo = new JComboBox<>();
        loadClienti();
        orderPanel.add(clienteCombo, gbc);
        
        // Date
        gbc.gridx = 0; gbc.gridy = 1;
        orderPanel.add(new JLabel("Date:"), gbc);
        
        gbc.gridx = 1;
        dataField = new JTextField(dateFormat.format(new Date()));
        orderPanel.add(dataField, gbc);
        
        // Status
        gbc.gridx = 0; gbc.gridy = 2;
        orderPanel.add(new JLabel("Status:"), gbc);
        
        gbc.gridx = 1;
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
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        // FIXED: Improved listener to prevent infinite recursion
        itemsTableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && 
                e.getColumn() == 2 && !updatingTotals) {
                SwingUtilities.invokeLater(this::updateTotals);
            }
        });
        
        // Panel for table buttons
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemButton = new JButton("Add Product");
        JButton removeItemButton = new JButton("Remove Product");
        
        addItemButton.addActionListener(e -> showAddProductDialog());
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
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        centerPanel.add(tableButtonPanel, BorderLayout.NORTH);
        centerPanel.add(totalPanel, BorderLayout.SOUTH);
        
        mainPanel.add(orderPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void loadClienti() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT id, nome, cognome FROM clienti ORDER BY cognome, nome";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    CustomerComboItem item = new CustomerComboItem(
                        rs.getInt("id"),
                        rs.getString("nome") + " " + rs.getString("cognome")
                    );
                    clienteCombo.addItem(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading customers: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
        // Select the customer
        for (int i = 0; i < clienteCombo.getItemCount(); i++) {
            CustomerComboItem item = clienteCombo.getItemAt(i);
            if (item.getId() == order.getClienteId()) {
                clienteCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Set the date
        dataField.setText(DateUtils.formatDate(order.getDataOrdine()));
        
        // Set the status
        statoCombo.setSelectedItem(order.getStato());
        
        // Load products
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
    
    private void showAddProductDialog() {
        // Dialog to select product and quantity
        JDialog dialog = new JDialog(this, "Add Product", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Product ComboBox
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Product:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ProductComboItem> productCombo = new JComboBox<>();
        for (Product product : productsCache.values()) {
            productCombo.addItem(new ProductComboItem(product));
        }
        panel.add(productCombo, gbc);
        
        // Quantity Spinner
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantity:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        panel.add(quantitySpinner, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        
        addButton.addActionListener(e -> {
            ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
            int quantity = (int)quantitySpinner.getValue();
            
            // Check if product is already in the order
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                int productId = (int)itemsTableModel.getValueAt(i, 0);
                if (productId == selectedProduct.getProduct().getId()) {
                    JOptionPane.showMessageDialog(dialog,
                        "This product is already in the order",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            // Add product to the table
            Vector<Object> row = new Vector<>();
            row.add(selectedProduct.getProduct().getId());
            row.add(selectedProduct.getProduct().getNome());
            row.add(quantity);
            row.add(String.format("%.2f", selectedProduct.getProduct().getPrezzo()));
            row.add(String.format("%.2f", quantity * selectedProduct.getProduct().getPrezzo()));
            itemsTableModel.addRow(row);
            
            updateTotals();
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void removeSelectedProduct() {
        int selectedRow = itemsTable.getSelectedRow();
        if (selectedRow != -1) {
            itemsTableModel.removeRow(selectedRow);
            updateTotals();
        }
    }
    
    // FIXED: Improved method to prevent infinite recursion
    private void updateTotals() {
        if (updatingTotals) return;
        
        updatingTotals = true;
        try {
            double total = 0;
            
            // First pass: calculate total without modifying the table
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                Object quantityObj = itemsTableModel.getValueAt(i, 2);
                Object priceObj = itemsTableModel.getValueAt(i, 3);
                
                int quantity = parseInteger(quantityObj);
                double price = parseDouble(priceObj);
                
                double itemTotal = quantity * price;
                total += itemTotal;
            }
            
            // Update the total label
            totalLabel.setText(String.format("Total: € %.2f", total));
            
            // Second pass: update row totals in a separate EDT event
            SwingUtilities.invokeLater(() -> {
                try {
                    updatingTotals = true;
                    for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                        Object quantityObj = itemsTableModel.getValueAt(i, 2);
                        Object priceObj = itemsTableModel.getValueAt(i, 3);
                        
                        int quantity = parseInteger(quantityObj);
                        double price = parseDouble(priceObj);
                        
                        double itemTotal = quantity * price;
                        
                        // Only update if the value has changed
                        String currentTotal = (String) itemsTableModel.getValueAt(i, 4);
                        String newTotal = String.format("%.2f", itemTotal);
                        if (!newTotal.equals(currentTotal)) {
                            itemsTableModel.setValueAt(newTotal, i, 4);
                        }
                    }
                } finally {
                    updatingTotals = false;
                }
            });
            
        } finally {
            updatingTotals = false;
        }
    }
    
    // Helper methods for safe parsing
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
            // Validation
            if (clienteCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                    "Select a customer",
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
                CustomerComboItem selectedCustomer = (CustomerComboItem)clienteCombo.getSelectedItem();
                Date orderDate = DateUtils.parseDate(dataField.getText(), dateFormat);
                String stato = (String)statoCombo.getSelectedItem();
                double totale = Double.parseDouble(totalLabel.getText().replace("Total: € ", "").replace(",", "."));
                
                if (order == null) { // New order
                    // Insert the order
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
                        
                        // Get the inserted order ID
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                orderId = rs.getInt(1);
                            } else { 
                                throw new SQLException("Failed to get order ID");
                            }
                        }
                    }
                    
                    // Insert order details
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
                    
                } else { // Edit existing order
                    // Update the order
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
                    
                    // Delete old details
                    String deleteDetailsQuery = "DELETE FROM dettagli_ordine WHERE ordine_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Insert new details
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