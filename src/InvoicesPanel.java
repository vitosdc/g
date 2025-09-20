import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoicesPanel extends JPanel {
    private JTable invoicesTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton printButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public InvoicesPanel() {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        setupPanel();
        initComponents();
        loadInvoices();
    }
    
    private void setupPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void initComponents() {
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Invoices"));
        
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchInvoices());
        
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Invoices table
        String[] columns = {"Number", "Date", "Customer", "Taxable Amount", "VAT", "Total", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        invoicesTable = new JTable(tableModel);
        invoicesTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        addButton = new JButton("New Invoice");
        editButton = new JButton("Modify");
        deleteButton = new JButton("Delete");
        printButton = new JButton("Print");
        refreshButton = new JButton("Refresh");
        
        addButton.addActionListener(e -> createNewInvoice());
        editButton.addActionListener(e -> editSelectedInvoice());
        deleteButton.addActionListener(e -> deleteSelectedInvoice());
        printButton.addActionListener(e -> printSelectedInvoice());
        refreshButton.addActionListener(e -> loadInvoices());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(printButton);
        buttonPanel.add(refreshButton);
        
        // Main layout
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(invoicesTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = invoicesTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
        printButton.setEnabled(isRowSelected);
    }
    
    private void loadInvoices() {
        tableModel.setRowCount(0);
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
                    
                    Date date = DateUtils.parseDate(rs, "data");
                    if (date != null) {
                        row.add(DateUtils.formatDate(date, dateFormat));
                    } else {
                        row.add("");
                    }
                    
                    row.add(rs.getString("cliente_nome"));
                    row.add(String.format("%.2f €", rs.getDouble("imponibile")));
                    row.add(String.format("%.2f €", rs.getDouble("iva")));
                    row.add(String.format("%.2f €", rs.getDouble("totale")));
                    row.add(rs.getString("stato"));
                    tableModel.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while loading invoices: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchInvoices() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadInvoices();
            return;
        }
        
        tableModel.setRowCount(0);
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
                        row.add(String.format("%.2f €", rs.getDouble("imponibile")));
                        row.add(String.format("%.2f €", rs.getDouble("iva")));
                        row.add(String.format("%.2f €", rs.getDouble("totale")));
                        row.add(rs.getString("stato"));
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while searching for invoices: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createNewInvoice() {
        try {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            
            InvoiceDialog dialog;
            if (parentWindow instanceof JFrame) {
                dialog = new InvoiceDialog((JFrame) parentWindow, null);
            } else {
                dialog = new InvoiceDialog((JDialog) parentWindow, null);
            }
            
            dialog.setVisible(true);
            if (dialog.isInvoiceSaved()) {
                loadInvoices();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error while creating the invoice: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void editSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            try {
                String numero = (String)tableModel.getValueAt(selectedRow, 0);
                Invoice invoice = loadInvoiceByNumber(numero);
                if (invoice != null) {
                    Window parentWindow = SwingUtilities.getWindowAncestor(this);
                    
                    InvoiceDialog dialog;
                    if (parentWindow instanceof JFrame) {
                        dialog = new InvoiceDialog((JFrame) parentWindow, invoice);
                    } else {
                        dialog = new InvoiceDialog((JDialog) parentWindow, invoice);
                    }
                    
                    dialog.setVisible(true);
                    if (dialog.isInvoiceSaved()) {
                        loadInvoices();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error while modifying the invoice: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
            
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete invoice " + numero + " from customer " + cliente + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
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
                        loadInvoices();
                        
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Error while deleting the invoice: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void printSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            try {
                String numero = (String)tableModel.getValueAt(selectedRow, 0);
                Invoice invoice = loadInvoiceByNumber(numero);
                if (invoice != null) {
                    InvoicePrinter printer = new InvoicePrinter(invoice);
                    printer.print();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error while printing the invoice: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}