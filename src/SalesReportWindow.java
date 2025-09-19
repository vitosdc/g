import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import javax.swing.border.TitledBorder;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.awt.Desktop;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SalesReportWindow extends JDialog {
    private JTextField startDateField;
    private JTextField endDateField;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel totalSalesLabel;
    private JLabel totalOrdersLabel;
    private JLabel averageOrderLabel;
    private SimpleDateFormat dateFormat;
    
    public SalesReportWindow(JFrame parent) {
        super(parent, "Sales Report", true);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadDefaultData();
    }
    
    private void setupWindow() {
        setSize(1000, 700);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Panel dei filtri
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));
        
        // Date fields
        startDateField = new JTextField(10);
        endDateField = new JTextField(10);
        
        // IMPOSTA DATE PREDEFINITE (ULTIMO MESE) - FIX PRINCIPALE
        Calendar cal = Calendar.getInstance();
        endDateField.setText(dateFormat.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        startDateField.setText(dateFormat.format(cal.getTime()));
        
        // Aggiungi tooltip con formato data
        startDateField.setToolTipText("Format: dd/MM/yyyy");
        endDateField.setToolTipText("Format: dd/MM/yyyy");
        
        filterPanel.add(new JLabel("Start Date:"));
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("End Date:"));
        filterPanel.add(endDateField);
        
        JButton filterButton = new JButton("Apply Filters");
        filterButton.addActionListener(e -> loadReportData());
        filterPanel.add(filterButton);
        
        // Pannello statistiche
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        
        totalSalesLabel = new JLabel("Total Sales: € 0.00");
        totalOrdersLabel = new JLabel("Number of Orders: 0");
        averageOrderLabel = new JLabel("Average per Order: € 0.00");
        
        statsPanel.add(totalSalesLabel);
        statsPanel.add(totalOrdersLabel);
        statsPanel.add(averageOrderLabel);
        
        // Tabella report
        String[] columns = {"Date", "Order ID", "Customer", "Status", "Total €"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(tableModel);
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        JButton printButton = new JButton("Print Report");
        JButton exportButton = new JButton("Export CSV");
        JButton detailsButton = new JButton("Order Details");
        
        printButton.addActionListener(e -> printReport());
        exportButton.addActionListener(e -> exportToCSV());
        detailsButton.addActionListener(e -> showOrderDetails());
        
        buttonPanel.add(printButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(detailsButton);
        
        // Layout principale
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadDefaultData() {
        // CARICA AUTOMATICAMENTE I DATI CON LE DATE PREDEFINITE
        SwingUtilities.invokeLater(() -> loadReportData());
    }
    
    private void loadReportData() {
        tableModel.setRowCount(0);
        try {
            // PARSING DELLE DATE - SEMPLIFICATO
            String startDateText = startDateField.getText().trim();
            String endDateText = endDateField.getText().trim();
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query;
            PreparedStatement pstmt = null;
            
            // Se le date sono vuote, carica tutto
            if (startDateText.isEmpty() || endDateText.isEmpty()) {
                query = """
                    SELECT o.id, o.data_ordine, o.stato, o.totale,
                           COALESCE(c.nome || ' ' || c.cognome, 'N/A') as cliente_nome
                    FROM ordini o
                    LEFT JOIN clienti c ON o.cliente_id = c.id
                    ORDER BY o.data_ordine DESC
                    LIMIT 1000
                """;
                pstmt = conn.prepareStatement(query);
                System.out.println("Loading all orders (no date filter)");
            } else {
                // Prova a parsare le date
                Date startDate = null;
                Date endDate = null;
                
                try {
                    startDate = DateUtils.parseDate(startDateText, dateFormat);
                    endDate = DateUtils.parseDate(endDateText, dateFormat);
                } catch (Exception e) {
                    System.err.println("Error parsing dates: " + e.getMessage());
                }
                
                if (startDate == null || endDate == null) {
                    JOptionPane.showMessageDialog(this,
                        "Invalid date format. Use dd/MM/yyyy\nLoading all orders instead.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                    
                    // Fallback: carica tutto
                    query = """
                        SELECT o.id, o.data_ordine, o.stato, o.totale,
                               COALESCE(c.nome || ' ' || c.cognome, 'N/A') as cliente_nome
                        FROM ordini o
                        LEFT JOIN clienti c ON o.cliente_id = c.id
                        ORDER BY o.data_ordine DESC
                        LIMIT 1000
                    """;
                    pstmt = conn.prepareStatement(query);
                } else {
                    // FILTRO DATE CON APPROCCIO STRING SEMPLICE
                    query = """
                        SELECT o.id, o.data_ordine, o.stato, o.totale,
                               COALESCE(c.nome || ' ' || c.cognome, 'N/A') as cliente_nome
                        FROM ordini o
                        LEFT JOIN clienti c ON o.cliente_id = c.id
                        WHERE o.data_ordine >= ? AND o.data_ordine <= ?
                        ORDER BY o.data_ordine DESC
                    """;
                    
                    pstmt = conn.prepareStatement(query);
                    
                    // CONVERTI DATE IN STRINGA ISO PER IL CONFRONTO
                    String startDateISO = String.format("%04d-%02d-%02d 00:00:00",
                        startDate.getYear() + 1900, startDate.getMonth() + 1, startDate.getDate());
                    String endDateISO = String.format("%04d-%02d-%02d 23:59:59",
                        endDate.getYear() + 1900, endDate.getMonth() + 1, endDate.getDate());
                    
                    pstmt.setString(1, startDateISO);
                    pstmt.setString(2, endDateISO);
                    
                    System.out.println("Filtering orders from " + startDateISO + " to " + endDateISO);
                }
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                double totalSales = 0;
                int totalOrders = 0;
                
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    
                    // PARSING DATA
                    Date orderDate = DateUtils.parseDate(rs, "data_ordine");
                    if (orderDate != null) {
                        row.add(DateUtils.formatDate(orderDate, dateFormat));
                    } else {
                        row.add("N/A");
                    }
                    
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("cliente_nome"));
                    row.add(rs.getString("stato"));
                    double total = rs.getDouble("totale");
                    row.add(String.format("%.2f", total));
                    
                    tableModel.addRow(row);
                    
                    totalSales += total;
                    totalOrders++;
                }
                
                // Aggiorna statistiche
                totalSalesLabel.setText(String.format("Total Sales: € %.2f", totalSales));
                totalOrdersLabel.setText("Number of Orders: " + totalOrders);
                if (totalOrders > 0) {
                    averageOrderLabel.setText(String.format("Average per Order: € %.2f", totalSales / totalOrders));
                } else {
                    averageOrderLabel.setText("Average per Order: € 0.00");
                }
                
                System.out.println("Loaded " + totalOrders + " orders, total sales: €" + totalSales);
            }
            
            if (pstmt != null) pstmt.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading the report: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showOrderDetails() {
        int selectedRow = reportTable.getSelectedRow();
        if (selectedRow != -1) {
            int orderId = (int)tableModel.getValueAt(selectedRow, 1);
            
            // Crea una finestra di dettaglio
            JDialog detailDialog = new JDialog(this, "Order Details #" + orderId, true);
            detailDialog.setSize(600, 400);
            detailDialog.setLocationRelativeTo(this);
            detailDialog.setLayout(new BorderLayout(10, 10));
            
            // Tabella dettagli
            String[] columns = {"Product", "Quantity", "Unit Price", "Total"};
            DefaultTableModel detailModel = new DefaultTableModel(columns, 0);
            JTable detailTable = new JTable(detailModel);
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String query = """
                    SELECT COALESCE(p.nome, 'Product N/A') as prodotto_nome, 
                           d.quantita, d.prezzo_unitario,
                           (d.quantita * d.prezzo_unitario) as totale
                    FROM dettagli_ordine d
                    LEFT JOIN prodotti p ON d.prodotto_id = p.id
                    WHERE d.ordine_id = ?
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, orderId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getString("prodotto_nome"));
                            row.add(rs.getInt("quantita"));
                            row.add(String.format("%.2f", rs.getDouble("prezzo_unitario")));
                            row.add(String.format("%.2f", rs.getDouble("totale")));
                            detailModel.addRow(row);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error loading the details: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
            detailDialog.add(new JScrollPane(detailTable), BorderLayout.CENTER);
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> detailDialog.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            detailDialog.add(buttonPanel, BorderLayout.SOUTH);
            detailDialog.setVisible(true);
        }
    }
    
    private void printReport() {
        // Crea una nuova finestra per l'anteprima di stampa
        JDialog previewDialog = new JDialog(this, "Print Preview - Sales Report", true);
        previewDialog.setSize(800, 600);
        previewDialog.setLocationRelativeTo(this);
        // Crea il pannello di anteprima
        JPanel previewPanel = new JPanel(new BorderLayout());
        // Crea l'intestazione
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String title = "Sales Report - " + new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Dati filtri
        JLabel dateRangeLabel = new JLabel(String.format("From: %s to: %s", startDateField.getText(), endDateField.getText()));
        dateRangeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(dateRangeLabel, BorderLayout.CENTER);
        
        // Tabella di anteprima
        JTable previewTable = new JTable(tableModel);
        
        previewPanel.add(headerPanel, BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        
        // Pulsanti di stampa
        JPanel printButtonPanel = new JPanel();
        JButton printButton = new JButton("Print");
        printButton.addActionListener(e -> {
            try {
                previewTable.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(previewDialog, "Error during printing: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> previewDialog.dispose());
        
        printButtonPanel.add(printButton);
        printButtonPanel.add(closeButton);
        previewDialog.add(previewPanel, BorderLayout.CENTER);
        previewDialog.add(printButtonPanel, BorderLayout.SOUTH);
        
        previewDialog.setVisible(true);
    }
    
    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report to CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getAbsolutePath().endsWith(".csv")) {
                fileToSave = new File(fileToSave + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
                // Intestazione colonne
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (i > 0) writer.print(",");
                    writer.print(tableModel.getColumnName(i));
                }
                writer.println();
                
                // Dati tabella
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    StringBuilder line = new StringBuilder();
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        if (j > 0) line.append(",");
                        String value = tableModel.getValueAt(i, j).toString();
                        // Gestisce le virgole nel testo racchiudendole tra virgolette
                        if (value.contains(",")) {
                            value = "\"" + value + "\"";
                        }
                        line.append(value);
                    }
                    writer.println(line);
                }
                
                // Riga vuota prima delle statistiche
                writer.println();
                
                // Statistiche
                writer.println("Summary");
                writer.println("Total Sales," + totalSalesLabel.getText().replace("Total Sales: ", ""));
                writer.println("Number of Orders," + totalOrdersLabel.getText().replace("Number of Orders: ", ""));
                writer.println("Average per Order," + averageOrderLabel.getText().replace("Average per Order: ", ""));
                
                JOptionPane.showMessageDialog(this,
                    "Report exported successfully",
                    "Export complete", JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error during export: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}