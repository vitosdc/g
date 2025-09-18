import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OrdersPanel extends BasePanel {
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public OrdersPanel(MainWindow parent) {
        super(parent, "Orders Management");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        addButton = createActionButton("New Order", "📝", new Color(34, 139, 34));
        editButton = createActionButton("Edit", "✏️", new Color(255, 140, 0));
        deleteButton = createActionButton("Delete", "🗑️", new Color(220, 20, 60));
        refreshButton = createActionButton("Refresh", "🔄", new Color(70, 130, 180));
        
        // Initially disable edit and delete buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        // Add action listeners
        addButton.addActionListener(e -> showOrderDialog(null));
        editButton.addActionListener(e -> editSelectedOrder());
        deleteButton.addActionListener(e -> deleteSelectedOrder());
        refreshButton.addActionListener(e -> refreshData());
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        
        // Add components to toolbar
        toolbarPanel.add(addButton);
        toolbarPanel.add(editButton);
        toolbarPanel.add(deleteButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(searchPanel);
    }
    
    @Override
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        this.searchField = searchField;
        
        // Search functionality
        searchButton.addActionListener(e -> searchOrders());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> searchOrders());
        
        // Real-time search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private Timer searchTimer;
            
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            
            private void scheduleSearch() {
                if (searchTimer != null) searchTimer.stop();
                searchTimer = new Timer(500, evt -> searchOrders());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create orders table
        String[] columns = {"ID", "Customer", "Date", "Status", "Total €"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        ordersTable = new JTable(tableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for status column
        ordersTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        
        // Add selection listener
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to edit
        ordersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedOrder();
                }
            }
        });
        
        // Add table to content panel
        JScrollPane tableScrollPane = createStandardTable(ordersTable);
        contentPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Statistics panel
        JPanel statsPanel = createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        statsPanel.setBackground(new Color(248, 248, 252));
        
        JLabel totalOrdersLabel = new JLabel("Total Orders: 0", SwingConstants.CENTER);
        JLabel totalValueLabel = new JLabel("Total Value: € 0.00", SwingConstants.CENTER);
        JLabel pendingLabel = new JLabel("Pending: 0", SwingConstants.CENTER);
        JLabel completedLabel = new JLabel("Completed: 0", SwingConstants.CENTER);
        
        Font statsFont = new Font("Arial", Font.BOLD, 12);
        totalOrdersLabel.setFont(statsFont);
        totalValueLabel.setFont(statsFont);
        pendingLabel.setFont(statsFont);
        completedLabel.setFont(statsFont);
        
        totalOrdersLabel.setForeground(new Color(70, 130, 180));
        totalValueLabel.setForeground(new Color(34, 139, 34));
        pendingLabel.setForeground(new Color(255, 140, 0));
        completedLabel.setForeground(new Color(50, 205, 50));
        
        statsPanel.add(totalOrdersLabel);
        statsPanel.add(totalValueLabel);
        statsPanel.add(pendingLabel);
        statsPanel.add(completedLabel);
        
        return statsPanel;
    }
    
    // Custom cell renderer for status column
    private class StatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String status = value.toString();
                switch (status.toLowerCase()) {
                    case "new":
                        c.setBackground(new Color(240, 248, 255));
                        setForeground(new Color(0, 100, 200));
                        break;
                    case "in progress":
                        c.setBackground(new Color(255, 248, 220));
                        setForeground(new Color(200, 120, 0));
                        break;
                    case "completed":
                        c.setBackground(new Color(240, 255, 240));
                        setForeground(new Color(0, 120, 0));
                        break;
                    case "cancelled":
                        c.setBackground(new Color(255, 240, 240));
                        setForeground(new Color(180, 50, 50));
                        break;
                    default:
                        if (row % 2 == 0) {
                            c.setBackground(Color.WHITE);
                        } else {
                            c.setBackground(new Color(248, 248, 252));
                        }
                        setForeground(Color.BLACK);
                }
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            
            return c;
        }
    }
    
    @Override
    public void refreshData() {
        loadOrders();
    }
    
    private void loadOrders() {
        tableModel.setRowCount(0);
        int totalOrders = 0;
        double totalValue = 0;
        int pending = 0;
        int completed = 0;
        
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
                    
                    // Date formatting
                    Date date = DateUtils.parseDate(rs, "data_ordine");
                    if (date != null) {
                        row.add(DateUtils.formatDate(date, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    String status = rs.getString("stato");
                    row.add(status);
                    
                    double total = rs.getDouble("totale");
                    row.add(String.format("%.2f", total));
                    
                    tableModel.addRow(row);
                    totalOrders++;
                    totalValue += total;
                    
                    // Count by status
                    if ("Completed".equalsIgnoreCase(status)) {
                        completed++;
                    } else if (!"Cancelled".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading orders: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalOrders, totalValue, pending, completed);
    }
    
    private void searchOrders() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshData();
            return;
        }
        
        tableModel.setRowCount(0);
        int totalOrders = 0;
        double totalValue = 0;
        int pending = 0;
        int completed = 0;
        
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
                        
                        Date date = DateUtils.parseDate(rs, "data_ordine");
                        if (date != null) {
                            row.add(DateUtils.formatDate(date, dateFormat));
                        } else {
                            row.add("");
                        }
                        
                        String status = rs.getString("stato");
                        row.add(status);
                        
                        double total = rs.getDouble("totale");
                        row.add(String.format("%.2f", total));
                        
                        tableModel.addRow(row);
                        totalOrders++;
                        totalValue += total;
                        
                        if ("Completed".equalsIgnoreCase(status)) {
                            completed++;
                        } else if (!"Cancelled".equalsIgnoreCase(status)) {
                            pending++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching orders: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalOrders, totalValue, pending, completed);
    }
    
    private void updateStatsPanel(int totalOrders, double totalValue, int pending, int completed) {
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() == 4) { // Stats panel has 4 labels
                    ((JLabel) panel.getComponent(0)).setText("Total Orders: " + totalOrders);
                    ((JLabel) panel.getComponent(1)).setText(String.format("Total Value: € %.2f", totalValue));
                    ((JLabel) panel.getComponent(2)).setText("Pending: " + pending);
                    ((JLabel) panel.getComponent(3)).setText("Completed: " + completed);
                    break;
                }
            }
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = ordersTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void showOrderDialog(Order order) {
        OrderDialog dialog = new OrderDialog(parentWindow, order);
        dialog.setVisible(true);
        if (dialog.isOrderSaved()) {
            refreshData();
            showSuccessMessage("Order " + (order == null ? "created" : "updated") + " successfully!");
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
                showErrorMessage("Error loading order: " + e.getMessage());
                e.printStackTrace();
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
                    Date date = DateUtils.parseDate(rs, "data_ordine");
                    if (date == null) {
                        date = new Date();
                    }
                    
                    Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("cliente_id"),
                        rs.getString("cliente_nome"),
                        date,
                        rs.getString("stato"),
                        rs.getDouble("totale")
                    );
                    
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
            
            if (!showConfirmDialog(
                "Are you sure you want to delete the order from customer '" + cliente + "'?\n" +
                "This action cannot be undone.", 
                "Confirm Deletion")) {
                return;
            }
            
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
                    refreshData();
                    showSuccessMessage("Order deleted successfully");
                    
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                showErrorMessage("Error deleting order: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}