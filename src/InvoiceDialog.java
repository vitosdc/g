import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date; // Explicitly specify java.util.Date

public class InvoiceDialog extends JDialog {
    private Invoice invoice;
    private boolean invoiceSaved = false;
    private JTextField numeroField;
    private JTextField dataField;
    private JComboBox<CustomerComboItem> clienteCombo;
    private JComboBox<String> statoCombo;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JLabel imponibileLabel;
    private JLabel ivaLabel;
    private JLabel totaleLabel;
    private SimpleDateFormat dateFormat;
    private Map<Integer, Product> productsCache;
    private volatile boolean isUpdatingTotals = false; // Thread-safe flag
    
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
        setSize(900, 700);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for invoice data
        JPanel invoicePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Invoice Number
        gbc.gridx = 0; gbc.gridy = 0;
        invoicePanel.add(new JLabel("Number:"), gbc);
        
        gbc.gridx = 1;
        numeroField = new JTextField(15);
        numeroField.setEditable(false); // The number is automatically generated
        invoicePanel.add(numeroField, gbc);
        
        // Date
        gbc.gridx = 2;
        invoicePanel.add(new JLabel("Date:"), gbc);
        
        gbc.gridx = 3;
        dataField = new JTextField(10);
        dataField.setText(dateFormat.format(new Date()));
        invoicePanel.add(dataField, gbc);
        
