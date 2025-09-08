
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class CustomersWindow extends JDialog {
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    
    public CustomersWindow(JFrame parent) {
        super(parent, "Gestione Clienti", true);
        setupWindow();
        initComponents();
        loadCustomers();
    }
    
    private void setupWindow() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Pannello di ricerca
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Cerca");
        searchButton.addActionListener(e -> searchCustomers());
        searchPanel.add(new JLabel("Cerca: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Tabella clienti
        String[] columns = {"ID", "Nome", "Cognome", "Email", "Telefono", "Indirizzo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customersTable = new JTable(tableModel);
        customersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Aggiungi Cliente");
        editButton = new JButton("Modifica");
        deleteButton = new JButton("Elimina");
        refreshButton = new JButton("Aggiorna");
        
        addButton.addActionListener(e -> showCustomerDialog(null));
        editButton.addActionListener(e -> editSelectedCustomer());
        deleteButton.addActionListener(e -> deleteSelectedCustomer());
        refreshButton.addActionListener(e -> loadCustomers());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Layout principale
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(customersTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = customersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void loadCustomers() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM clienti ORDER BY cognome, nome";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("nome"));
                    row.add(rs.getString("cognome"));
                    row.add(rs.getString("email"));
                    row.add(rs.getString("telefono"));
                    row.add(rs.getString("indirizzo"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei clienti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchCustomers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadCustomers();
            return;
        }
        
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT * FROM clienti 
                WHERE nome LIKE ? OR cognome LIKE ? OR email LIKE ? OR telefono LIKE ?
                ORDER BY cognome, nome
            """;
            String searchPattern = "%" + searchTerm + "%";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getInt("id"));
                        row.add(rs.getString("nome"));
                        row.add(rs.getString("cognome"));
                        row.add(rs.getString("email"));
                        row.add(rs.getString("telefono"));
                        row.add(rs.getString("indirizzo"));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Errore durante la ricerca dei clienti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showCustomerDialog(Customer customer) {
        CustomerDialog dialog = new CustomerDialog(this, customer);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            loadCustomers();
        }
    }
    
    private void editSelectedCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow != -1) {
            Customer customer = new Customer(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                (String)tableModel.getValueAt(selectedRow, 3),
                (String)tableModel.getValueAt(selectedRow, 4),
                (String)tableModel.getValueAt(selectedRow, 5)
            );
            showCustomerDialog(customer);
        }
    }
    
    private void deleteSelectedCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 1);
            String cognome = (String)tableModel.getValueAt(selectedRow, 2);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler eliminare il cliente '" + nome + " " + cognome + "'?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    String query = "DELETE FROM clienti WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        loadCustomers();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this,
                        "Errore durante l'eliminazione del cliente: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}