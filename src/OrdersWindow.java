// File: OrdersWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OrdersWindow extends JDialog {
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public OrdersWindow(JFrame parent) {
        super(parent, "Orders Management", true);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        setupWindow();
        initComponents();
        loadOrders();
    }
    
    private void setupWindow() {
        setSize(1000, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchOrders());
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Orders table
        String[] columns = {"ID", "Customer", "Date", "Status", "Total €"};
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
        editButton = new JButton("Modify");
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
        add(searchPanel, BorderLayout.NORTH);
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
                SELECT o.*, c.nome || ' ' || c.cognome as cliente_nome
                FROM ordini o
                LEFT JOIN clienti c ON o.cliente_id = c.id
                ORDER BY o.data_ordine DESC
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("cliente_nome"));
                    
                    // FIXED: Use DateUtils for proper date parsing
                    Date date = DateUtils.parseDate(rs, "data_ordine");
                    if (date != null) {
                        row.add(DateUtils.formatDate(date, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("stato"));
                    row.add(String.format("%.2f", rs.getDouble("totale")));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while loading orders: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchOrders() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadOrders();
            return;
        }
        
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT o.*, c.nome || ' ' || c.cognome as cliente_nome
                FROM ordini o
                LEFT JOIN clienti c ON o.cliente_id = c.id
                WHERE c.nome LIKE ? OR c.cognome LIKE ? OR o.stato LIKE ?
                ORDER BY o.data_ordine DESC
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
                        row.add(rs.getString("cliente_nome"));
                        
                        // FIXED: Use DateUtils for proper date parsing
                        Date date = DateUtils.parseDate(rs, "data_ordine");
                        if (date != null) {
                            row.add(DateUtils.formatDate(date, dateFormat));
                        } else {
                            row.add("");
                        }
                        
                        row.add(rs.getString("stato"));
                        row.add(String.format("%.2f", rs.getDouble("totale")));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while searching orders: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showOrderDialog(Order order) {
        OrderDialog dialog = new OrderDialog(this, order);
        dialog.setVisible(true);
        if (dialog.isOrderSaved()) {
            loadOrders();
        }
    }
    
    private void editSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow != -1) {
            int orderId = (int)tableModel.getValueAt(selectedRow, 0);
            try {
                Order order = loadOrderDetails(orderId);
                if (order != null) {
                    showOrderDialog(order);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error while loading the order: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private Order loadOrderDetails(int orderId) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = """
            SELECT o.*, c.nome || ' ' || c.cognome as cliente_nome
            FROM ordini o
            LEFT JOIN clienti c ON o.cliente_id = c.id
            WHERE o.id = ?
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // FIXED: Use DateUtils for proper date parsing
                    Date date = DateUtils.parseDate(rs, "data_ordine");
                    if (date == null) {
                        date = new Date(); // Fallback to current date
                    }
                    
                    Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("cliente_id"),
                        rs.getString("cliente_nome"),
                        date,
                        rs.getString("stato"),
                        rs.getDouble("totale")
                    );
                    
                    // Load order details
                    loadOrderItems(order);
                    return order;
                }
            }
        }
        return null;
    }
    
    private void loadOrderItems(Order order) throws SQLException {
        String query = """
            SELECT i.*, p.nome as prodotto_nome
            FROM dettagli_ordine i
            LEFT JOIN prodotti p ON i.prodotto_id = p.id
            WHERE i.ordine_id = ?
        """;
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, order.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("ordine_id"),
                        rs.getInt("prodotto_id"),
                        rs.getString("prodotto_nome"),
                        rs.getInt("quantita"),
                        rs.getDouble("prezzo_unitario")
                    );
                    order.getItems().add(item);
                }
            }
        }
    }
    
    private void deleteSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow != -1) {
            int id = (int)tableModel.getValueAt(selectedRow, 0);
            String cliente = (String)tableModel.getValueAt(selectedRow, 1);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the order from customer '" + cliente + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    conn.setAutoCommit(false);
                    
                    try {
                        // First, delete the order details
                        String deleteDetailsQuery = "DELETE FROM dettagli_ordine WHERE ordine_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                            pstmt.setInt(1, id);
                            pstmt.executeUpdate();
                        }
                        
                        // Then, delete the order
                        String deleteOrderQuery = "DELETE FROM ordini WHERE id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderQuery)) {
                            pstmt.setInt(1, id);
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
                        "Error while deleting the order: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}