        // Customer
        gbc.gridx = 0; gbc.gridy = 1;
        invoicePanel.add(new JLabel("Customer:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        clienteCombo = new JComboBox<>();
        loadClienti();
        invoicePanel.add(clienteCombo, gbc);
        
        // Status
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        invoicePanel.add(new JLabel("Status:"), gbc);
        
        gbc.gridx = 1;
        statoCombo = new JComboBox<>(new String[]{"Draft", "Issued", "Paid", "Canceled"});
        invoicePanel.add(statoCombo, gbc);
        
        // Products table
        String[] columns = {"Code", "Product", "Quantity", "Unit Price", "VAT Rate", "Total"};
        itemsTableModel = new InvoiceTableModel(columns, 0);
        itemsTable = new JTable(itemsTableModel);
        
        // Configure table editors for type safety
        configureTableEditors();
        
        // Panel for table buttons
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemButton = new JButton("Add Product");
        JButton removeItemButton = new JButton("Remove Product");
        
        addItemButton.addActionListener(e -> showAddProductDialog());
        removeItemButton.addActionListener(e -> removeSelectedProduct());
        
        tableButtonPanel.add(addItemButton);
        tableButtonPanel.add(removeItemButton);
        
        // Panel for totals
        JPanel totalsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        imponibileLabel = new JLabel("Taxable Amount: € 0.00");
        ivaLabel = new JLabel("VAT: € 0.00");
        totaleLabel = new JLabel("Total: € 0.00");
        
        totalsPanel.add(imponibileLabel);
        totalsPanel.add(ivaLabel);
        totalsPanel.add(totaleLabel);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveInvoice());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Main layout
        mainPanel.add(invoicePanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tableButtonPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        centerPanel.add(totalsPanel, BorderLayout.SOUTH);
        
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
            String.class,  // Unit Price (formatted as string)
            String.class,  // VAT Rate (formatted as string)
            String.class   // Total (formatted as string)
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
                    
                    // Trigger totals update after successful value setting
                    SwingUtilities.invokeLater(() -> updateTotals());
                    
                } catch (NumberFormatException e) {
                    // Revert to previous value or set to 1
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
    
    // Configure table cell editors for better type safety
    private void configureTableEditors() {
        // Set up a custom editor for the quantity column to ensure Integer values
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
                    return false; // Don't stop editing
                }
            }
        });
    }
    
    // Safe type conversion methods
    private int parseInteger(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt(((String) obj).trim());
            } catch (NumberFormatException e) {
                return 0; // Default value for invalid input
            }
        }
        if (obj != null) {
            try {
                return Integer.parseInt(obj.toString().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private double parseDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof String) {
            try {
                String str = ((String) obj).replace(",", ".");
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException e) {
                return 0.0; // Default value for invalid input
            }
        }
        if (obj != null) {
            try {
                String str = obj.toString().replace(",", ".");
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
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
                "Error while loading customers: " + e.getMessage(),
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
                "Error while loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setupNewInvoice() {
        try {
            // Generate new invoice number
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            String numero = DatabaseManager.getInstance().getNextInvoiceNumber(year);
            numeroField.setText(numero);
            
            // Set today's date
            dataField.setText(dateFormat.format(cal.getTime()));
            
            // Set initial status
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
        
        // Select the customer
        for (int i = 0; i < clienteCombo.getItemCount(); i++) {
            CustomerComboItem item = clienteCombo.getItemAt(i);
            if (item.getId() == invoice.getClienteId()) {
                clienteCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Load products
        for (InvoiceItem item : invoice.getItems()) {
            Vector<Object> row = new Vector<>();
            row.add(item.getProdottoCodice());
            row.add(item.getProdottoNome());
            row.add(item.getQuantita()); // Integer value
            row.add(String.format("%.2f", item.getPrezzoUnitario()));
            row.add(String.format("%.2f", item.getAliquotaIva()));
            row.add(String.format("%.2f", item.getTotale()));
            itemsTableModel.addRow(row);
        }
        
        updateTotals();
    }
    
    private void showAddProductDialog() {
        JDialog dialog = new JDialog(this, "Add Product", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Pannello principale con padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Pannello del form con GridBagLayout migliorato
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Product
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Product:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JComboBox<ProductComboItem> productCombo = new JComboBox<>();
        productCombo.setPreferredSize(new Dimension(300, 25));
        for (Product product : productsCache.values()) {
            productCombo.addItem(new ProductComboItem(product));
        }
        formPanel.add(productCombo, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Quantity:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        quantitySpinner.setPreferredSize(new Dimension(100, 25));
        formPanel.add(quantitySpinner, gbc);
        
        // VAT Rate
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("VAT Rate %:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField ivaField = new JTextField("22.0");
        ivaField.setPreferredSize(new Dimension(100, 25));
        formPanel.add(ivaField, gbc);
        
        // Pannello dei pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        
        // Impostare dimensioni preferite per i pulsanti
        Dimension buttonSize = new Dimension(80, 30);
        addButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        
        addButton.addActionListener(e -> {
            try {
                ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
                if (selectedProduct == null) {
                    JOptionPane.showMessageDialog(dialog,
                        "Please select a product",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                int quantity = (int)quantitySpinner.getValue();
                double aliquotaIva = Double.parseDouble(ivaField.getText().replace(",", "."));
                
                // Check if the product is already in the list
                for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                    String codice = (String)itemsTableModel.getValueAt(i, 0);
                    if (codice.equals(selectedProduct.getProduct().getCodice())) {
                        JOptionPane.showMessageDialog(dialog,
                            "This product is already in the invoice",
                            "Warning", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                
                // Calculate total
                double prezzo = selectedProduct.getProduct().getPrezzo();
                double totale = quantity * prezzo;
                
                // Add to table
                Vector<Object> row = new Vector<>();
                row.add(selectedProduct.getProduct().getCodice());
                row.add(selectedProduct.getProduct().getNome());
                row.add(quantity); // Integer value
                row.add(String.format("%.2f", prezzo));
                row.add(String.format("%.2f", aliquotaIva));
                row.add(String.format("%.2f", totale));
                itemsTableModel.addRow(row);
                
                updateTotals();
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Please enter a valid value for the VAT rate",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        // Assemblaggio del pannello principale
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        
        // Impostazioni finestra
        dialog.pack(); // Adatta automaticamente alle dimensioni del contenuto
        dialog.setMinimumSize(new Dimension(400, 200)); // Dimensione minima
        dialog.setLocationRelativeTo(this); // Centra rispetto alla finestra padre
        dialog.setResizable(true); // Permette il ridimensionamento
        
        // Mostra la finestra
        dialog.setVisible(true);
    }
    
    private void removeSelectedProduct() {
        int selectedRow = itemsTable.getSelectedRow();
        if (selectedRow != -1) {
            itemsTableModel.removeRow(selectedRow);
            updateTotals();
        }
    }
    
    // Thread-safe updateTotals method with proper type handling
    private synchronized void updateTotals() {
        if (isUpdatingTotals) return;
        
        isUpdatingTotals = true;
        try {
            double imponibile = 0;
            double totaleIva = 0;
            
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                try {
                    // Safe parsing of table values
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
                    
                    // Update the total column for this row
                    String currentTotal = (String) itemsTableModel.getValueAt(i, 5);
                    String newTotal = String.format("%.2f", subtotal);
                    if (!newTotal.equals(currentTotal)) {
                        itemsTableModel.setValueAt(newTotal, i, 5);
                    }
                    
                } catch (Exception e) {
                    // Log the error but don't crash the application
                    System.err.println("Error processing row " + i + ": " + e.getMessage());
                    // Skip this row and continue
                    continue;
                }
            }
            
            double totale = imponibile + totaleIva;
            
            // Update the labels
            imponibileLabel.setText(String.format("Taxable Amount: € %.2f", imponibile));
            ivaLabel.setText(String.format("VAT: € %.2f", totaleIva));
            totaleLabel.setText(String.format("Total: € %.2f", totale));
            
        } finally {
            isUpdatingTotals = false;
        }
    }
    
    private void saveInvoice() {
        try {
            // Validation
            if (clienteCombo.getSelectedItem() == null) {
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
            
            // Date parsing
            Date dataFattura;
            try {
                dataFattura = dateFormat.parse(dataField.getText());
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid date format. Use dd/MM/yyyy",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Retrieve values
            CustomerComboItem selectedCustomer = (CustomerComboItem)clienteCombo.getSelectedItem();
            String stato = (String)statoCombo.getSelectedItem();
            String numero = numeroField.getText();
            
            // Calculate totals from labels
            double imponibile = Double.parseDouble(imponibileLabel.getText()
                .replace("Taxable Amount: € ", "").replace(",", "."));
            double iva = Double.parseDouble(ivaLabel.getText()
                .replace("VAT: € ", "").replace(",", "."));
            double totale = Double.parseDouble(totaleLabel.getText()
                .replace("Total: € ", "").replace(",", "."));
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            try {
                if (invoice == null) { // New invoice
                    // Insert the invoice
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
                    
                    // Insert invoice details
                    insertInvoiceDetails(conn, invoiceId);
                    
                } else { // Modify existing invoice
                    // Update the invoice
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
                    
                    // Delete old details
                    String deleteDetailsQuery = "DELETE FROM dettagli_fattura WHERE fattura_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, invoice.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Insert new details
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
