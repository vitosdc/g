
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoicesWindow extends JDialog {
    private JTable invoicesTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton printButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public InvoicesWindow(JFrame parent) {
        super(parent, "Gestione Fatture", true);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadInvoices();
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
        searchButton.addActionListener(e -> searchInvoices());
        searchPanel.add(new JLabel("Cerca: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Tabella fatture
        String[] columns = {"Numero", "Data", "Cliente", "Imponibile", "IVA", "Totale", "Stato"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        invoicesTable = new JTable(tableModel);
        invoicesTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Nuova Fattura");
        editButton = new JButton("Modifica");
        deleteButton = new JButton("Elimina");
        printButton = new JButton("Stampa");
        refreshButton = new JButton("Aggiorna");
        
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
        
        // Layout principale
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
                    Date data = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("data"));
                    row.add(dateFormat.format(data));
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
                "Errore durante il caricamento delle fatture: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
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
                        Date data = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("data"));
                        row.add(dateFormat.format(data));
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
                "Errore durante la ricerca delle fatture: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createNewInvoice() {
        try {
            InvoiceDialog dialog = new InvoiceDialog(this, null);
            dialog.setVisible(true);
            if (dialog.isInvoiceSaved()) {
                loadInvoices();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante la creazione della fattura: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void editSelectedInvoice() {
        int selectedRow = invoicesTable.getSelectedRow();
        if (selectedRow != -1) {
            try {
                String numero = (String)tableModel.getValueAt(selectedRow, 0);
                Invoice invoice = loadInvoiceByNumber(numero);
                if (invoice != null) {
                    InvoiceDialog dialog = new InvoiceDialog(this, invoice);
                    dialog.setVisible(true);
                    if (dialog.isInvoiceSaved()) {
                        loadInvoices();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante la modifica della fattura: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
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
                    Invoice invoice = new Invoice(
                        rs.getInt("id"),
                        rs.getString("numero"),
                        new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("data")),
                        rs.getInt("cliente_id"),
                        rs.getString("cliente_nome"),
                        rs.getDouble("imponibile"),
                        rs.getDouble("iva"),
                        rs.getDouble("totale"),
                        rs.getString("stato")
                    );
                    
                    // Carica i dettagli della fattura
                    loadInvoiceItems(invoice);
                    return invoice;
                }
            } catch (Exception e) {
                throw new SQLException("Errore nel caricamento della fattura: " + e.getMessage());
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
                "Sei sicuro di voler eliminare la fattura " + numero + " del cliente " + cliente + "?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    conn.setAutoCommit(false);
                    
                    try {
                        // Prima elimina i dettagli della fattura
                        String deleteDetailsQuery = "DELETE FROM dettagli_fattura WHERE fattura_id = (SELECT id FROM fatture WHERE numero = ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(deleteDetailsQuery)) {
                            pstmt.setString(1, numero);
                            pstmt.executeUpdate();
                        }
                        
                        // Poi elimina la fattura
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
                        "Errore durante l'eliminazione della fattura: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
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
                    "Errore durante la stampa della fattura: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}