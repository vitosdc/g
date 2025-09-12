// File: ProductsWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class ProductsWindow extends JDialog {
    private JTable productsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    
    public ProductsWindow(JFrame parent) {
        super(parent, "Product Management", true);
        setupWindow();
        initComponents();
        loadProducts();
    }
    
    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchProducts());
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Products table
        String[] columns = {"ID", "Code", "Name", "Description", "Price", "Quantity"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(tableModel);
        productsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productsTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");
        
        addButton.addActionListener(e -> showProductDialog(null));
        editButton.addActionListener(e -> editSelectedProduct());
        deleteButton.addActionListener(e -> deleteSelectedProduct());
        refreshButton.addActionListener(e -> loadProducts());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Main layout
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(productsTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = productsTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void loadProducts() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT * FROM prodotti ORDER BY nome";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("codice"));
                    row.add(rs.getString("nome"));
                    row.add(rs.getString("descrizione"));
                    row.add(rs.getDouble("prezzo"));
                    row.add(rs.getInt("quantita"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchProducts() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadProducts();
            return;
        }
        
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT * FROM prodotti 
                WHERE codice LIKE ? OR nome LIKE ? OR descrizione LIKE ?
                ORDER BY nome
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
                        row.add(rs.getString("codice"));
                        row.add(rs.getString("nome"));
                        row.add(rs.getString("descrizione"));
                        row.add(rs.getDouble("prezzo"));
                        row.add(rs.getInt("quantita"));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error searching for products: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showProductDialog(Product product) {
        ProductDialog dialog = new ProductDialog(this, product);
        dialog.setVisible(true);
        if (dialog.isProductSaved()) {
            loadProducts();
        }
    }
    
    private void editSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            Product product = new Product(
                (int)tableModel.getValueAt(selectedRow, 0),
                (String)tableModel.getValueAt(selectedRow, 1),
                (String)tableModel.getValueAt(selectedRow, 2),
                (String)tableModel.getValueAt(selectedRow, 3),
                (double)tableModel.getValueAt(selectedRow, 4),
                (int)tableModel.getValueAt(selectedRow, 5)
            );
            showProductDialog(product);
        }
    }
    
    private void deleteSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String nome = (String)tableModel.getValueAt(selectedRow, 2);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the product '" + nome + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    String query = "DELETE FROM prodotti WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        loadProducts();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this,
                        "Error deleting the product: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}