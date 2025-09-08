// File: ProductDialog.java
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
    
    public ProductDialog(JDialog parent, Product product) {
        super(parent, product == null ? "Nuovo Prodotto" : "Modifica Prodotto", true);
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
        // Pannello form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Codice
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Codice:"), gbc);
        
        gbc.gridx = 1;
        codiceField = new JTextField(20);
        formPanel.add(codiceField, gbc);
        
        // Nome
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Nome:"), gbc);
        
        gbc.gridx = 1;
        nomeField = new JTextField(20);
        formPanel.add(nomeField, gbc);
        
        // Descrizione
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Descrizione:"), gbc);
        
        gbc.gridx = 1;
        descrizioneArea = new JTextArea(4, 20);
        descrizioneArea.setLineWrap(true);
        descrizioneArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(descrizioneArea), gbc);
        
        // Prezzo
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Prezzo:"), gbc);
        
        gbc.gridx = 1;
        prezzoField = new JTextField(20);
        formPanel.add(prezzoField, gbc);
        
        // Quantità
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Quantità:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 999999, 1);
        quantitaSpinner = new JSpinner(spinnerModel);
        formPanel.add(quantitaSpinner, gbc);
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
        saveButton.addActionListener(e -> saveProduct());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout principale
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
            // Validazione
            String codice = codiceField.getText().trim();
            String nome = nomeField.getText().trim();
            String descrizione = descrizioneArea.getText().trim();
            double prezzo = Double.parseDouble(prezzoField.getText().trim());
            int quantita = (int)quantitaSpinner.getValue();
            
            if (codice.isEmpty() || nome.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "I campi Codice e Nome sono obbligatori",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            if (product == null) { // Nuovo prodotto
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
            } else { // Modifica prodotto
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
                "Il prezzo deve essere un numero valido",
                "Errore", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Errore durante il salvataggio del prodotto: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isProductSaved() {
        return productSaved;
    }
}