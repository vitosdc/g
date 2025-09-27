import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class CustomerSelectionDialog extends JDialog {
    private JTextField searchField;
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private Customer selectedCustomer;
    private boolean customerSelected = false;
    
    public CustomerSelectionDialog(JDialog parent) {
        super(parent, "Select Customer", true);
        
        setupWindow();
        initComponents();
        loadAllCustomers();
    }
    
    private void setupWindow() {
        setSize(700, 500);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Panel principale con padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Panel di ricerca migliorato
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Customer"));
        
        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(30);
        searchField.setToolTipText("Search by name, surname, email, phone or address");
        
        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");
        JButton newCustomerButton = new JButton("New Customer");
        
        // Ricerca in tempo reale mentre si digita
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
        });
        
        // Enter key per ricerca
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });
        
        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadAllCustomers();
        });
        newCustomerButton.addActionListener(e -> createNewCustomer());
        
        searchInputPanel.add(new JLabel("Search:"));
        searchInputPanel.add(searchField);
        searchInputPanel.add(searchButton);
        searchInputPanel.add(clearButton);
        searchInputPanel.add(newCustomerButton);
        
        searchPanel.add(searchInputPanel, BorderLayout.CENTER);
        
        // Tabella clienti con colonne dettagliate
        String[] columns = {"ID", "Name", "Surname", "Email", "Phone", "City", "Full Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customersTable = new JTable(tableModel);
        
        // Nascondi colonna ID ma mantienila per i dati
        customersTable.getColumnModel().getColumn(0).setMinWidth(0);
        customersTable.getColumnModel().getColumn(0).setMaxWidth(0);
        customersTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        
        // Imposta larghezza colonne
        customersTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Name
        customersTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Surname
        customersTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Email
        customersTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Phone
        customersTable.getColumnModel().getColumn(5).setPreferredWidth(100); // City
        customersTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Full Address
        
        // Double-click per selezione
        customersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCustomer();
                }
            }
        });
        
        // Selezione con Enter
        customersTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectCustomer();
                }
            }
        });
        
        JScrollPane tableScrollPane = new JScrollPane(customersTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Customers (Double-click to select)"));
        
        // Panel pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton selectButton = new JButton("Select");
        JButton cancelButton = new JButton("Cancel");
        
        selectButton.addActionListener(e -> selectCustomer());
        cancelButton.addActionListener(e -> dispose());
        
        // Stile pulsanti
        selectButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        selectButton.setFont(selectButton.getFont().deriveFont(Font.BOLD));
        
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        
        // Assemblaggio
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Focus iniziale sul campo di ricerca
        SwingUtilities.invokeLater(() -> searchField.requestFocus());
    }
    
    private void loadAllCustomers() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT id, nome, cognome, email, telefono, indirizzo
                FROM clienti 
                ORDER BY cognome, nome
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    addCustomerRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading customers: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadAllCustomers();
            return;
        }
        
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT id, nome, cognome, email, telefono, indirizzo
                FROM clienti 
                WHERE LOWER(nome) LIKE LOWER(?) 
                   OR LOWER(cognome) LIKE LOWER(?) 
                   OR LOWER(email) LIKE LOWER(?)
                   OR telefono LIKE ?
                   OR LOWER(indirizzo) LIKE LOWER(?)
                ORDER BY 
                    CASE 
                        WHEN LOWER(cognome) LIKE LOWER(?) THEN 1
                        WHEN LOWER(nome) LIKE LOWER(?) THEN 2
                        ELSE 3
                    END,
                    cognome, nome
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            String exactPattern = searchTerm + "%";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
                pstmt.setString(5, searchPattern);
                pstmt.setString(6, exactPattern); // Per ordinamento
                pstmt.setString(7, exactPattern); // Per ordinamento
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        addCustomerRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error searching customers: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addCustomerRow(ResultSet rs) throws SQLException {
        Vector<Object> row = new Vector<>();
        row.add(rs.getInt("id")); // ID nascosto
        row.add(rs.getString("nome"));
        row.add(rs.getString("cognome"));
        row.add(rs.getString("email"));
        row.add(rs.getString("telefono"));
        
        // Estrai la città dall'indirizzo (prendi l'ultima parte dopo la virgola)
        String fullAddress = rs.getString("indirizzo");
        String city = "";
        if (fullAddress != null && !fullAddress.isEmpty()) {
            String[] parts = fullAddress.split(",");
            if (parts.length > 1) {
                city = parts[parts.length - 1].trim();
            }
        }
        
        row.add(city);
        row.add(fullAddress);
        tableModel.addRow(row);
    }
    
    private void selectCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedCustomer = new Customer(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                (String)tableModel.getValueAt(selectedRow, 3),
                (String)tableModel.getValueAt(selectedRow, 4),
                (String)tableModel.getValueAt(selectedRow, 6)
            );
            customerSelected = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select a customer from the list",
                "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void createNewCustomer() {
        CustomerDialog dialog = new CustomerDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isCustomerSaved()) {
            // Ricarica la lista e seleziona il nuovo cliente
            loadAllCustomers();
            // Se possibile, trova e seleziona il cliente appena creato
            // (questo richiederebbe di ritornare l'ID del nuovo cliente dal dialog)
        }
    }
    
    public Customer getSelectedCustomer() {
        return selectedCustomer;
    }
    
    public boolean isCustomerSelected() {
        return customerSelected;
    }
}