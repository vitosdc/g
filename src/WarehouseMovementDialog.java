import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarehouseMovementDialog extends JDialog {
    private WarehouseMovement movement;
    private boolean movementSaved = false;
    
    private JComboBox<ProductComboItem> productCombo;
    private JComboBox<String> typeCombo;
    private JSpinner quantitySpinner;
    private JComboBox<String> reasonCombo;
    private JTextField documentNumberField;
    private JComboBox<String> documentTypeCombo;
    private JTextArea notesArea;
    private SimpleDateFormat dateFormat;
    
    public WarehouseMovementDialog(JDialog parent, WarehouseMovement movement) {
        super(parent, movement == null ? "New Movement" : "Edit Movement", true);
        this.movement = movement;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        setupWindow();
        initComponents();
        if (movement != null) {
            loadMovementData();
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
        
        // Product
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("* Product:"), gbc);
        
        gbc.gridx = 1;
        productCombo = new JComboBox<>();
        loadProducts();
        formPanel.add(productCombo, gbc);
        
        // Movement type
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("* Type:"), gbc);
        
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(new String[]{"INWARD", "OUTWARD"});
        formPanel.add(typeCombo, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("* Quantity:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 99999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitySpinner, gbc);
        
        // Reason
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("* Reason:"), gbc);
        
        gbc.gridx = 1;
        reasonCombo = new JComboBox<>(new String[]{
            "PURCHASE", "SALE", "CUSTOMER RETURN", "SUPPLIER RETURN",
            "INVENTORY", "GIFT", "THEFT/LOSS", "OTHER"
        });
        formPanel.add(reasonCombo, gbc);
        
        // Document number
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Document N°:"), gbc);
        
        gbc.gridx = 1;
        documentNumberField = new JTextField(20);
        formPanel.add(documentNumberField, gbc);
        
        // Document type
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Document Type:"), gbc);
        
        gbc.gridx = 1;
        documentTypeCombo = new JComboBox<>(new String[]{
            "", "DDT", "INVOICE", "ORDER", "INVENTORY"
        });
        formPanel.add(documentTypeCombo, gbc);
        
        // Notes
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        notesArea = new JTextArea(4, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(notesArea), gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveMovement());
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
                    productCombo.addItem(new ProductComboItem(product));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadMovementData() {
        // Select the product
        for (int i = 0; i < productCombo.getItemCount(); i++) {
            ProductComboItem item = (ProductComboItem)productCombo.getItemAt(i);
            if (item.getProduct().getId() == movement.getProdottoId()) {
                productCombo.setSelectedIndex(i);
                break;
            }
        }
        
        typeCombo.setSelectedItem(movement.getTipo());
        quantitySpinner.setValue(movement.getQuantita());
        reasonCombo.setSelectedItem(movement.getCausale());
        documentNumberField.setText(movement.getDocumentoNumero());
        documentTypeCombo.setSelectedItem(movement.getDocumentoTipo());
        notesArea.setText(movement.getNote());
        
        // Disable product modification for existing movements
        productCombo.setEnabled(false);
    }
    
    private void saveMovement() {
        try {
            // Validation
            if (productCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                    "Select a product",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
            String type = (String)typeCombo.getSelectedItem();
            int quantity = (Integer)quantitySpinner.getValue();
            String reason = (String)reasonCombo.getSelectedItem();
            
            // Check availability for outward movements
            if (type.equals("OUTWARD")) {
                int availability = selectedProduct.getProduct().getQuantita();
                if (movement != null && movement.getTipo().equals("OUTWARD")) {
                    availability += movement.getQuantita(); // Restore original quantity
                }
                if (quantity > availability) {
                    JOptionPane.showMessageDialog(this,
                        "Insufficient quantity. Current availability: " + availability,
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            try {
                if (movement == null) {
                    // Insert new movement
                    String insertQuery = """
                        INSERT INTO movimenti_magazzino (
                            prodotto_id, data, tipo, quantita, causale,
                            documento_numero, documento_tipo, note
                        ) VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                        pstmt.setInt(1, selectedProduct.getProduct().getId());
                        pstmt.setString(2, type);
                        pstmt.setInt(3, quantity);
                        pstmt.setString(4, reason);
                        pstmt.setString(5, documentNumberField.getText().trim());
                        pstmt.setString(6, (String)documentTypeCombo.getSelectedItem());
                        pstmt.setString(7, notesArea.getText().trim());
                        pstmt.executeUpdate();
                    }
                    
                } else {
                    // Update existing movement
                    String updateQuery = """
                        UPDATE movimenti_magazzino SET
                            tipo = ?, quantita = ?, causale = ?,
                            documento_numero = ?, documento_tipo = ?, note = ?
                        WHERE id = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                        pstmt.setString(1, type);
                        pstmt.setInt(2, quantity);
                        pstmt.setString(3, reason);
                        pstmt.setString(4, documentNumberField.getText().trim());
                        pstmt.setString(5, (String)documentTypeCombo.getSelectedItem());
                        pstmt.setString(6, notesArea.getText().trim());
                        pstmt.setInt(7, movement.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Restore original quantity
                    String resetQuery = """
                        UPDATE prodotti 
                        SET quantita = quantita + ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(resetQuery)) {
                        int quantityDelta = movement.getTipo().equals("INWARD") ? 
                            -movement.getQuantita() : movement.getQuantita();
                        pstmt.setInt(1, quantityDelta);
                        pstmt.setInt(2, movement.getProdottoId());
                        pstmt.executeUpdate();
                    }
                }
                
                // Update product quantity
                String updateProductQuery = """
                    UPDATE prodotti 
                    SET quantita = quantita + ?
                    WHERE id = ?
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(updateProductQuery)) {
                    int quantityDelta = type.equals("INWARD") ? quantity : -quantity;
                    pstmt.setInt(1, quantityDelta);
                    pstmt.setInt(2, selectedProduct.getProduct().getId());
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                movementSaved = true;
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
                "Error saving movement: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isMovementSaved() {
        return movementSaved;
    }
}