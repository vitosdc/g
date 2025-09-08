
// File: SupplierPriceListWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SupplierPriceListWindow extends JDialog {
    private int supplierId;
    private String supplierName;
    private JTable priceListTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private SimpleDateFormat dateFormat;
    
    public SupplierPriceListWindow(JDialog parent, int supplierId, String supplierName) {
        super(parent, "Listino Prezzi: " + supplierName, true);
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadPriceList();
    }
    
    private void setupWindow() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Tabella listino prezzi
        String[] columns = {"Prodotto", "Codice Fornitore", "Prezzo €", "Q.tà Min.", "Validità Dal", "Validità Al", "Note"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        priceListTable = new JTable(tableModel);
        priceListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        priceListTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Nuovo Prezzo");
        editButton = new JButton("Modifica");
        deleteButton = new JButton("Elimina");
        refreshButton = new JButton("Aggiorna");
        
        addButton.addActionListener(e -> showPriceDialog(null));
        editButton.addActionListener(e -> editSelectedPrice());
        deleteButton.addActionListener(e -> deleteSelectedPrice());
        refreshButton.addActionListener(e -> loadPriceList());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        // Layout principale
        add(new JScrollPane(priceListTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRowSelected = priceListTable.getSelectedRow() != -1;
        editButton.setEnabled(isRowSelected);
        deleteButton.setEnabled(isRowSelected);
    }
    
    private void loadPriceList() {
        tableModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT l.*, p.nome as prodotto_nome
                FROM listini_fornitori l
                JOIN prodotti p ON l.prodotto_id = p.id
                WHERE l.fornitore_id = ?
                ORDER BY p.nome
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, supplierId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("codice_prodotto_fornitore"));
                    row.add(String.format("%.2f", rs.getDouble("prezzo")));
                    row.add(rs.getInt("quantita_minima"));
                    row.add(dateFormat.format(rs.getDate("data_validita_inizio")));
                    Date validitaFine = rs.getDate("data_validita_fine");
                    row.add(validitaFine != null ? dateFormat.format(validitaFine) : "");
                    row.add(rs.getString("note"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento del listino prezzi: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showPriceDialog(SupplierPriceList priceList) {
        SupplierPriceListDialog dialog = new SupplierPriceListDialog(this, supplierId, supplierName, priceList);
        dialog.setVisible(true);
        if (dialog.isPriceSaved()) {
            loadPriceList();
        }
    }
    
    private void editSelectedPrice() {
        int selectedRow = priceListTable.getSelectedRow();
        if (selectedRow != -1) {
            String prodottoNome = (String)tableModel.getValueAt(selectedRow, 0);
            String codiceFornitore = (String)tableModel.getValueAt(selectedRow, 1);
            try {
                SupplierPriceList priceList = loadPriceListItem(prodottoNome, codiceFornitore);
                if (priceList != null) {
                    showPriceDialog(priceList);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante il caricamento del prezzo: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private SupplierPriceList loadPriceListItem(String prodottoNome, String codiceFornitore) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String query = """
            SELECT l.*, p.nome as prodotto_nome
            FROM listini_fornitori l
            JOIN prodotti p ON l.prodotto_id = p.id
            WHERE l.fornitore_id = ? AND p.nome = ? AND l.codice_prodotto_fornitore = ?
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, supplierId);
            pstmt.setString(2, prodottoNome);
            pstmt.setString(3, codiceFornitore);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new SupplierPriceList(
                        rs.getInt("id"),
                        rs.getInt("fornitore_id"),
                        rs.getInt("prodotto_id"),
                        rs.getString("codice_prodotto_fornitore"),
                        rs.getDouble("prezzo"),
                        rs.getInt("quantita_minima"),
                        rs.getDate("data_validita_inizio"),
                        rs.getDate("data_validita_fine"),
                        rs.getString("note")
                    );
                }
            }
        }
        return null;
    }
    
    private void deleteSelectedPrice() {
        int selectedRow = priceListTable.getSelectedRow();
        if (selectedRow != -1) {
            String prodottoNome = (String)tableModel.getValueAt(selectedRow, 0);
            String codiceFornitore = (String)tableModel.getValueAt(selectedRow, 1);
            
            int result = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler eliminare il prezzo per il prodotto " + prodottoNome + "?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    String query = """
                        DELETE FROM listini_fornitori 
                        WHERE fornitore_id = ? 
                        AND prodotto_id = (SELECT id FROM prodotti WHERE nome = ?)
                        AND codice_prodotto_fornitore = ?
                    """;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, supplierId);
                        pstmt.setString(2, prodottoNome);
                        pstmt.setString(3, codiceFornitore);
                        pstmt.executeUpdate();
                        loadPriceList();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Errore durante l'eliminazione del prezzo: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
