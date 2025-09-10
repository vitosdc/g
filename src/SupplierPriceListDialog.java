import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

public class SupplierPriceListDialog extends JDialog {
    private int supplierId;
    private String supplierName;
    private SupplierPriceList priceList;
    private boolean priceSaved = false;
    
    private JComboBox<ProductDisplay> productCombo;
    private JTextField codiceFornitoreField;
    private JTextField prezzoField;
    private JSpinner quantitaMinimaSpinner;
    private JTextField dataInizioField;
    private JTextField dataFineField;
    private JTextArea noteArea;
    private SimpleDateFormat dateFormat;
    
    public SupplierPriceListDialog(JDialog parent, int supplierId, String supplierName, SupplierPriceList priceList) {
        super(parent, priceList == null ? "New Price" : "Edit Price", true);
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.priceList = priceList;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadProducts();
        if (priceList != null) {
            loadPriceData();
        }
    }
    
    private void setupWindow() {
        setSize(500, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Supplier
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Supplier:"), gbc);
        
        gbc.gridx = 1;
        JTextField supplierField = new JTextField(supplierName);
        supplierField.setEditable(false);
        formPanel.add(supplierField, gbc);
        
        // Product
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("* Product:"), gbc);
        
        gbc.gridx = 1;
        productCombo = new JComboBox<>();
        if (priceList != null) {
            productCombo.setEnabled(false); // Do not allow product change in edit mode
        }
        formPanel.add(productCombo, gbc);
        
        // Supplier Code
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Supplier Code:"), gbc);
        
        gbc.gridx = 1;
        codiceFornitoreField = new JTextField(20);
        formPanel.add(codiceFornitoreField, gbc);
        
        // Price
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("* Price €:"), gbc);
        
        gbc.gridx = 1;
        prezzoField = new JTextField(10);
        formPanel.add(prezzoField, gbc);
        
        // Minimum quantity
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Minimum Quantity:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 9999, 1);
        quantitaMinimaSpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitaMinimaSpinner, gbc);
        
        // Validity start date
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("* Validity Start Date:"), gbc);
        
        gbc.gridx = 1;
        dataInizioField = new JTextField(10);
        dataInizioField.setText(dateFormat.format(new Date()));
        formPanel.add(dataInizioField, gbc);
        
        // Validity end date
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Validity End Date:"), gbc);
        
        gbc.gridx = 1;
        dataFineField = new JTextField(10);
        formPanel.add(dataFineField, gbc);
        
        // Notes
        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        noteArea = new JTextArea(4, 30);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(noteArea), gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> savePrice());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Main layout
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.add(new JLabel("* Required fields"));
        add(legendPanel, BorderLayout.NORTH);
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
                    productCombo.addItem(new ProductDisplay(product));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static class ProductDisplay {
        private Product product;
        
        public ProductDisplay(Product product) {
            this.product = product;
        }
        
        public Product getProduct() { return product; }
        
        @Override
        public String toString() {
            return String.format("%s - %s", product.getCodice(), product.getNome());
        }
    }
    
    private void loadPriceData() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT l.*, p.nome as prodotto_nome
                FROM listini_fornitori l
                JOIN prodotti p ON l.prodotto_id = p.id
                WHERE l.id = ?
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, priceList.getId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Select product
                        for (int i = 0; i < productCombo.getItemCount(); i++) {
                            ProductDisplay item = (ProductDisplay)productCombo.getItemAt(i);
                            if (item.getProduct().getId() == rs.getInt("prodotto_id")) {
                                productCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                        
                        codiceFornitoreField.setText(rs.getString("codice_prodotto_fornitore"));
                        prezzoField.setText(String.format("%.2f", rs.getDouble("prezzo")));
                        quantitaMinimaSpinner.setValue(rs.getInt("quantita_minima"));
                        dataInizioField.setText(dateFormat.format(rs.getDate("data_validita_inizio")));
                        Date dataFine = rs.getDate("data_validita_fine");
                        if (dataFine != null) {
                            dataFineField.setText(dateFormat.format(dataFine));
                        }
                        noteArea.setText(rs.getString("note"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading price: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void savePrice() {
        try {
            // Validation
            if (productCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                    "Select a product",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String prezzoText = prezzoField.getText().trim().replace(",", ".");
            if (prezzoText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Enter a price",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            double prezzo;
            try {
                prezzo = Double.parseDouble(prezzoText);
                if (prezzo <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Price must be a positive number",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parsing date
            Date dataInizio;
            Date dataFine = null;
            try {
                dataInizio = dateFormat.parse(dataInizioField.getText());
                String dataFineText = dataFineField.getText().trim();
                if (!dataFineText.isEmpty()) {
                    dataFine = dateFormat.parse(dataFineText);
                    if (dataFine.before(dataInizio)) {
                        JOptionPane.showMessageDialog(this,
                            "The end validity date must be after the start date",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid date format. Use dd/MM/yyyy format",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            ProductDisplay selectedProduct = (ProductDisplay)productCombo.getSelectedItem();
            int prodottoId = selectedProduct.getProduct().getId();
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            if (priceList == null) {
                // Check if a valid price already exists for this product
                String checkQuery = """
                    SELECT id FROM listini_fornitori 
                    WHERE fornitore_id = ? AND prodotto_id = ?
                    AND (data_validita_fine IS NULL OR data_validita_fine >= ?)
                    AND data_validita_inizio <= ?
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setInt(1, supplierId);
                    pstmt.setInt(2, prodottoId);
                    pstmt.setDate(3, new java.sql.Date(dataInizio.getTime()));
                    pstmt.setDate(4, dataFine != null ? new java.sql.Date(dataFine.getTime()) : 
                                                       new java.sql.Date(dataInizio.getTime()));
                    
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this,
                            "A valid price already exists for this product in the specified period",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                // Insert new price
                String insertQuery = """
                    INSERT INTO listini_fornitori (
                        fornitore_id, prodotto_id, codice_prodotto_fornitore,
                        prezzo, quantita_minima, data_validita_inizio,
                        data_validita_fine, note
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setInt(1, supplierId);
                    pstmt.setInt(2, prodottoId);
                    pstmt.setString(3, codiceFornitoreField.getText().trim());
                    pstmt.setDouble(4, prezzo);
                    pstmt.setInt(5, (Integer)quantitaMinimaSpinner.getValue());
                    pstmt.setDate(6, new java.sql.Date(dataInizio.getTime()));
                    pstmt.setDate(7, dataFine != null ? new java.sql.Date(dataFine.getTime()) : null);
                    pstmt.setString(8, noteArea.getText().trim());
                    pstmt.executeUpdate();
                }
                
            } else {
                // Update existing price
                String updateQuery = """
                    UPDATE listini_fornitori SET
                        codice_prodotto_fornitore = ?,
                        prezzo = ?, quantita_minima = ?,
                        data_validita_inizio = ?, data_validita_fine = ?,
                        note = ?
                    WHERE id = ?
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setString(1, codiceFornitoreField.getText().trim());
                    pstmt.setDouble(2, prezzo);
                    pstmt.setInt(3, (Integer)quantitaMinimaSpinner.getValue());
                    pstmt.setDate(4, new java.sql.Date(dataInizio.getTime()));
                    pstmt.setDate(5, dataFine != null ? new java.sql.Date(dataFine.getTime()) : null);
                    pstmt.setString(6, noteArea.getText().trim());
                    pstmt.setInt(7, priceList.getId());
                    pstmt.executeUpdate();
                }
            }
            
            priceSaved = true;
            dispose();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error saving price: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
                	}
                }
                
         public boolean isPriceSaved() {
               return priceSaved;
               }
     }