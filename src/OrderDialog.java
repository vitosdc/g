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
    
    public OrderDialog(JDialog parent, Order order) {
        super(parent, order == null ? "Nuovo Ordine" : "Modifica Ordine", true);
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
        // Panel principale con padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superiore per i dati dell'ordine
        JPanel orderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Cliente
        gbc.gridx = 0; gbc.gridy = 0;
        orderPanel.add(new JLabel("Cliente:"), gbc);
        
        gbc.gridx = 1;
        clienteCombo = new JComboBox<>();
        loadClienti();
        orderPanel.add(clienteCombo, gbc);
        
        // Data
        gbc.gridx = 0; gbc.gridy = 1;
        orderPanel.add(new JLabel("Data:"), gbc);
        
        gbc.gridx = 1;
        dataField = new JTextField(dateFormat.format(new Date()));
        orderPanel.add(dataField, gbc);
        
        // Stato
        gbc.gridx = 0; gbc.gridy = 2;
        orderPanel.add(new JLabel("Stato:"), gbc);
        
        gbc.gridx = 1;
        statoCombo = new JComboBox<>(new String[]{"Nuovo", "In Lavorazione", "Completato", "Annullato"});
        orderPanel.add(statoCombo, gbc);
        
        // Tabella prodotti
        String[] columns = {"ID", "Prodotto", "Quantità", "Prezzo Unit.", "Totale"};
        itemsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Solo la quantità è modificabile
            }
        };
        itemsTable = new JTable(itemsTableModel);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        // Aggiungi listener per l'aggiornamento della quantità
        itemsTableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                updateTotals();
            }
        });
        
        // Panel per i pulsanti della tabella
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemButton = new JButton("Aggiungi Prodotto");
        JButton removeItemButton = new JButton("Rimuovi Prodotto");
        
        addItemButton.addActionListener(e -> showAddProductDialog());
        removeItemButton.addActionListener(e -> removeSelectedProduct());
        
        tableButtonPanel.add(addItemButton);
        tableButtonPanel.add(removeItemButton);
        
        // Panel per il totale
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLabel = new JLabel("Totale: € 0.00");
        totalLabel.setFont(new Font(totalLabel.getFont().getName(), Font.BOLD, 14));
        totalPanel.add(totalLabel);
        
        // Panel per i pulsanti di salvataggio
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
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
                "Errore durante il caricamento dei clienti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
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
                "Errore durante il caricamento dei prodotti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadOrderData() {
        // Seleziona il cliente
        for (int i = 0; i < clienteCombo.getItemCount(); i++) {
            CustomerComboItem item = clienteCombo.getItemAt(i);
            if (item.getId() == order.getClienteId()) {
                clienteCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Imposta la data
        dataField.setText(dateFormat.format(order.getDataOrdine()));
        
        // Imposta lo stato
        statoCombo.setSelectedItem(order.getStato());
        
        // Carica i prodotti
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
        // Dialog per selezionare il prodotto e la quantità
        JDialog dialog = new JDialog(this, "Aggiungi Prodotto", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // ComboBox prodotti
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Prodotto:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ProductComboItem> productCombo = new JComboBox<>();
        for (Product product : productsCache.values()) {
            productCombo.addItem(new ProductComboItem(product));
        }
        panel.add(productCombo, gbc);
        
        // Spinner quantità
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantità:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        panel.add(quantitySpinner, gbc);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Aggiungi");
        JButton cancelButton = new JButton("Annulla");
        
        addButton.addActionListener(e -> {
            ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
            int quantity = (int)quantitySpinner.getValue();
            
            // Verifica se il prodotto è già presente
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                int productId = (int)itemsTableModel.getValueAt(i, 0);
                if (productId == selectedProduct.getProduct().getId()) {
                    JOptionPane.showMessageDialog(dialog,
                        "Questo prodotto è già presente nell'ordine",
                        "Avviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            // Aggiungi il prodotto alla tabella
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
    
    private void updateTotals() {
        double total = 0;
        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
            int quantity = (int)itemsTableModel.getValueAt(i, 2);
            double price = Double.parseDouble(((String)itemsTableModel.getValueAt(i, 3)).replace(",", "."));
            double itemTotal = quantity * price;
            itemsTableModel.setValueAt(String.format("%.2f", itemTotal), i, 4);
            total += itemTotal;
        }
        totalLabel.setText(String.format("Totale: € %.2f", total));
    }
    
    private void saveOrder() {
        try {
            // Validazione
            if (clienteCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                    "Seleziona un cliente",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (itemsTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "Aggiungi almeno un prodotto all'ordine",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            try {
                CustomerComboItem selectedCustomer = (CustomerComboItem)clienteCombo.getSelectedItem();
                Date orderDate = dateFormat.parse(dataField.getText());
                String stato = (String)statoCombo.getSelectedItem();
                double totale = Double.parseDouble(totalLabel.getText().replace("Totale: € ", ""));
                
                if (order == null) { // Nuovo ordine
                    // Inserisci l'ordine
                    String orderQuery = """
                        INSERT INTO ordini (cliente_id, data_ordine, stato, totale)
                        VALUES (?, ?, ?, ?)
                    """;
                    int orderId;
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, selectedCustomer.getId());
                        pstmt.setDate(2, new java.sql.Date(orderDate.getTime()));
                        pstmt.setString(3, stato);
                        pstmt.setDouble(4, totale);
                        pstmt.executeUpdate();
                        
                        // Ottieni l'ID dell'ordine inserito
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                orderId = rs.getInt(1);
                            } else { throw new SQLException("Impossibile ottenere l'ID dell'ordine");
                            }
                        }
                    }
                    
                    // Inserisci i dettagli dell'ordine
                    String detailQuery = """
                        INSERT INTO dettagli_ordine (ordine_id, prodotto_id, quantita, prezzo_unitario)
                        VALUES (?, ?, ?, ?)
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(detailQuery)) {
                        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                            pstmt.setInt(1, orderId);
                            pstmt.setInt(2, (int)itemsTableModel.getValueAt(i, 0));
                            pstmt.setInt(3, (int)itemsTableModel.getValueAt(i, 2));
                            pstmt.setDouble(4, Double.parseDouble(((String)itemsTableModel.getValueAt(i, 3)).replace(",", ".")));
                            pstmt.executeUpdate();
                        }
                    }
                    
                } else { // Modifica ordine esistente
                    // Aggiorna l'ordine
                    String orderQuery = """
                        UPDATE ordini
                        SET cliente_id = ?, data_ordine = ?, stato = ?, totale = ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery)) {
                        pstmt.setInt(1, selectedCustomer.getId());
                        pstmt.setDate(2, new java.sql.Date(orderDate.getTime()));
                        pstmt.setString(3, stato);
                        pstmt.setDouble(4, totale);
                        pstmt.setInt(5, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Elimina i vecchi dettagli
                    String deleteDetailsQuery = "DELETE FROM dettagli_ordine WHERE ordine_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Inserisci i nuovi dettagli
                    String detailQuery = """
                        INSERT INTO dettagli_ordine (ordine_id, prodotto_id, quantita, prezzo_unitario)
                        VALUES (?, ?, ?, ?)
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(detailQuery)) {
                        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                            pstmt.setInt(1, order.getId());
                            pstmt.setInt(2, (int)itemsTableModel.getValueAt(i, 0));
                            pstmt.setInt(3, (int)itemsTableModel.getValueAt(i, 2));
                            pstmt.setDouble(4, Double.parseDouble(((String)itemsTableModel.getValueAt(i, 3)).replace(",", ".")));
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
                "Errore durante il salvataggio dell'ordine: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isOrderSaved() {
        return orderSaved;
    }
}