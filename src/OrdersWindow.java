// File: OrdersWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;

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
        super(parent, "Gestione Ordini", true);
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
        // Pannello di ricerca
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Cerca");
        searchButton.addActionListener(e -> searchOrders());
        searchPanel.add(new JLabel("Cerca: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Tabella ordini
        String[] columns = {"ID", "Cliente", "Data", "Stato", "Totale €"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(tableModel);
        ordersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Nuovo Ordine");
        editButton = new JButton("Modifica");
        deleteButton = new JButton("Elimina");
        refreshButton = new JButton("Aggiorna");
        
        addButton.addActionListener(e -> showOrderDialog(null));
        editButton.addActionListener(e -> editSelectedOrder());
        deleteButton.addActionListener(e -> deleteSelectedOrder());
        refreshButton.addActionListener(e -> loadOrders());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Layout principale
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
                    // Formatta la data
                    java.sql.Date sqlDate = rs.getDate("data_ordine");
                    row.add(dateFormat.format(sqlDate));
                    row.add(rs.getString("stato"));
                    row.add(String.format("%.2f", rs.getDouble("totale")));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento degli ordini: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
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
                        java.sql.Date sqlDate = rs.getDate("data_ordine");
                        row.add(dateFormat.format(sqlDate));
                        row.add(rs.getString("stato"));
                        row.add(String.format("%.2f", rs.getDouble("totale")));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la ricerca degli ordini: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
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
                    "Errore durante il caricamento dell'ordine: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
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
                    Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("cliente_id"),
                        rs.getString("cliente_nome"),
                        rs.getDate("data_ordine"),
                        rs.getString("stato"),
                        rs.getDouble("totale")
                    );
                    
                    // Carica i dettagli dell'ordine
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
                "Sei sicuro di voler eliminare l'ordine del cliente '" + cliente + "'?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    // Prima elimina i dettagli dell'ordine
                    String deleteDetailsQuery = "DELETE FROM dettagli_ordine WHERE ordine_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                    }
                    
                    // Poi elimina l'ordine
                    String deleteOrderQuery = "DELETE FROM ordini WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderQuery)) {
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                    }
                    
                    loadOrders();
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Errore durante l'eliminazione dell'ordine: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
