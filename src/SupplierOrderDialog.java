
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

public class SupplierOrderDialog extends JDialog {
    private int supplierId;
    private String supplierName;
    private SupplierOrder order;
    private boolean orderSaved = false;
    
    private JTextField numeroField;
    private JTextField dataField;
    private JTextField dataConsegnaField;
    private JComboBox<String> statoCombo;
    private JTextArea noteArea;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JLabel totalLabel;
    private SimpleDateFormat dateFormat;
    private Map<Integer, Product> productsCache;
    
    public SupplierOrderDialog(JDialog parent, int supplierId, String supplierName, SupplierOrder order) {
        super(parent, order == null ? "Nuovo Ordine Fornitore" : "Modifica Ordine Fornitore", true);
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.order = order;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.productsCache = new HashMap<>();
        
        setupWindow();
        initComponents();
        loadProducts();
        if (order != null) {
            loadOrderData();
        } else {
            generateOrderNumber();
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
        
        // Numero ordine
        gbc.gridx = 0; gbc.gridy = 0;
        orderPanel.add(new JLabel("Numero:"), gbc);
        
        gbc.gridx = 1;
        numeroField = new JTextField(15);
        numeroField.setEditable(false);
        orderPanel.add(numeroField, gbc);
        
        // Fornitore
        gbc.gridx = 2;
        orderPanel.add(new JLabel("Fornitore:"), gbc);
        
        gbc.gridx = 3;
        JTextField supplierField = new JTextField(supplierName);
        supplierField.setEditable(false);
        orderPanel.add(supplierField, gbc);
        
        // Data ordine
        gbc.gridx = 0; gbc.gridy = 1;
        orderPanel.add(new JLabel("Data Ordine:"), gbc);
        
        gbc.gridx = 1;
        dataField = new JTextField(10);
        dataField.setText(dateFormat.format(new Date()));
        orderPanel.add(dataField, gbc);
        
        // Data consegna
        gbc.gridx = 2;
        orderPanel.add(new JLabel("Data Consegna:"), gbc);
        
        gbc.gridx = 3;
        dataConsegnaField = new JTextField(10);
        orderPanel.add(dataConsegnaField, gbc);
        
        // Stato
        gbc.gridx = 0; gbc.gridy = 2;
        orderPanel.add(new JLabel("Stato:"), gbc);
        
        gbc.gridx = 1;
        statoCombo = new JComboBox<>(new String[]{"Bozza", "Confermato", "In Consegna", "Completato", "Annullato"});
        orderPanel.add(statoCombo, gbc);
        
        // Note
        gbc.gridx = 0; gbc.gridy = 3;
        orderPanel.add(new JLabel("Note:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        noteArea = new JTextArea(3, 40);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        orderPanel.add(new JScrollPane(noteArea), gbc);
        
        // Tabella prodotti
        String[] columns = {"Codice", "Prodotto", "Quantità", "Prezzo Unit.", "Totale", "Note"};
        itemsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 5; // Solo quantità e note modificabili
            }
        };
        itemsTable = new JTable(itemsTableModel);
        
        // Aggiorna totali quando cambia la quantità
        itemsTableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 2) {
                updateTotals();
            }
        });
        
        // Panel pulsanti tabella
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemButton = new JButton("Aggiungi Prodotto");
        JButton removeItemButton = new JButton("Rimuovi Prodotto");
        
        addItemButton.addActionListener(e -> showAddProductDialog());
        removeItemButton.addActionListener(e -> removeSelectedProduct());
        
        tableButtonPanel.add(addItemButton);
        tableButtonPanel.add(removeItemButton);
        
        // Panel totale
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLabel = new JLabel("Totale: € 0,00");
        totalLabel.setFont(new Font(totalLabel.getFont().getName(), Font.BOLD, 14));
        totalPanel.add(totalLabel);
        
        // Panel pulsanti principali
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
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
    
    private void generateOrderNumber() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT COALESCE(MAX(CAST(SUBSTR(numero, 5) AS INTEGER)), 0) + 1 as next_num
                FROM ordini_fornitori
                WHERE numero LIKE ?
            """;
            
            Calendar cal = Calendar.getInstance();
            String yearPrefix = String.format("OF%d", cal.get(Calendar.YEAR));
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, yearPrefix + "%");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int nextNum = rs.getInt("next_num");
                    numeroField.setText(String.format("%s%04d", yearPrefix, nextNum));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la generazione del numero ordine: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadProducts() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, COALESCE(l.prezzo, p.prezzo) as prezzo_fornitore,
                       l.codice_prodotto_fornitore
                FROM prodotti p
                LEFT JOIN listini_fornitori l ON p.id = l.prodotto_id 
                    AND l.fornitore_id = ?
                    AND (l.data_validita_fine IS NULL OR l.data_validita_fine >= CURRENT_DATE)
                ORDER BY p.nome
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, supplierId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("codice"),
                        rs.getString("nome"),
                        rs.getString("descrizione"),
                        rs.getDouble("prezzo_fornitore"),
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
        numeroField.setText(order.getNumero());
        dataField.setText(dateFormat.format(order.getDataOrdine()));
        if (order.getDataConsegnaPrevista() != null) {
            dataConsegnaField.setText(dateFormat.format(order.getDataConsegnaPrevista()));
        }
        statoCombo.setSelectedItem(order.getStato());
        noteArea.setText(order.getNote());
        
        // Carica prodotti
        for (SupplierOrderItem item : order.getItems()) {
            Vector<Object> row = new Vector<>();
            row.add(item.getProdottoCodice());
            row.add(item.getProdottoNome());
            row.add(item.getQuantita());
            row.add(String.format("%.2f", item.getPrezzoUnitario()));
            row.add(String.format("%.2f", item.getTotale()));
            row.add(item.getNote());
            itemsTableModel.addRow(row);
        }
        
        updateTotals();
    }
    
    private void showAddProductDialog() {
        JDialog dialog = new JDialog(this, "Aggiungi Prodotto", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // ComboBox prodotti
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Prodotto:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ProductDisplay> productCombo = new JComboBox<>();
        for (Product product : productsCache.values()) {
            productCombo.addItem(new ProductDisplay(product));
        }
        panel.add(productCombo, gbc);
        
        // Quantità
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantità:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 9999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        panel.add(quantitySpinner, gbc);
        
        // Note
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Note:"), gbc);
        
        gbc.gridx = 1;
        JTextField noteField = new JTextField(30);
        panel.add(noteField, gbc);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Aggiungi");
        JButton cancelButton = new JButton("Annulla");
        
        addButton.addActionListener(e -> {
            ProductDisplay selectedProduct = (ProductDisplay)productCombo.getSelectedItem();
            int quantity = (int)quantitySpinner.getValue();
            String note = noteField.getText().trim();
            
            // Verifica se il prodotto è già presente
            for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                String codice = (String)itemsTableModel.getValueAt(i, 0);
                if (codice.equals(selectedProduct.getProduct().getCodice())) {
                    JOptionPane.showMessageDialog(dialog,
                        "Questo prodotto è già presente nell'ordine",
                        "Avviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            // Aggiungi alla tabella
            Vector<Object> row = new Vector<>();
            row.add(selectedProduct.getProduct().getCodice());
            row.add(selectedProduct.getProduct().getNome());
            row.add(quantity);
            row.add(String.format("%.2f", selectedProduct.getProduct().getPrezzo()));
            row.add(String.format("%.2f", quantity * selectedProduct.getProduct().getPrezzo()));
            row.add(note);
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
    
    private static class ProductDisplay {
        private Product product;
        
        public ProductDisplay(Product product) {
            this.product = product;
        }
        
        public Product getProduct() { return product; }
        
        @Override
        public String toString() {
            return String.format("%s - %s (€ %.2f)", 
                product.getCodice(), product.getNome(), product.getPrezzo());
        }
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
            if (itemsTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "Aggiungi almeno un prodotto all'ordine",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parsing date
            Date dataOrdine;
            Date dataConsegna = null;
            try {
                dataOrdine = dateFormat.parse(dataField.getText());
                String dataConsegnaText = dataConsegnaField.getText().trim();
                if (!dataConsegnaText.isEmpty()) {
                    dataConsegna = dateFormat.parse(dataConsegnaText);
                }
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                    "Formato data non valido. Usa il formato dd/MM/yyyy",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Calcola totale
            double totale = Double.parseDouble(
                totalLabel.getText().replace("Totale: € ", "").replace(",", "."));
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            try {
                if (order == null) {
                    // Inserisci nuovo ordine
                    String orderQuery = """
                        INSERT INTO ordini_fornitori (
                            fornitore_id, numero, data_ordine, data_consegna_prevista,
                            stato, totale, note
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
                    
                    int orderId;
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, supplierId);
                        pstmt.setString(2, numeroField.getText());
                        pstmt.setDate(3, new java.sql.Date(dataOrdine.getTime()));
                        pstmt.setDate(4, dataConsegna != null ? new java.sql.Date(dataConsegna.getTime()) : null);
                        pstmt.setString(5, (String)statoCombo.getSelectedItem());
                        pstmt.setDouble(6, totale);
                        pstmt.setString(7, noteArea.getText().trim());
                        pstmt.executeUpdate();
                        
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                orderId = rs.getInt(1);
                            } else {
                                throw new SQLException("Impossibile ottenere l'ID dell'ordine");
                            }
                        }
                    }
                    
                    // Inserisci dettagli ordine
                    insertOrderDetails(conn, orderId);
                    
                } else {
                    // Aggiorna ordine esistente
                    String orderQuery = """
                        UPDATE ordini_fornitori SET
                            data_ordine = ?, data_consegna_prevista = ?,
                            stato = ?, totale = ?, note = ?
                        WHERE id = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(orderQuery)) {
                        pstmt.setDate(1, new java.sql.Date(dataOrdine.getTime()));
                        pstmt.setDate(2, dataConsegna != null ? new java.sql.Date(dataConsegna.getTime()) : null);
                        pstmt.setString(3, (String)statoCombo.getSelectedItem());
                        pstmt.setDouble(4, totale);
                        pstmt.setString(5, noteArea.getText().trim());
                        pstmt.setInt(6, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Elimina vecchi dettagli
                    String deleteDetailsQuery = "DELETE FROM dettagli_ordini_fornitori WHERE ordine_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, order.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Inserisci nuovi dettagli
                    insertOrderDetails(conn, order.getId());
                }
                
                conn.commit();
                orderSaved = true;
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
                "Errore durante il salvataggio dell'ordine: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void insertOrderDetails(Connection conn, int orderId) throws SQLException {
        String detailQuery = """
            INSERT INTO dettagli_ordini_fornitori (
                ordine_id, prodotto_id, quantita, prezzo_unitario,
                totale, note
            ) VALUES (?, ?, ?, ?, ?, ?)
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
                
                int quantita = (int)itemsTableModel.getValueAt(i, 2);
                double prezzoUnitario = Double.parseDouble(
                    ((String)itemsTableModel.getValueAt(i, 3)).replace(",", "."));
                double totale = Double.parseDouble(
                    ((String)itemsTableModel.getValueAt(i, 4)).replace(",", "."));
                String note = (String)itemsTableModel.getValueAt(i, 5);
                
                pstmt.setInt(1, orderId);
                pstmt.setInt(2, prodottoId);
                pstmt.setInt(3, quantita);
                pstmt.setDouble(4, prezzoUnitario);
                pstmt.setDouble(5, totale);
                pstmt.setString(6, note);
                pstmt.executeUpdate();
            }
        }
    }
    
    public boolean isOrderSaved() {
        return orderSaved;
    }
}
