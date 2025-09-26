import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class SupplierDialog extends JDialog {
    private Supplier supplier;
    private boolean supplierSaved = false;
    
    private JTextField ragioneSocialeField;
    private JTextField partitaIvaField;
    private JTextField codiceFiscaleField;
    private JTextField indirizzoField;
    private JTextField telefonoField;
    private JTextField emailField;
    private JTextField pecField;
    private JTextField sitoWebField;
    private JTextArea noteArea;
    
    // Constructor for JFrame parent
    public SupplierDialog(JFrame parent, Supplier supplier) {
        super(parent, supplier == null ? "New Supplier" : "Edit Supplier", true);
        this.supplier = supplier;
        
        setupWindow();
        initComponents();
        if (supplier != null) {
            loadSupplierData();
        }
    }
    
    // Constructor for JDialog parent
    public SupplierDialog(JDialog parent, Supplier supplier) {
        super(parent, supplier == null ? "New Supplier" : "Edit Supplier", true);
        this.supplier = supplier;
        
        setupWindow();
        initComponents();
        if (supplier != null) {
            loadSupplierData();
        }
    }
    
    private void setupWindow() {
        setLayout(new BorderLayout(10, 10));
        pack();
        setMinimumSize(new Dimension(650, 550));
        setLocationRelativeTo(getOwner());
    }
    
    private void initComponents() {
        // Pannello form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Ragione Sociale
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("* Company Name:"), gbc);
        
        gbc.gridx = 1;
        ragioneSocialeField = new JTextField(30);
        formPanel.add(ragioneSocialeField, gbc);
        
        // Partita IVA
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("* VAT Number:"), gbc);
        
        gbc.gridx = 1;
        partitaIvaField = new JTextField(20);
        formPanel.add(partitaIvaField, gbc);
        
        // Codice Fiscale
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Tax Code:"), gbc);
        
        gbc.gridx = 1;
        codiceFiscaleField = new JTextField(20);
        formPanel.add(codiceFiscaleField, gbc);
        
        // Indirizzo
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Address:"), gbc);
        
        gbc.gridx = 1;
        indirizzoField = new JTextField(30);
        formPanel.add(indirizzoField, gbc);
        
        // Telefono
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Phone:"), gbc);
        
        gbc.gridx = 1;
        telefonoField = new JTextField(20);
        formPanel.add(telefonoField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        emailField = new JTextField(30);
        formPanel.add(emailField, gbc);
        
        // PEC
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Certified Email:"), gbc);
        
        gbc.gridx = 1;
        pecField = new JTextField(30);
        formPanel.add(pecField, gbc);
        
        // Sito Web
        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(new JLabel("Website:"), gbc);
        
        gbc.gridx = 1;
        sitoWebField = new JTextField(30);
        formPanel.add(sitoWebField, gbc);
        
        // Note
        gbc.gridx = 0; gbc.gridy = 8;
        formPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        noteArea = new JTextArea(4, 30);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(noteArea), gbc);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> saveSupplier());
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout principale
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Aggiungi legenda campi obbligatori
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.add(new JLabel("* Required fields"));
        add(legendPanel, BorderLayout.NORTH);
    }
    
    private void loadSupplierData() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM fornitori WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, supplier.getId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        ragioneSocialeField.setText(rs.getString("ragione_sociale"));
                        partitaIvaField.setText(rs.getString("partita_iva"));
                        codiceFiscaleField.setText(rs.getString("codice_fiscale"));
                        indirizzoField.setText(rs.getString("indirizzo"));
                        telefonoField.setText(rs.getString("telefono"));
                        emailField.setText(rs.getString("email"));
                        pecField.setText(rs.getString("pec"));
                        sitoWebField.setText(rs.getString("sito_web"));
                        noteArea.setText(rs.getString("note"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading supplier data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveSupplier() {
        String ragioneSociale = ragioneSocialeField.getText().trim();
        String partitaIva = partitaIvaField.getText().trim();
        
        if (ragioneSociale.isEmpty() || partitaIva.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all required fields.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            if (supplier == null) {
                // Nuovo fornitore
                String insertQuery = """
                    INSERT INTO fornitori (ragione_sociale, partita_iva, codice_fiscale,
                                          indirizzo, telefono, email, pec, sito_web, note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, ragioneSociale);
                    pstmt.setString(2, partitaIva);
                    pstmt.setString(3, codiceFiscaleField.getText().trim());
                    pstmt.setString(4, indirizzoField.getText().trim());
                    pstmt.setString(5, telefonoField.getText().trim());
                    pstmt.setString(6, emailField.getText().trim());
                    pstmt.setString(7, pecField.getText().trim());
                    pstmt.setString(8, sitoWebField.getText().trim());
                    pstmt.setString(9, noteArea.getText().trim());
                    
                    pstmt.executeUpdate();
                    
                    // Recupera l'ID del nuovo fornitore
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            supplier = new Supplier(newId, ragioneSociale, partitaIva,
                                codiceFiscaleField.getText().trim(), indirizzoField.getText().trim(),
                                telefonoField.getText().trim(), emailField.getText().trim(),
                                pecField.getText().trim(), sitoWebField.getText().trim(),
                                noteArea.getText().trim());
                        }
                    }
                }
            } else {
                // Modifica fornitore esistente
                String updateQuery = """
                    UPDATE fornitori
                    SET ragione_sociale = ?, partita_iva = ?, codice_fiscale = ?,
                        indirizzo = ?, telefono = ?, email = ?, pec = ?,
                        sito_web = ?, note = ?
                    WHERE id = ?
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setString(1, ragioneSociale);
                    pstmt.setString(2, partitaIva);
                    pstmt.setString(3, codiceFiscaleField.getText().trim());
                    pstmt.setString(4, indirizzoField.getText().trim());
                    pstmt.setString(5, telefonoField.getText().trim());
                    pstmt.setString(6, emailField.getText().trim());
                    pstmt.setString(7, pecField.getText().trim());
                    pstmt.setString(8, sitoWebField.getText().trim());
                    pstmt.setString(9, noteArea.getText().trim());
                    pstmt.setInt(10, supplier.getId());
                    
                    pstmt.executeUpdate();
                }
            }
            
            supplierSaved = true;
            dispose();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error saving the supplier: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isSupplierSaved() {
        return supplierSaved;
    }
}