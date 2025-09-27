import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ProductDialog extends JDialog {
    private JTextField codiceField;
    private JTextField nomeField;
    private JTextArea descrizioneArea;
    private JTextField prezzoField;
    private JSpinner quantitaSpinner;
    private boolean productSaved = false;
    private Product product;
    
    // Constructor for JFrame parent
    public ProductDialog(JFrame parent, Product product) {
        super(parent, product == null ? "New Product" : "Edit Product", true);
        this.product = product;
        
        setupDialog();
        initComponents();
        if (product != null) {
            loadProductData();
        }
    }
    
    // Constructor for JDialog parent
    public ProductDialog(JDialog parent, Product product) {
        super(parent, product == null ? "New Product" : "Edit Product", true);
        this.product = product;
        
        setupDialog();
        initComponents();
        if (product != null) {
            loadProductData();
        }
    }
    
    private void setupDialog() {
        setSize(400, 500);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Code
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Code:"), gbc);
        
        gbc.gridx = 1;
        codiceField = new JTextField(20);
        formPanel.add(codiceField, gbc);
        
        // Name
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        nomeField = new JTextField(20);
        formPanel.add(nomeField, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        descrizioneArea = new JTextArea(4, 20);
        descrizioneArea.setLineWrap(true);
        descrizioneArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(descrizioneArea), gbc);
        
        // Price
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Price:"), gbc);
        
        gbc.gridx = 1;
        prezzoField = new JTextField(20);
        formPanel.add(prezzoField, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Quantity:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 999999, 1);
        quantitaSpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitaSpinner, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveProduct());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Main layout
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadProductData() {
        codiceField.setText(product.getCodice());
        nomeField.setText(product.getNome());
        descrizioneArea.setText(product.getDescrizione());
        prezzoField.setText(String.valueOf(product.getPrezzo()));
        quantitaSpinner.setValue(product.getQuantita());
    }
    
    private void saveProduct() {
        try {
            // Validation
            String codice = codiceField.getText().trim();
            String nome = nomeField.getText().trim();
            String descrizione = descrizioneArea.getText().trim();
            double prezzo = Double.parseDouble(prezzoField.getText().trim());
            int quantita = (int)quantitaSpinner.getValue();
            
            if (codice.isEmpty() || nome.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Code and Name fields are required",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            if (product == null) { // New product
                String query = """
                    INSERT INTO prodotti (codice, nome, descrizione, prezzo, quantita)
                    VALUES (?, ?, ?, ?, ?)
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, codice);
                    pstmt.setString(2, nome);
                    pstmt.setString(3, descrizione);
                    pstmt.setDouble(4, prezzo);
                    pstmt.setInt(5, quantita);
                    pstmt.executeUpdate();
                }
            } else { // Edit product
                String query = """
                    UPDATE prodotti
                    SET codice = ?, nome = ?, descrizione = ?, prezzo = ?, quantita = ?
                    WHERE id = ?
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, codice);
                    pstmt.setString(2, nome);
                    pstmt.setString(3, descrizione);
                    pstmt.setDouble(4, prezzo);
                    pstmt.setInt(5, quantita);
                    pstmt.setInt(6, product.getId());
                    pstmt.executeUpdate();
                }
            }
            
            productSaved = true;
            dispose();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Price must be a valid number",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error while saving product: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isProductSaved() {
        return productSaved;
    }
}