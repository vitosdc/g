
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;  // Specifichiamo esplicitamente java.util.Date

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
    
    public InvoiceDialog(JDialog parent, Invoice invoice) {
        super(parent, invoice == null ? "Nuova Fattura" : "Modifica Fattura", true);
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
        // Panel principale con padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superiore per i dati della fattura
        JPanel invoicePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Numero fattura
        gbc.gridx = 0; gbc.gridy = 0;
        invoicePanel.add(new JLabel("Numero:"), gbc);
        
        gbc.gridx = 1;
        numeroField = new JTextField(15);
        numeroField.setEditable(false); // Il numero viene generato automaticamente
        invoicePanel.add(numeroField, gbc);
        
        // Data
        gbc.gridx = 2;
        invoicePanel.add(new JLabel("Data:"), gbc);
        
        gbc.gridx = 3;
        dataField = new JTextField(10);
        dataField.setText(dateFormat.format(new Date()));
        invoicePanel.add(dataField, gbc);
        
        // Cliente
        gbc.gridx = 0; gbc.gridy = 1;
        invoicePanel.add(new JLabel("Cliente:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        clienteCombo = new JComboBox<>();
        loadClienti();
        invoicePanel.add(clienteCombo, gbc);
        
        // Stato
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        invoicePanel.add(new JLabel("Stato:"), gbc);
        
        gbc.gridx = 1;
        statoCombo = new JComboBox<>(new String[]{"Bozza", "Emessa", "Pagata", "Annullata"});
        invoicePanel.add(statoCombo, gbc);
        
        // Tabella prodotti
        String[] columns = {"Codice", "Prodotto", "Quantità", "Prezzo Unit.", "Aliquota IVA", "Totale"};
        itemsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Solo la quantità è modificabile
            }
        };
        itemsTable = new JTable(itemsTableModel);
        
        // Aggiorna i totali quando cambia la quantità
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
        
        // Panel per i totali
        JPanel totalsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        imponibileLabel = new JLabel("Imponibile: € 0,00");
        ivaLabel = new JLabel("IVA: € 0,00");
        totaleLabel = new JLabel("Totale: € 0,00");
        
        totalsPanel.add(imponibileLabel);
        totalsPanel.add(ivaLabel);
        totalsPanel.add(totaleLabel);
        
        // Panel pulsanti
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
        saveButton.addActionListener(e -> saveInvoice());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout principale
        mainPanel.add(invoicePanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tableButtonPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        centerPanel.add(totalsPanel, BorderLayout.SOUTH);
        
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
    
    private void setupNewInvoice() {
        try {
            // Genera nuovo numero fattura
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            String numero = DatabaseManager.getInstance().getNextInvoiceNumber(year);
            numeroField.setText(numero);
            
            // Imposta data odierna
            dataField.setText(dateFormat.format(cal.getTime()));
            
            // Imposta stato iniziale
            statoCombo.setSelectedItem("Bozza");
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la generazione del numero fattura: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadInvoiceData() {
        numeroField.setText(invoice.getNumero());
        dataField.setText(dateFormat.format(invoice.getData()));
        statoCombo.setSelectedItem(invoice.getStato());
        
        // Seleziona il cliente
        for (int i = 0; i < clienteCombo.getItemCount(); i++) {
            CustomerComboItem item = clienteCombo.getItemAt(i);
            if (item.getId() == invoice.getClienteId()) {
                clienteCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Carica i prodotti
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
    
    private void showAddProductDialog() {
        JDialog dialog = new JDialog(this, "Aggiungi Prodotto", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Prodotto
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Prodotto:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ProductComboItem> productCombo = new JComboBox<>();
        for (Product product : productsCache.values()) {
            productCombo.addItem(new ProductComboItem(product));
        }
        panel.add(productCombo, gbc);
        
        // Quantità
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantità:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
        JSpinner quantitySpinner = new JSpinner(spinnerModel);
        panel.add(quantitySpinner, gbc);
        
        // Aliquota IVA
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Aliquota IVA %:"), gbc);
        
        gbc.gridx = 1;
        JTextField ivaField = new JTextField("22.0");
        panel.add(ivaField, gbc);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Aggiungi");
        JButton cancelButton = new JButton("Annulla");
        
        addButton.addActionListener(e -> {
            try {
                ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
                int quantity = (int)quantitySpinner.getValue();
                double aliquotaIva = Double.parseDouble(ivaField.getText().replace(",", "."));
                
                // Verifica se il prodotto è già presente
                for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
                    String codice = (String)itemsTableModel.getValueAt(i, 0);
                    if (codice.equals(selectedProduct.getProduct().getCodice())) {
                        JOptionPane.showMessageDialog(dialog,
                            "Questo prodotto è già presente nella fattura",
                            "Avviso", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                
                // Calcola il totale
                double prezzo = selectedProduct.getProduct().getPrezzo();
                double totale = quantity * prezzo;
                
                // Aggiungi alla tabella
                Vector<Object> row = new Vector<>();
                row.add(selectedProduct.getProduct().getCodice());
                row.add(selectedProduct.getProduct().getNome());
                row.add(quantity);
                row.add(String.format("%.2f", prezzo));
                row.add(String.format("%.2f", aliquotaIva));
                row.add(String.format("%.2f", totale));
                itemsTableModel.addRow(row);
                
                updateTotals();
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Inserire un valore valido per l'aliquota IVA",
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
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
        double imponibile = 0;
        double totaleIva = 0;
        
        for (int i = 0; i < itemsTableModel.getRowCount(); i++) {
            int quantity = (int)itemsTableModel.getValueAt(i, 2);
            String prezzoStr = (String)itemsTableModel.getValueAt(i, 3);
            String aliquotaStr = (String)itemsTableModel.getValueAt(i, 4);
            
            double price = Double.parseDouble(prezzoStr.replace(",", "."));
            double aliquotaIva = Double.parseDouble(aliquotaStr.replace(",", "."));
            
            double subtotal = quantity * price;
            double iva = subtotal * (aliquotaIva / 100);
            
            imponibile += subtotal;
            totaleIva += iva;
        }
        
        double totale = imponibile + totaleIva;
        
        imponibileLabel.setText(String.format("Imponibile: € %.2f", imponibile));
        ivaLabel.setText(String.format("IVA: € %.2f", totaleIva));
        totaleLabel.setText(String.format("Totale: € %.2f", totale));
    }
    
    private void saveInvoice() {
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
                    "Aggiungi almeno un prodotto alla fattura",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parsing data
            Date dataFattura;
            try {
                dataFattura = dateFormat.parse(dataField.getText());
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                    "Formato data non valido. Usa il formato dd/MM/yyyy",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Recupera i valori
            CustomerComboItem selectedCustomer = (CustomerComboItem)clienteCombo.getSelectedItem();
            String stato = (String)statoCombo.getSelectedItem();
            String numero = numeroField.getText();
            
            // Calcola i totali
            double imponibile = Double.parseDouble(imponibileLabel.getText()
                .replace("Imponibile: € ", "").replace(",", "."));
            double iva = Double.parseDouble(ivaLabel.getText()
                .replace("IVA: € ", "").replace(",", "."));
            double totale = Double.parseDouble(totaleLabel.getText()
                .replace("Totale: € ", "").replace(",", "."));
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            try {
                if (invoice == null) { // Nuova fattura
                    // Inserisci la fattura
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
                                throw new SQLException("Impossibile ottenere l'ID della fattura");
                            }
                        }
                    }
                    
                    // Inserisci i dettagli della fattura
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
                            
                            int quantita = (int)itemsTableModel.getValueAt(i, 2);
                            double prezzoUnitario = Double.parseDouble(((String)itemsTableModel.getValueAt(i, 3)).replace(",", "."));
                            double aliquotaIva = Double.parseDouble(((String)itemsTableModel.getValueAt(i, 4)).replace(",", "."));
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
                    
                } else { // Modifica fattura esistente
                    // Aggiorna la fattura
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
                    
                    // Elimina i vecchi dettagli
                    String deleteDetailsQuery = "DELETE FROM dettagli_fattura WHERE fattura_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, invoice.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Inserisci i nuovi dettagli
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
                            
                            int quantita = (int)itemsTableModel.getValueAt(i, 2);
                            double prezzoUnitario = Double.parseDouble(((String)itemsTableModel.getValueAt(i, 3)).replace(",", "."));
                            double aliquotaIva = Double.parseDouble(((String)itemsTableModel.getValueAt(i, 4)).replace(",", "."));
                            double totaleProdotto = quantita * prezzoUnitario;
                            
                            pstmt.setInt(1, invoice.getId());
                            pstmt.setInt(2, prodottoId);
                            pstmt.setInt(3, quantita);
                            pstmt.setDouble(4, prezzoUnitario);
                            pstmt.setDouble(5, aliquotaIva);
                            pstmt.setDouble(6, totaleProdotto);
                            pstmt.executeUpdate();
                        }
                    }
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
                "Errore durante il salvataggio della fattura: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isInvoiceSaved() {
        return invoiceSaved;
    }
}
