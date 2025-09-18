import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoicesPanel extends BasePanel {
    private JTable invoicesTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton printButton;
    private JButton refreshButton;
    private JButton advancedStatsButton;
    private SimpleDateFormat dateFormat;
    
    public InvoicesPanel(MainWindow parent) {
        super(parent, "Invoices Management");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        setupToolbar();
        setupMainContent();
        refreshData();
    }
    
    @Override
    protected void setupToolbar() {
        // Create action buttons
        addButton = createActionButton("New Invoice", "📋", new Color(220, 20, 60));
        editButton = createActionButton("Edit", "✏️", new Color(255, 140, 0));
        deleteButton = createActionButton("Delete", "🗑️", new Color(180, 50, 50));
        printButton = createActionButton("Print", "🖨️", new Color(70, 130, 180));
        advancedStatsButton = createActionButton("Advanced Stats", "📊", new Color(50, 205, 50));
        refreshButton = createActionButton("Refresh", "🔄", new Color(105, 105, 105));
        
        // Initially disable edit, delete and print buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        printButton.setEnabled(false);
        
        // Add action listeners
        addButton.addActionListener(e -> showInvoiceDialog(null));
        editButton.addActionListener(e -> editSelectedInvoice());
        deleteButton.addActionListener(e -> deleteSelectedInvoice());
        printButton.addActionListener(e -> printSelectedInvoice());
        advancedStatsButton.addActionListener(e -> showAdvancedStats());
        refreshButton.addActionListener(e -> refreshData());
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        
        // Add components to toolbar
        toolbarPanel.add(addButton);
        toolbarPanel.add(editButton);
        toolbarPanel.add(deleteButton);
        toolbarPanel.add(printButton);
        toolbarPanel.add(Box.createHorizontalStrut(10)); // Small spacer
        toolbarPanel.add(advancedStatsButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(searchPanel);
    }
    
    @Override
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        this.searchField = searchField;
        
        // Search functionality
        searchButton.addActionListener(e -> searchInvoices());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        
        // Search on Enter key
        searchField.addActionListener(e -> searchInvoices());
        
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
                searchTimer = new Timer(500, evt -> searchInvoices());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }
        });
    }
    
    @Override
    protected void setupMainContent() {
        // Create invoices table
        String[] columns = {"Number", "Date", "Customer", "Taxable Amount €", "VAT €", "Total €", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        invoicesTable = new JTable(tableModel);
        invoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for status column
        invoicesTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        
        // Add selection listener
        invoicesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Double-click to edit
        invoicesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedInvoice();
                }
            }
        });
        
        // Add table to content panel
        JScrollPane tableScrollPane = createStandardTable(invoicesTable);
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
        
        JLabel totalInvoicesLabel = new JLabel("Total Invoices: 0", SwingConstants.CENTER);
        JLabel totalValueLabel = new JLabel("Total Value: € 0.00", SwingConstants.CENTER);
        JLabel paidLabel = new JLabel("Paid: 0", SwingConstants.CENTER);
        JLabel pendingLabel = new JLabel("Pending: 0", SwingConstants.CENTER);
        
        Font statsFont = new Font("Arial", Font.BOLD, 12);
        totalInvoicesLabel.setFont(statsFont);
        totalValueLabel.setFont(statsFont);
        paidLabel.setFont(statsFont);
        pendingLabel.setFont(statsFont);
        
        totalInvoicesLabel.setForeground(new Color(220, 20, 60));
        totalValueLabel.setForeground(new Color(34, 139, 34));
        paidLabel.setForeground(new Color(50, 205, 50));
        pendingLabel.setForeground(new Color(255, 140, 0));
        
        statsPanel.add(totalInvoicesLabel);
        statsPanel.add(totalValueLabel);
        statsPanel.add(paidLabel);
        statsPanel.add(pendingLabel);
        
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
                    case "draft":
                        c.setBackground(new Color(240, 248, 255));
                        setForeground(new Color(0, 100, 200));
                        break;
                    case "issued":
                        c.setBackground(new Color(255, 248, 220));
                        setForeground(new Color(200, 120, 0));
                        break;
                    case "paid":
                        c.setBackground(new Color(240, 255, 240));
                        setForeground(new Color(0, 120, 0));
                        break;
                    case "canceled":
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
        loadInvoices();
    }
    
    private void loadInvoices() {
        tableModel.setRowCount(0);
        int totalInvoices = 0;
        double totalValue = 0;
        int paid = 0;
        int pending = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT f.*, c.nome || ' ' || c.cognome as cliente_nome
                FROM fatture f
                LEFT JOIN clienti c ON f.cliente_id = c.id
                ORDER BY f.data DESC
            """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("numero"));
                    
                    // Date formatting
                    Date date = DateUtils.parseDate(rs, "data");
                    if (date != null) {
                        row.add(DateUtils.formatDate(date, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("cliente_nome"));
                    row.add(String.format("%.2f", rs.getDouble("imponibile")));
                    row.add(String.format("%.2f", rs.getDouble("iva")));
                    
                    double total = rs.getDouble("totale");
                    row.add(String.format("%.2f", total));
                    
                    String status = rs.getString("stato");
                    row.add(status);
                    
                    tableModel.addRow(row);
                    totalInvoices++;
                    totalValue += total;
                    
                    // Count by status
                    if ("Paid".equalsIgnoreCase(status)) {
                        paid++;
                    } else if (!"Canceled".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error loading invoices: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalInvoices, totalValue, paid, pending);
    }
    
    private void searchInvoices() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshData();
            return;
        }
        
        tableModel.setRowCount(0);
        int totalInvoices = 0;
        double totalValue = 0;
        int paid = 0;
        int pending = 0;
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT f.*, c.nome || ' ' || c.cognome as cliente_nome
                FROM fatture f
                LEFT JOIN clienti c ON f.cliente_id = c.id
                WHERE f.numero LIKE ? OR c.nome LIKE ? OR c.cognome LIKE ?
                ORDER BY f.data DESC
            """;
            
            String searchPattern = "%" + searchTerm + "%";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getString("numero"));
                        
                        Date date = DateUtils.parseDate(rs, "data");
                        if (date != null) {
                            row.add(DateUtils.formatDate(date, dateFormat));
                        } else {
                            row.add("");
                        }
                        
                        row.add(rs.getString("cliente_nome"));
                        row.add(String.format("%.2f", rs.getDouble("imponibile")));
                        row.add(String.format("%.2f", rs.getDouble("iva")));
                        
                        double total = rs.getDouble("totale");
                        row.add(String.format("%.2f", total));
                        
                        String status = rs.getString("stato");
                        row.add(status);
                        
                        tableModel.addRow(row);
                        totalInvoices++;
                        totalValue += total;
                        
                        if ("Paid".equalsIgnoreCase(status)) {
                            paid++;
                        } else if (!"Canceled".equalsIgnoreCase(status)) {
                            pending++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Error searching invoices: " + e.getMessage());
            e.printStackTrace();
        }
        
        updateStatsPanel(totalInvoices, totalValue, paid, pending);
    }
    
    private void updateStatsPanel(int totalInvoices, double totalValue, int paid, int pending) {
        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getComponentCount() == 4) { // Stats panel has 4 labels
                    ((JLabel) panel.getComponent(0)).setText("Total Invoices: " + totalInvoices);
                    ((JLabel) panel.getComponent(1)).setText(String.format("Total Value: € %.2f", totalValue));
                    ((JLabel) panel.getComponent(2)).setText("Paid: " + paid);
                    ((JLabel) panel.getComponent(3)).setText("Pending: " + pending);
                    break;
                }
            }
        }
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = invoicesTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        printButton.setEnabled(isRowSelected);
    }
    
    private void showInvoiceDialog(Invoice invoice) {
        InvoiceDialog dialog = new InvoiceDialog(parentWindow, invoice);
        dialog.setVisible(true);
        if (dialog.isInvoiceSaved()) {
            refreshData();
            showSuccessMessage("Invoice " + (invoice == null ? "created" : "updated") + " successfully!");
        }
    }
    
    private void editSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            String numero = (String)tableModel.getValueAt(selectedRow, 0);
            try {
                Invoice invoice = loadInvoiceByNumber(numero);
                if (invoice != null) {
                    showInvoiceDialog(invoice);
                }
            } catch (SQLException e) {
                showErrorMessage("Error loading invoice: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private Invoice loadInvoiceByNumber(String numero) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = """
            SELECT f.*, c.nome || ' ' || c.cognome as cliente_nome
            FROM fatture f
            LEFT JOIN clienti c ON f.cliente_id = c.id
            WHERE f.numero = ?
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, numero);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Date date = DateUtils.parseDate(rs, "data");
                    if (date == null) {
                        date = new Date();
                    }
                    
                    Invoice invoice = new Invoice(
                        rs.getInt("id"),
                        rs.getString("numero"),
                        date,
                        rs.getInt("cliente_id"),
                        rs.getString("cliente_nome"),
                        rs.getDouble("imponibile"),
                        rs.getDouble("iva"),
                        rs.getDouble("totale"),
                        rs.getString("stato")
                    );
                    
                    loadInvoiceItems(invoice);
                    return invoice;
                }
            }
        }
        return null;
    }
    
    private void loadInvoiceItems(Invoice invoice) throws SQLException {
        String query = """
            SELECT i.*, p.nome as prodotto_nome, p.codice as prodotto_codice
            FROM dettagli_fattura i
            LEFT JOIN prodotti p ON i.prodotto_id = p.id
            WHERE i.fattura_id = ?
        """;
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, invoice.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem(
                        rs.getInt("id"),
                        rs.getInt("fattura_id"),
                        rs.getInt("prodotto_id"),
                        rs.getString("prodotto_nome"),
                        rs.getString("prodotto_codice"),
                        rs.getInt("quantita"),
                        rs.getDouble("prezzo_unitario"),
                        rs.getDouble("aliquota_iva"),
                        rs.getDouble("totale")
                    );
                    invoice.getItems().add(item);
                }
            }
        }
    }
    
    private void deleteSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            String numero = (String)tableModel.getValueAt(selectedRow, 0);
            String cliente = (String)tableModel.getValueAt(selectedRow, 2);
            
            if (!showConfirmDialog(
                "Are you sure you want to delete invoice " + numero + " from customer " + cliente + "?\n" +
                "This action cannot be undone.", 
                "Confirm Deletion")) {
                return;
            }
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                conn.setAutoCommit(false);
                
                try {
                    // First, delete the invoice details
                    String deleteDetailsQuery = "DELETE FROM dettagli_fattura WHERE fattura_id = (SELECT id FROM fatture WHERE numero = ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                        pstmt.setString(1, numero);
                        pstmt.executeUpdate();
                    }
                    
                    // Then, delete the invoice
                    String deleteInvoiceQuery = "DELETE FROM fatture WHERE numero = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteInvoiceQuery)) {
                        pstmt.setString(1, numero);
                        pstmt.executeUpdate();
                    }
                    
                    conn.commit();
                    refreshData();
                    showSuccessMessage("Invoice deleted successfully");
                    
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
                
            } catch (SQLException e) {
                showErrorMessage("Error deleting invoice: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void printSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            String numero = (String)tableModel.getValueAt(selectedRow, 0);
            try {
                Invoice invoice = loadInvoiceByNumber(numero);
                if (invoice != null) {
                    InvoicePrinter printer = new InvoicePrinter(invoice);
                    printer.print();
                }
            } catch (SQLException e) {
                showErrorMessage("Error loading invoice for printing: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void showAdvancedStats() {
        AdvancedStatsWindow statsWindow = new AdvancedStatsWindow(parentWindow);
        statsWindow.setVisible(true);
    }
}