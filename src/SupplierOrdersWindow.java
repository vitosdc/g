// File: SupplierOrdersWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SupplierOrdersWindow extends JDialog {
    private int supplierId;
    private String supplierName;
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public SupplierOrdersWindow(JDialog parent, int supplierId, String supplierName) {
        super(parent, "Supplier Orders: " + supplierName, true);
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadOrders();
    }
    
    private void setupWindow() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Orders table
        String[] columns = {"Number", "Date", "Delivery Date", "Status", "Total €", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(tableModel);
        ordersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("New Order");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");
        
        addButton.addActionListener(e -> showOrderDialog(null));
        editButton.addActionListener(e -> editSelectedOrder());
        deleteButton.addActionListener(e -> deleteSelectedOrder());
        refreshButton.addActionListener(e -> loadOrders());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Main layout
        add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
 
    private void updateButtonStates() {
        boolean isRowSelected = ordersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void loadOrders() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT * FROM ordini_fornitori 
                WHERE fornitore_id = ?
                ORDER BY data_ordine DESC
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, supplierId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("numero"));
                    
                    // FIXED: Use DateUtils for proper date parsing
                    Date orderDate = DateUtils.parseDate(rs, "data_ordine");
                    if (orderDate != null) {
                        row.add(DateUtils.formatDate(orderDate, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    // FIXED: Use DateUtils for delivery date parsing
                    Date deliveryDate = DateUtils.parseDate(rs, "data_consegna_prevista");
                    if (deliveryDate != null) {
                        row.add(DateUtils.formatDate(deliveryDate, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("stato"));
                    row.add(String.format("%.2f", rs.getDouble("totale")));
                    row.add(rs.getString("note"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading orders: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showOrderDialog(SupplierOrder order) {
        SupplierOrderDialog dialog = new SupplierOrderDialog(this, supplierId, supplierName, order);
        dialog.setVisible(true);
        if (dialog.isOrderSaved()) {
            loadOrders();
        }
    }
    
    private void editSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow != -1) {
            String numero = (String)tableModel.getValueAt(selectedRow, 0);
            try {
                SupplierOrder order = loadOrderByNumber(numero);
                if (order != null) {
                    showOrderDialog(order);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error loading order: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private SupplierOrder loadOrderByNumber(String numero) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = "SELECT * FROM ordini_fornitori WHERE numero = ? AND fornitore_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, numero);
            pstmt.setInt(2, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // FIXED: Use DateUtils for proper date parsing
                    Date orderDate = DateUtils.parseDate(rs, "data_ordine");
                    Date deliveryDate = DateUtils.parseDate(rs, "data_consegna_prevista");
                    
                    // Fallback to current date if parsing fails
                    if (orderDate == null) {
                        orderDate = new Date();
                    }
                    
                    SupplierOrder order = new SupplierOrder(
                        rs.getInt("id"),
                        supplierId,
                        supplierName,
                        rs.getString("numero"),
                        orderDate,
                        deliveryDate, // Can be null
                        rs.getString("stato"),
                        rs.getDouble("totale"),
                        rs.getString("note")
                    );
                    
                    loadOrderItems(order);
                    return order;
                }
            }
        }
        return null;
    }
    
    private void loadOrderItems(SupplierOrder order) throws SQLException {
        String query = """
            SELECT i.*, p.nome as prodotto_nome, p.codice as prodotto_codice
            FROM dettagli_ordini_fornitori i
            JOIN prodotti p ON i.prodotto_id = p.id
            WHERE i.ordine_id = ?
        """;
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, order.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SupplierOrderItem item = new SupplierOrderItem(
                        rs.getInt("id"),
                        rs.getInt("ordine_id"),
                        rs.getInt("prodotto_id"),
                        rs.getString("prodotto_nome"),
                        rs.getString("prodotto_codice"),
                        rs.getInt("quantita"),
                        rs.getDouble("prezzo_unitario"),
                        rs.getDouble("totale"),
                        rs.getString("note")
                    );
                    order.getItems().add(item);
                }
            }
        }
    }
    
    private void deleteSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow != -1) {
            String numero = (String)tableModel.getValueAt(selectedRow, 0);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete order " + numero + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    conn.setAutoCommit(false);
                    
                    try {
                        // First delete order details
                        String deleteDetailsQuery = """
                            DELETE FROM dettagli_ordini_fornitori 
                            WHERE ordine_id = (
                                SELECT id FROM ordini_fornitori 
                                WHERE numero = ? AND fornitore_id = ?
                            )
                        """;
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                            pstmt.setString(1, numero);
                            pstmt.setInt(2, supplierId);
                            pstmt.executeUpdate();
                        }
                        
                        // Then delete the order
                        String deleteOrderQuery = "DELETE FROM ordini_fornitori WHERE numero = ? AND fornitore_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderQuery)) {
                            pstmt.setString(1, numero);
                            pstmt.setInt(2, supplierId);
                            pstmt.executeUpdate();
                        }
                        
                        conn.commit();
                        loadOrders();
                        
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Error deleting order: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}