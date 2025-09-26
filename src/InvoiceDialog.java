import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class InvoiceDialog extends JDialog {
    private Invoice invoice;
    private boolean invoiceSaved = false;
    private JTextField numeroField;
    private JTextField dataField;
    private JButton selectCustomerButton;
    private Customer selectedCustomer;
    private JComboBox<String> statoCombo;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JLabel imponibileLabel;
    private JLabel ivaLabel;
    private JLabel totaleLabel;
    private SimpleDateFormat dateFormat;
    private Map<Integer, Product> productsCache;
    private volatile boolean isUpdatingTotals = false;
    
    // Constructor for JFrame parent
    public InvoiceDialog(JFrame parent, Invoice invoice) {
        super(parent, invoice == null ? "New Invoice" : "Modify Invoice", true);
        this.invoice = invoice;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.productsCache = new HashMap<>();
        
        setupWindow();
        initComponents();
        loadProducts();
        if (invoice != null) {
            loadInvoiceData();
        } else {
            setupNewInvoice();
        }
    }
    
    // Constructor for JDialog parent
    public InvoiceDialog(JDialog parent, Invoice invoice) {
        super(parent, invoice == null ? "New Invoice" : "Modify Invoice", true);
        this.invoice = invoice;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.productsCache = new HashMap<>();
        
        setupWindow();
        initComponents();
        loadProducts();
        if (invoice != null) {
            loadInvoiceData();
        } else {
            setupNewInvoice();
        }
    }
    
    private void setupWindow() {
        setSize(1000, 750);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for invoice data
        JPanel invoicePanel = new JPanel(new GridBagLayout());
        invoicePanel.setBorder(BorderFactory.createTitledBorder("Invoice Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Invoice Number
        gbc.gridx = 0; gbc.gridy = 0;
        invoicePanel.add(new JLabel("Number:"), gbc);
        
        gbc.gridx = 1;
        numeroField = new JTextField(15);
        numeroField.setEditable(false);
        numeroField.setBackground(Color.LIGHT_GRAY);
        invoicePanel.add(numeroField, gbc);
        
        // Date
        gbc.gridx = 2;
        invoicePanel.add(new JLabel("Date:"), gbc);
        
        gbc.gridx = 3;
        dataField = new JTextField(10);
        dataField.setText(dateFormat.format(new Date()));
        invoicePanel.add(dataField, gbc);
        
        // Customer Selection - MIGLIORATO
        gbc.gridx = 0; gbc.gridy = 1;
        invoicePanel.add(new JLabel("* Customer:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        selectCustomerButton = new JButton("Click to select customer...");
        selectCustomerButton.setPreferredSize(new Dimension(300, 35));
        selectCustomerButton.setHorizontalAlignment(SwingConstants.LEFT);
        selectCustomerButton.addActionListener(e -> showCustomerSelectionDialog());
        invoicePanel.add(selectCustomerButton, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        JButton newCustomerButton = new JButton("New Customer");
        newCustomerButton.addActionListener(e -> createNewCustomer());
        invoicePanel.add(newCustomerButton, gbc);
        
        // Status
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        invoicePanel.add(new JLabel("Status:"), gbc);
        
        gbc.gridx = 1;
        statoCombo = new JComboBox<>(new String[]{"Draft", "Issued", "Paid", "Canceled"});
        invoicePanel.add(statoCombo, gbc);
        
        // Products table
        String[] columns = {"Code", "Product", "Quantity", "Unit Price €", "VAT Rate %", "Total €"};
        itemsTableModel = new InvoiceTableModel(columns, 0);
        itemsTable = new JTable(itemsTableModel);
        
        // Configure table
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Code
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Product
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Quantity
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Unit Price
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // VAT Rate
        itemsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Total
        
        configureTableEditors();
        
        // Panel for table buttons
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableButtonPanel.setBorder(BorderFactory.createTitledBorder("Products"));
        
        JButton addItemButton = new JButton("Add Product");
        addItemButton.setPreferredSize(new Dimension(120, 30));
        addItemButton.addActionListener(e -> showProductSelectionDialog());
        
        JButton removeItemButton = new JButton("Remove");
        removeItemButton.setPreferredSize(new Dimension(100, 30));
        removeItemButton.addActionListener(e -> removeSelectedProduct());
        
        JButton editItemButton = new JButton("Edit Quantity");
        editItemButton.setPreferredSize(new Dimension(120, 30));
        editItemButton.addActionListener(e -> editSelectedProduct());
        
        tableButtonPanel.add(addItemButton);
        tableButtonPanel.add(removeItemButton);
        tableButtonPanel.add(editItemButton);
        
        // Panel for totals
        JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout(new BoxLayout(totalsPanel, BoxLayout.Y_AXIS));
        totalsPanel.setBorder(BorderFactory.createTitledBorder("Totals"));
        
        imponibileLabel = new JLabel("Taxable Amount: € 0.00");
        ivaLabel = new JLabel("VAT: € 0.00");
        totaleLabel = new JLabel("TOTAL: € 0.00");
        
        // Stile per il totale
        totaleLabel.setFont(totaleLabel.getFont().deriveFont(Font.BOLD, 16f));
        
        JPanel totalsPanelInner = new JPanel(new GridLayout(3, 1, 5, 5));
        totalsPanelInner.add(imponibileLabel);
        totalsPanelInner.add(ivaLabel);
        totalsPanelInner.add(totaleLabel);
        totalsPanel.add(totalsPanelInner);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save Invoice");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.setPreferredSize(new Dimension(120, 35));
        cancelButton.setPreferredSize(new Dimension(100, 35));
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        
        saveButton.addActionListener(e -> saveInvoice());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout assembly
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(tableButtonPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(totalsPanel, BorderLayout.EAST);
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        mainPanel.add(invoicePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    // Custom table model for type safety
    private class InvoiceTableModel extends DefaultTableModel {
        private final Class<?>[] columnTypes = {
            String.class,  // Code
            String.class,  // Product
            Integer.class, // Quantity
            String.class,  // Unit Price
            String.class,  // VAT Rate
            String.class   // Total
        };
        
        public InvoiceTableModel(String[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex < columnTypes.length) {
                return columnTypes[columnIndex];
            }
            return Object.class;
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2; // Only quantity column is editable
        }
        
        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) { // Quantity column
                try {
                    int intValue;
                    if (value instanceof String) {
                        intValue = Integer.parseInt(((String) value).trim());
                    } else if (value instanceof Integer) {
                        intValue = (Integer) value;
                    } else {
                        intValue = Integer.parseInt(value.toString().trim());
                    }
                    
                    if (intValue < 0) {
                        throw new NumberFormatException("Negative quantity not allowed");
                    }
                    
                    super.setValueAt(intValue, row, col);
                    SwingUtilities.invokeLater(() -> updateTotals());
                    
                } catch (NumberFormatException e) {
                    super.setValueAt(1, row, col);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(InvoiceDialog.this,
                            "Invalid quantity. Please enter a positive integer.",
                            "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    });
                }
            } else {
                super.setValueAt(value, row, col);
            }
        }
    }
    
    private void configureTableEditors() {
        itemsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = (String) getCellEditorValue();
                    int intValue = Integer.parseInt(value.trim());
                    if (intValue < 0) {
                        throw new NumberFormatException("Quantity cannot be negative");
                    }
                    return super.stopCellEditing();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(InvoiceDialog.this,
                        "Please enter a valid positive integer for quantity",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
    }
    
    private void showCustomerSelectionDialog() {
        CustomerSelectionDialog dialog = new CustomerSelectionDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isCustomerSelected()) {
            selectedCustomer = dialog.getSelectedCustomer();
            updateCustomerButton();
        }
    }
    
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
    
    private void createNewCustomer() {
        CustomerDialog dialog = new CustomerDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            // Optionally reload customer and auto-select the new one
            JOptionPane.showMessageDialog(this,
                "Customer created successfully. Please select it from the list.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showProductSelectionDialog() {
        ProductSelectionDialog dialog = new ProductSelectionDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isProductSelected()) {
            Product product = dialog.getSelectedProduct();
            int quantity = dialog.getSelectedQuantity();
            double vatRate = dialog.getSelectedVatRate();
            
            // Check if product already exists in the table
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                String existingCode = (String) itemsTableModel.getValueAt(i, 0);
                if (existingCode.equals(product.getCodice())) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "This product is already in the invoice.\n" +
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
            row.add(product.getCodice());
            row.add(product.getNome());
            row.add(quantity);
            row.add(String.format("%.2f", product.getPrezzo()));
            row.add(String.format("%.1f", vatRate));
            row.add(String.format("%.2f", quantity * product.getPrezzo()));
            itemsTableModel.addRow(row);
            
            updateTotals();
        }
    }
    
    private void editSelectedProduct() {
        int selectedRow = itemsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product to edit",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String productName = (String) itemsTableModel.getValueAt(selectedRow, 1);
        int currentQuantity = parseInteger(itemsTableModel.getValueAt(selectedRow, 2));
        
        String input = JOptionPane.showInputDialog(this,
            "Enter new quantity for " + productName + ":",
            "Edit Quantity",
            JOptionPane.QUESTION_MESSAGE);
            
        if (input != null && !input.trim().isEmpty()) {
            try {
                int newQuantity = Integer.parseInt(input.trim());
                if (newQuantity <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Quantity must be greater than zero",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                itemsTableModel.setValueAt(newQuantity, selectedRow, 2);
                updateTotals();
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid number",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void removeSelectedProduct() {
        int selectedRow = itemsTable.getSelectedRow();
        if (selectedRow != -1) {
            String productName = (String) itemsTableModel.getValueAt(selectedRow, 1);
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove " + productName + " from the invoice?",
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
                "Error while loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setupNewInvoice() {
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            String numero = DatabaseManager.getInstance().getNextInvoiceNumber(year);
            numeroField.setText(numero);
            
            dataField.setText(dateFormat.format(cal.getTime()));
            statoCombo.setSelectedItem("Draft");
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while generating invoice number: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadInvoiceData() {
        numeroField.setText(invoice.getNumero());
        dataField.setText(dateFormat.format(invoice.getData()));
        statoCombo.setSelectedItem(invoice.getStato());
        
        // Load customer
        selectedCustomer = new Customer(
            invoice.getClienteId(),
            "", "", "", "", "" // We'd need to load full customer data
        );
        // For now, just show the customer name from invoice
        selectCustomerButton.setText(invoice.getClienteNome());
        
        // Load products
        for (InvoiceItem item : invoice.getItems()) {
            Vector<Object> row = new Vector<>();
            row.add(item.getProdottoCodice());
            row.add(item.getProdottoNome());
            row.add(item.getQuantita());
            row.add(String.format("%.2f", item.getPrezzoUnitario()));
            row.add(String.format("%.2f", item.getAliquotaIva()));
            row.add(String.format("%.2f", item.getTotale()));
            itemsTableModel.addRow(row);
        }
        
        updateTotals();
    }
    
    private synchronized void updateTotals() {
        if (isUpdatingTotals) return;
        
        isUpdatingTotals = true;
        try {
            double imponibile = 0;
            double totaleIva = 0;
            
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                try {
                    Object quantityObj = itemsTableModel.getValueAt(i, 2);
                    Object prezzoObj = itemsTableModel.getValueAt(i, 3);
                    Object aliquotaObj = itemsTableModel.getValueAt(i, 4);
                    
                    int quantity = parseInteger(quantityObj);
                    double price = parseDouble(prezzoObj);
                    double aliquotaIva = parseDouble(aliquotaObj);
                    
                    double subtotal = quantity * price;
                    double iva = subtotal * (aliquotaIva / 100);
                    
                    imponibile += subtotal;
                    totaleIva += iva;
                    
                    String currentTotal = (String) itemsTableModel.getValueAt(i, 5);
                    String newTotal = String.format("%.2f", subtotal);
                    if (!newTotal.equals(currentTotal)) {
                        itemsTableModel.setValueAt(newTotal, i, 5);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error processing row " + i + ": " + e.getMessage());
                    continue;
                }
            }
            
            double totale = imponibile + totaleIva;
            
            imponibileLabel.setText(String.format("Taxable Amount: € %.2f", imponibile));
            ivaLabel.setText(String.format("VAT: € %.2f", totaleIva));
            totaleLabel.setText(String.format("TOTAL: € %.2f", totale));
            
        } finally {
            isUpdatingTotals = false;
        }
    }
    
    private int parseInteger(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        try {
            return Integer.parseInt(obj.toString().trim());
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
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void saveInvoice() {
        try {
            // Validation
            if (selectedCustomer == null) {
                JOptionPane.showMessageDialog(this,
                    "Please select a customer",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (itemsTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "Please add at least one product to the invoice",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Date dataFattura;
            try {
                dataFattura = dateFormat.parse(dataField.getText());
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid date format. Use dd/MM/yyyy",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String stato = (String)statoCombo.getSelectedItem();
            String numero = numeroField.getText();
            
            double imponibile = Double.parseDouble(imponibileLabel.getText()
                .replace("Taxable Amount: € ", "").replace(",", "."));
            double iva = Double.parseDouble(ivaLabel.getText()
                .replace("VAT: € ", "").replace(",", "."));
            double totale = Double.parseDouble(totaleLabel.getText()
                .replace("TOTAL: € ", "").replace(",", "."));
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            try {
                if (invoice == null) {
                    String invoiceQuery = """
                        INSERT INTO fatture (numero, data, cliente_id, imponibile, iva, totale, stato)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
                    int invoiceId;
                    try (PreparedStatement pstmt = conn.prepareStatement(invoiceQuery, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setString(1, numero);
                        pstmt.setDate(2, new java.sql.Date(dataFattura.getTime()));
                        pstmt.setInt(3, selectedCustomer.getId());
                        pstmt.setDouble(4, imponibile);
                        pstmt.setDouble(5, iva);
                        pstmt.setDouble(6, totale);
                        pstmt.setString(7, stato);
                        pstmt.executeUpdate();
                        
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                invoiceId = rs.getInt(1);
                            } else {
                                throw new SQLException("Failed to get invoice ID");
                            }
                        }
                    }
                    
                    insertInvoiceDetails(conn, invoiceId);
                    
                } else {
                    String invoiceQuery = """
                        UPDATE fatture 
                        SET data = ?, cliente_id = ?, imponibile = ?, iva = ?, totale = ?, stato = ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(invoiceQuery)) {
                        pstmt.setDate(1, new java.sql.Date(dataFattura.getTime()));
                        pstmt.setInt(2, selectedCustomer.getId());
                        pstmt.setDouble(3, imponibile);
                        pstmt.setDouble(4, iva);
                        pstmt.setDouble(5, totale);
                        pstmt.setString(6, stato);
                        pstmt.setInt(7, invoice.getId());
                        pstmt.executeUpdate();
                    }
                    
                    String deleteDetailsQuery = "DELETE FROM dettagli_fattura WHERE fattura_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, invoice.getId());
                        pstmt.executeUpdate();
                    }
                    
                    insertInvoiceDetails(conn, invoice.getId());
                }
                
                conn.commit();
                invoiceSaved = true;
                dispose();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while saving the invoice: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void insertInvoiceDetails(Connection conn, int invoiceId) throws SQLException {
        String detailQuery = """
            INSERT INTO dettagli_fattura 
            (fattura_id, prodotto_id, quantita, prezzo_unitario, aliquota_iva, totale)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(detailQuery)) {
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                String codice = (String)itemsTableModel.getValueAt(i, 0);
                int prodottoId = -1;
                for (Product p : productsCache.values()) {
                    if (p.getCodice().equals(codice)) {
                        prodottoId = p.getId();
                        break;
                    }
                }
                if (prodottoId == -1) continue;
                
                int quantita = parseInteger(itemsTableModel.getValueAt(i, 2));
                double prezzoUnitario = parseDouble(itemsTableModel.getValueAt(i, 3));
                double aliquotaIva = parseDouble(itemsTableModel.getValueAt(i, 4));
                double totaleProdotto = quantita * prezzoUnitario;
                
                pstmt.setInt(1, invoiceId);
                pstmt.setInt(2, prodottoId);
                pstmt.setInt(3, quantita);
                pstmt.setDouble(4, prezzoUnitario);
                pstmt.setDouble(5, aliquotaIva);
                pstmt.setDouble(6, totaleProdotto);
                pstmt.executeUpdate();
            }
        }
    }
    
    public boolean isInvoiceSaved() {
        return invoiceSaved;
    }
}