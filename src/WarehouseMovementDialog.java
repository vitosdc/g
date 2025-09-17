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
    private JLabel availabilityLabel;
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
        
        // Initial availability check
        SwingUtilities.invokeLater(this::updateAvailabilityCheck);
    }
    
    private void setupWindow() {
        setSize(550, 580); // FIXED: Reduced height for better screen fit
        setLocationRelativeTo(null); // FIXED: Center on screen instead of relative to parent
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(500, 550)); // FIXED: Reduced minimum size
    }
    
    private void initComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // Form panel with improved layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // FIXED: Reduced spacing for compactness
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // FIXED: Allow horizontal expansion
        
        // Product
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.0; // Label doesn't expand
        formPanel.add(new JLabel("* Product:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0; // ComboBox expands
        productCombo = new JComboBox<>();
        productCombo.setPreferredSize(new Dimension(300, 28)); // FIXED: Slightly smaller height
        productCombo.addActionListener(e -> updateAvailabilityCheck());
        loadProducts();
        formPanel.add(productCombo, gbc);
        
        // Movement type
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("* Type:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        typeCombo = new JComboBox<>(new String[]{"INWARD", "OUTWARD"});
        typeCombo.setPreferredSize(new Dimension(300, 28));
        typeCombo.addActionListener(e -> updateAvailabilityCheck());
        formPanel.add(typeCombo, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("* Quantity:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 99999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        quantitySpinner.setPreferredSize(new Dimension(300, 28));
        quantitySpinner.addChangeListener(e -> updateAvailabilityCheck());
        
        // FIXED: Make spinner editor wider
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) quantitySpinner.getEditor();
        editor.getTextField().setColumns(10);
        
        formPanel.add(quantitySpinner, gbc);
        
        // Availability info label - FIXED: Better positioning
        gbc.gridx = 0; gbc.gridy = 3; 
        gbc.gridwidth = 2; // Span across both columns
        gbc.weightx = 1.0;
        availabilityLabel = new JLabel(" ");
        availabilityLabel.setOpaque(true);
        availabilityLabel.setBackground(new Color(245, 245, 245));
        availabilityLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8) // FIXED: Reduced padding
        ));
        availabilityLabel.setPreferredSize(new Dimension(450, 28)); // FIXED: Reduced height
        formPanel.add(availabilityLabel, gbc);
        gbc.gridwidth = 1; // Reset
        
        // Reason
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("* Reason:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        reasonCombo = new JComboBox<>(new String[]{
            "PURCHASE", "SALE", "CUSTOMER RETURN", "SUPPLIER RETURN",
            "INVENTORY", "GIFT", "THEFT/LOSS", "OTHER"
        });
        reasonCombo.setPreferredSize(new Dimension(300, 28));
        formPanel.add(reasonCombo, gbc);
        
        // Document number
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Document N°:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        documentNumberField = new JTextField();
        documentNumberField.setPreferredSize(new Dimension(300, 28));
        formPanel.add(documentNumberField, gbc);
        
        // Document type
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Document Type:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        documentTypeCombo = new JComboBox<>(new String[]{
            "", "DDT", "INVOICE", "ORDER", "INVENTORY"
        });
        documentTypeCombo.setPreferredSize(new Dimension(300, 28));
        formPanel.add(documentTypeCombo, gbc);
        
        // Notes - FIXED: Better layout for text area
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST; // Align label to top
        formPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // FIXED: Allow vertical expansion
        gbc.fill = GridBagConstraints.BOTH; // Fill both directions
        gbc.anchor = GridBagConstraints.CENTER;
        notesArea = new JTextArea(3, 25); // FIXED: Reduced from 4 to 3 rows
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3)); // FIXED: Reduced padding
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setPreferredSize(new Dimension(300, 65)); // FIXED: Reduced from 100 to 65
        notesScroll.setMinimumSize(new Dimension(300, 60)); // FIXED: Reduced minimum
        notesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formPanel.add(notesScroll, gbc);
        
        // Buttons panel - FIXED: Better button layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8)); // FIXED: Reduced vertical padding
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        // FIXED: Improve button appearance
        saveButton.setPreferredSize(new Dimension(90, 32)); // FIXED: Slightly smaller buttons
        cancelButton.setPreferredSize(new Dimension(90, 32));
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        
        saveButton.addActionListener(e -> saveMovement());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Legend panel - FIXED: Better positioning
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel legendLabel = new JLabel("* Required fields");
        legendLabel.setFont(legendLabel.getFont().deriveFont(Font.ITALIC));
        legendLabel.setForeground(Color.GRAY);
        legendPanel.add(legendLabel);
        
        // Assembly with proper layout
        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setBorder(null);
        
        mainPanel.add(legendPanel, BorderLayout.NORTH);
        mainPanel.add(formScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
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
    
    private void updateAvailabilityCheck() {
        try {
            if (availabilityLabel == null) return;
            
            ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
            if (selectedProduct == null) {
                availabilityLabel.setText("Select a product to see availability information");
                availabilityLabel.setForeground(Color.GRAY);
                return;
            }
            
            String type = (String)typeCombo.getSelectedItem();
            int requestedQuantity = (Integer)quantitySpinner.getValue();
            int currentStock = selectedProduct.getProduct().getQuantita();
            
            if ("OUTWARD".equals(type)) {
                if (movement != null && "OUTWARD".equals(movement.getTipo())) {
                    // If editing an outward movement, add back the original quantity
                    currentStock += movement.getQuantita();
                }
                
                if (requestedQuantity > currentStock) {
                    availabilityLabel.setText(String.format(
                        "⚠️ Insufficient stock! Available: %d, Requested: %d", 
                        currentStock, requestedQuantity));
                    availabilityLabel.setForeground(Color.RED);
                    availabilityLabel.setBackground(new Color(255, 240, 240));
                } else {
                    availabilityLabel.setText(String.format(
                        "✓ Available: %d, After movement: %d", 
                        currentStock, currentStock - requestedQuantity));
                    availabilityLabel.setForeground(new Color(0, 120, 0));
                    availabilityLabel.setBackground(new Color(240, 255, 240));
                }
            } else {
                availabilityLabel.setText(String.format(
                    "➕ Current: %d, After movement: %d", 
                    currentStock, currentStock + requestedQuantity));
                availabilityLabel.setForeground(new Color(0, 120, 0));
                availabilityLabel.setBackground(new Color(240, 255, 240));
            }
        } catch (Exception e) {
            // Silent fail - not critical
            if (availabilityLabel != null) {
                availabilityLabel.setText("Error checking availability");
                availabilityLabel.setForeground(Color.RED);
            }
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
        documentNumberField.setText(movement.getDocumentoNumero() != null ? movement.getDocumentoNumero() : "");
        documentTypeCombo.setSelectedItem(movement.getDocumentoTipo() != null ? movement.getDocumentoTipo() : "");
        notesArea.setText(movement.getNote() != null ? movement.getNote() : "");
        
        // Disable product modification for existing movements
        productCombo.setEnabled(false);
        
        // Update availability info
        updateAvailabilityCheck();
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
            if ("OUTWARD".equals(type)) {
                int availability = selectedProduct.getProduct().getQuantita();
                if (movement != null && "OUTWARD".equals(movement.getTipo())) {
                    availability += movement.getQuantita(); // Restore original quantity
                }
                if (quantity > availability) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        String.format("Insufficient quantity. Current availability: %d\n" +
                                     "Requested: %d\n\n" +
                                     "Do you want to proceed anyway?\n" +
                                     "This will result in negative stock.", 
                                     availability, quantity),
                        "Insufficient Stock",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
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
                        
                        String docNumber = documentNumberField.getText().trim();
                        pstmt.setString(5, docNumber.isEmpty() ? null : docNumber);
                        
                        String docType = (String)documentTypeCombo.getSelectedItem();
                        pstmt.setString(6, (docType == null || docType.trim().isEmpty()) ? null : docType);
                        
                        String notes = notesArea.getText().trim();
                        pstmt.setString(7, notes.isEmpty() ? null : notes);
                        
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
                        
                        String docNumber = documentNumberField.getText().trim();
                        pstmt.setString(4, docNumber.isEmpty() ? null : docNumber);
                        
                        String docType = (String)documentTypeCombo.getSelectedItem();
                        pstmt.setString(5, (docType == null || docType.trim().isEmpty()) ? null : docType);
                        
                        String notes = notesArea.getText().trim();
                        pstmt.setString(6, notes.isEmpty() ? null : notes);
                        
                        pstmt.setInt(7, movement.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Restore original quantity before applying new changes
                    String resetQuery = """
                        UPDATE prodotti 
                        SET quantita = quantita + ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(resetQuery)) {
                        int quantityDelta = "INWARD".equals(movement.getTipo()) ? 
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
                    int quantityDelta = "INWARD".equals(type) ? quantity : -quantity;
                    pstmt.setInt(1, quantityDelta);
                    pstmt.setInt(2, selectedProduct.getProduct().getId());
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                movementSaved = true;
                
                JOptionPane.showMessageDialog(this,
                    "Movement saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
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