
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class CustomerDialog extends JDialog {
    private JTextField nomeField;
    private JTextField cognomeField;
    private JTextField emailField;
    private JTextField telefonoField;
    private JTextArea indirizzoArea;
    private boolean customerSaved = false;
    private Customer customer;
    
    public CustomerDialog(JDialog parent, Customer customer) {
        super(parent, customer == null ? "Nuovo Cliente" : "Modifica Cliente", true);
        this.customer = customer;
        
        setupDialog();
        initComponents();
        if (customer != null) {
            loadCustomerData();
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
        
        // Nome
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Nome:"), gbc);
        
        gbc.gridx = 1;
        nomeField = new JTextField(20);
        formPanel.add(nomeField, gbc);
        
        // Cognome
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Cognome:"), gbc);
        
        gbc.gridx = 1;
        cognomeField = new JTextField(20);
        formPanel.add(cognomeField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        emailField = new JTextField(20);
        formPanel.add(emailField, gbc);
        
        // Telefono
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Telefono:"), gbc);
        
        gbc.gridx = 1;
        telefonoField = new JTextField(20);
        formPanel.add(telefonoField, gbc);
        
        // Indirizzo
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Indirizzo:"), gbc);
        
        gbc.gridx = 1;
        indirizzoArea = new JTextArea(4, 20);
        indirizzoArea.setLineWrap(true);
        indirizzoArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(indirizzoArea), gbc);
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Salva");
        JButton cancelButton = new JButton("Annulla");
        
        saveButton.addActionListener(e -> saveCustomer());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Layout principale
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadCustomerData() {
        nomeField.setText(customer.getNome());
        cognomeField.setText(customer.getCognome());
        emailField.setText(customer.getEmail());
        telefonoField.setText(customer.getTelefono());
        indirizzoArea.setText(customer.getIndirizzo());
    }
    
    private void saveCustomer() {
        try {
            // Validazione
            String nome = nomeField.getText().trim();
            String cognome = cognomeField.getText().trim();
            String email = emailField.getText().trim();
            String telefono = telefonoField.getText().trim();
            String indirizzo = indirizzoArea.getText().trim();
            
            if (nome.isEmpty() || cognome.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "I campi Nome e Cognome sono obbligatori",
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            if (customer == null) { // Nuovo cliente
                String query = """
                    INSERT INTO clienti (nome, cognome, email, telefono, indirizzo)
                    VALUES (?, ?, ?, ?, ?)
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, nome);
                    pstmt.setString(2, cognome);
                    pstmt.setString(3, email);
                    pstmt.setString(4, telefono);
                    pstmt.setString(5, indirizzo);
                    pstmt.executeUpdate();
                }
            } else { // Modifica cliente
                String query = """
                    UPDATE clienti
                    SET nome = ?, cognome = ?, email = ?, telefono = ?, indirizzo = ?
                    WHERE id = ?
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, nome);
                    pstmt.setString(2, cognome);
                    pstmt.setString(3, email);
                    pstmt.setString(4, telefono);
                    pstmt.setString(5, indirizzo);
                    pstmt.setInt(6, customer.getId());
                    pstmt.executeUpdate();
                }
            }
            
            customerSaved = true;
            dispose();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Errore durante il salvataggio del cliente: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isCustomerSaved() {
        return customerSaved;
    }
}
