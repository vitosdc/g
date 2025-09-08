
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarehouseMovementDialog extends JDialog {
    private WarehouseMovement movement;
    private boolean movementSaved = false;
    
    private JComboBox<ProductComboItem> productCombo;
    private JComboBox<String> tipoCombo;
    private JSpinner quantitySpinner;
    private JComboBox<String> causaleCombo;
    private JTextField documentoNumeroField;
    private JComboBox<String> documentoTipoCombo;
    private JTextArea noteArea;
    private SimpleDateFormat dateFormat;
    
    public WarehouseMovementDialog(JDialog parent, WarehouseMovement movement) {
        super(parent, movement == null ? "Nuovo Movimento" : "Modifica Movimento", true);
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
        
        // Prodotto
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("* Prodotto:"), gbc);
        
        gbc.gridx = 1;
        productCombo = new JComboBox<>();
        loadProducts();
        formPanel.add(productCombo, gbc);
        
        // Tipo movimento
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("* Tipo:"), gbc);
        
        gbc.gridx = 1;
        tipoCombo = new JComboBox<>(new String[]{"CARICO", "SCARICO"});
        formPanel.add(tipoCombo, gbc);
        
        // Quantità
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("* Quantità:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 99999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitySpinner, gbc);
        
        // Causale
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("* Causale:"), gbc);
        
        gbc.gridx = 1;
        causaleCombo = new JComboBox<>(new String[]{
            "ACQUISTO", "VENDITA", "RESO CLIENTE", "RESO FORNITORE",
            "INVENTARIO", "OMAGGIO", "FURTO/SMARRIMENTO", "ALTRO"
        });
        formPanel.add(causaleCombo, gbc);
        
        // Documento numero
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Documento N°:"), gbc);
        
        gbc.gridx = 1;
        documentoNumeroField = new JTextField(20);
        formPanel.add(documentoNumeroField, gbc);
        
        // Documento tipo
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Tipo Documento:"), gbc);
        
        gbc.gridx = 1;
        documentoTipoCombo = new JComboBox<>(new String[]{
            "", "DDT", "FATTURA", "ORDINE", "INVENTARIO"
        });
        formPanel.add(documentoTipoCombo, gbc);
        
        // Note
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Note:"), gbc);
        
        gbc.gridx = 1;
        noteArea = new JTextArea(4, 30);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(noteArea), gbc);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
        saveButton.addActionListener(e -> saveMovement());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout principale
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Legenda
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.add(new JLabel("* Campi obbligatori"));
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
                "Errore durante il caricamento dei prodotti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadMovementData() {
        // Seleziona il prodotto
        for (int i = 0; i < productCombo.getItemCount(); i++) {
            ProductComboItem item = (ProductComboItem)productCombo.getItemAt(i);
            if (item.getProduct().getId() == movement.getProdottoId()) {
                productCombo.setSelectedIndex(i);
                break;
            }
        }
        
        tipoCombo.setSelectedItem(movement.getTipo());
        quantitySpinner.setValue(movement.getQuantita());
        causaleCombo.setSelectedItem(movement.getCausale());
        documentoNumeroField.setText(movement.getDocumentoNumero());
        documentoTipoCombo.setSelectedItem(movement.getDocumentoTipo());
        noteArea.setText(movement.getNote());
        
        // Disabilita la modifica del prodotto per movimenti esistenti
        productCombo.setEnabled(false);
    }
    
    private void saveMovement() {
        try {
            // Validazione
            if (productCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                    "Seleziona un prodotto",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            ProductComboItem selectedProduct = (ProductComboItem)productCombo.getSelectedItem();
            String tipo = (String)tipoCombo.getSelectedItem();
            int quantita = (Integer)quantitySpinner.getValue();
            String causale = (String)causaleCombo.getSelectedItem();
            
            // Verifica disponibilità per scarichi
            if (tipo.equals("SCARICO")) {
                int disponibilita = selectedProduct.getProduct().getQuantita();
                if (movement != null && movement.getTipo().equals("SCARICO")) {
                    disponibilita += movement.getQuantita(); // Ripristina la quantità originale
                }
                if (quantita > disponibilita) {
                    JOptionPane.showMessageDialog(this,
                        "Quantità non disponibile. Disponibilità attuale: " + disponibilita,
                        "Errore", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            try {
                if (movement == null) {
                    // Inserisci nuovo movimento
                    String insertQuery = """
                        INSERT INTO movimenti_magazzino (
                            prodotto_id, data, tipo, quantita, causale,
                            documento_numero, documento_tipo, note
                        ) VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                        pstmt.setInt(1, selectedProduct.getProduct().getId());
                        pstmt.setString(2, tipo);
                        pstmt.setInt(3, quantita);
                        pstmt.setString(4, causale);
                        pstmt.setString(5, documentoNumeroField.getText().trim());
                        pstmt.setString(6, (String)documentoTipoCombo.getSelectedItem());
                        pstmt.setString(7, noteArea.getText().trim());
                        pstmt.executeUpdate();
                    }
                    
                } else {
                    // Aggiorna movimento esistente
                    String updateQuery = """
                        UPDATE movimenti_magazzino SET
                            tipo = ?, quantita = ?, causale = ?,
                            documento_numero = ?, documento_tipo = ?, note = ?
                        WHERE id = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                        pstmt.setString(1, tipo);
                        pstmt.setInt(2, quantita);
                        pstmt.setString(3, causale);
                        pstmt.setString(4, documentoNumeroField.getText().trim());
                        pstmt.setString(5, (String)documentoTipoCombo.getSelectedItem());
                        pstmt.setString(6, noteArea.getText().trim());
                        pstmt.setInt(7, movement.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Ripristina la quantità originale
                    String resetQuery = """
                        UPDATE prodotti 
                        SET quantita = quantita + ?
                        WHERE id = ?
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(resetQuery)) {
                        int deltaQuantita = movement.getTipo().equals("CARICO") ? 
                            -movement.getQuantita() : movement.getQuantita();
                        pstmt.setInt(1, deltaQuantita);
                        pstmt.setInt(2, movement.getProdottoId());
                        pstmt.executeUpdate();
                    }
                }
                
                // Aggiorna la quantità del prodotto
                String updateProductQuery = """
                    UPDATE prodotti 
                    SET quantita = quantita + ?
                    WHERE id = ?
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(updateProductQuery)) {
                    int deltaQuantita = tipo.equals("CARICO") ? quantita : -quantita;
                    pstmt.setInt(1, deltaQuantita);
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
                "Errore durante il salvataggio del movimento: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isMovementSaved() {
        return movementSaved;
    }
}
