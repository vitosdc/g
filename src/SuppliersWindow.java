import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class SuppliersWindow extends JDialog {
    private JTable suppliersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton ordersButton;
    private JButton priceListButton;
    private JButton refreshButton;
    
    public SuppliersWindow(JFrame parent) {
        super(parent, "Gestione Fornitori", true);
        setupWindow();
        initComponents();
        loadSuppliers();
    }
    
    private void setupWindow() {
        setSize(1000, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Pannello di ricerca
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Cerca");
        searchButton.addActionListener(e -> searchSuppliers());
        searchPanel.add(new JLabel("Cerca: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Tabella fornitori
        String[] columns = {"ID", "Ragione Sociale", "P.IVA", "Email", "Telefono", "Indirizzo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        suppliersTable = new JTable(tableModel);
        suppliersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suppliersTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Nuovo Fornitore");
        editButton = new JButton("Modifica");
        deleteButton = new JButton("Elimina");
        ordersButton = new JButton("Ordini");
        priceListButton = new JButton("Listino Prezzi");
        refreshButton = new JButton("Aggiorna");
        
        addButton.addActionListener(e -> showSupplierDialog(null));
        editButton.addActionListener(e -> editSelectedSupplier());
        deleteButton.addActionListener(e -> deleteSelectedSupplier());
        ordersButton.addActionListener(e -> showSupplierOrders());
        priceListButton.addActionListener(e -> showSupplierPriceList());
        refreshButton.addActionListener(e -> loadSuppliers());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(ordersButton);
        buttonPanel.add(priceListButton);
        buttonPanel.add(refreshButton);
        
        // Layout principale
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(suppliersTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = suppliersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        ordersButton.setEnabled(isRowSelected);
        priceListButton.setEnabled(isRowSelected);
    }
    
    private void loadSuppliers() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM fornitori ORDER BY ragione_sociale";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("ragione_sociale"));
                    row.add(rs.getString("partita_iva"));
                    row.add(rs.getString("email"));
                    row.add(rs.getString("telefono"));
                    row.add(rs.getString("indirizzo"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei fornitori: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchSuppliers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadSuppliers();
            return;
        }
        
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT * FROM fornitori 
                WHERE ragione_sociale LIKE ? 
                   OR partita_iva LIKE ? 
                   OR email LIKE ?
                ORDER BY ragione_sociale
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getInt("id"));
                        row.add(rs.getString("ragione_sociale"));
                        row.add(rs.getString("partita_iva"));
                        row.add(rs.getString("email"));
                        row.add(rs.getString("telefono"));
                        row.add(rs.getString("indirizzo"));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la ricerca dei fornitori: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showSupplierDialog(Supplier supplier) {
        SupplierDialog dialog = new SupplierDialog(this, supplier);
        dialog.setVisible(true);
        if (dialog.isSupplierSaved()) {
            loadSuppliers();
        }
    }
    
    private void editSelectedSupplier() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            Supplier supplier = new Supplier(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                "", // codice fiscale
                (String)tableModel.getValueAt(selectedRow, 5), // indirizzo
                (String)tableModel.getValueAt(selectedRow, 4), // telefono
                (String)tableModel.getValueAt(selectedRow, 3), // email
                "", // pec
                "", // sito web
                ""  // note
            );
            showSupplierDialog(supplier);
        }
    }
    
    private void deleteSelectedSupplier() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 1);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler eliminare il fornitore '" + nome + "'?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    String query = "DELETE FROM fornitori WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        loadSuppliers();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Errore durante l'eliminazione del fornitore: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void showSupplierOrders() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int supplierId = (int)tableModel.getValueAt(selectedRow, 0);
            String supplierName = (String)tableModel.getValueAt(selectedRow, 1);
            SupplierOrdersWindow ordersWindow = new SupplierOrdersWindow(this, supplierId, supplierName);
            ordersWindow.setVisible(true);
        }
    }
    
    private void showSupplierPriceList() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow != -1) {
            int supplierId = (int)tableModel.getValueAt(selectedRow, 0);
            String supplierName = (String)tableModel.getValueAt(selectedRow, 1);
            SupplierPriceListWindow priceListWindow = new SupplierPriceListWindow(this, supplierId, supplierName);
            priceListWindow.setVisible(true);
        }
    }
}