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
//Aggiunti questi nuovi import
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
        super(parent, "Report Vendite", true);
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
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtri"));
        
        // Date fields
        startDateField = new JTextField(10);
        endDateField = new JTextField(10);
        
        // Imposta date predefinite (ultimo mese)
        Calendar cal = Calendar.getInstance();
        endDateField.setText(dateFormat.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        startDateField.setText(dateFormat.format(cal.getTime()));
        
        // Aggiungi tooltip con formato data
        startDateField.setToolTipText("Formato: dd/MM/yyyy");
        endDateField.setToolTipText("Formato: dd/MM/yyyy");
        
        filterPanel.add(new JLabel("Data Inizio:"));
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("Data Fine:"));
        filterPanel.add(endDateField);
        
        JButton filterButton = new JButton("Applica Filtri");
        filterButton.addActionListener(e -> loadReportData());
        filterPanel.add(filterButton);
        
        // Pannello statistiche
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistiche"));
        
        totalSalesLabel = new JLabel("Totale Vendite: € 0.00");
        totalOrdersLabel = new JLabel("Numero Ordini: 0");
        averageOrderLabel = new JLabel("Media per Ordine: € 0.00");
        
        statsPanel.add(totalSalesLabel);
        statsPanel.add(totalOrdersLabel);
        statsPanel.add(averageOrderLabel);
        
        // Tabella report
        String[] columns = {"Data", "Ordine ID", "Cliente", "Stato", "Totale €"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(tableModel);
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        JButton printButton = new JButton("Stampa Report");
        JButton exportButton = new JButton("Esporta CSV");
        JButton detailsButton = new JButton("Dettagli Ordine");
        
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
        loadReportData();
    }
    
    private void loadReportData() {
        tableModel.setRowCount(0);
        try {
            // Parsing delle date
            Date startDate = dateFormat.parse(startDateField.getText());
            Date endDate = dateFormat.parse(endDateField.getText());
            
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT o.*, c.nome || ' ' || c.cognome as cliente_nome
                FROM ordini o
                LEFT JOIN clienti c ON o.cliente_id = c.id
                WHERE o.data_ordine BETWEEN ? AND ?
                ORDER BY o.data_ordine DESC
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
                pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
                
                double totalSales = 0;
                int totalOrders = 0;
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        Date orderDate = rs.getDate("data_ordine");
                        row.add(dateFormat.format(orderDate));
                        row.add(rs.getInt("id"));
                        row.add(rs.getString("cliente_nome"));
                        row.add(rs.getString("stato"));
                        double total = rs.getDouble("totale");
                        row.add(String.format("%.2f", total));
                        
                        tableModel.addRow(row);
                        
                        totalSales += total;
                        totalOrders++;
                    }
                }
                
                // Aggiorna statistiche
                totalSalesLabel.setText(String.format("Totale Vendite: € %.2f", totalSales));
                totalOrdersLabel.setText("Numero Ordini: " + totalOrders);
                if (totalOrders > 0) {
                    averageOrderLabel.setText(String.format("Media per Ordine: € %.2f", totalSales / totalOrders));
                } else {
                    averageOrderLabel.setText("Media per Ordine: € 0.00");
                }
            }
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this,
                "Formato data non valido. Usa il formato dd/MM/yyyy",
                "Errore", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento del report: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showOrderDetails() {
        int selectedRow = reportTable.getSelectedRow();
        if (selectedRow != -1) {
            int orderId = (int)tableModel.getValueAt(selectedRow, 1);
            
            // Crea una finestra di dettaglio
            JDialog detailDialog = new JDialog(this, "Dettagli Ordine #" + orderId, true);
            detailDialog.setSize(600, 400);
            detailDialog.setLocationRelativeTo(this);
            detailDialog.setLayout(new BorderLayout(10, 10));
            
            // Tabella dettagli
            String[] columns = {"Prodotto", "Quantità", "Prezzo Unit.", "Totale"};
            DefaultTableModel detailModel = new DefaultTableModel(columns, 0);
            JTable detailTable = new JTable(detailModel);
            
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String query = """
                    SELECT p.nome as prodotto_nome, d.quantita, d.prezzo_unitario,
                           (d.quantita * d.prezzo_unitario) as totale
                    FROM dettagli_ordine d
                    JOIN prodotti p ON d.prodotto_id = p.id
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
                    "Errore durante il caricamento dei dettagli: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
            
            detailDialog.add(new JScrollPane(detailTable), BorderLayout.CENTER);
            
            JButton closeButton = new JButton("Chiudi");
            closeButton.addActionListener(e -> detailDialog.dispose());
            
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            detailDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            detailDialog.setVisible(true);
        }
    }
    
    private void printReport() {
        // Crea una nuova finestra per l'anteprima di stampa
        JDialog previewDialog = new JDialog(this, "Anteprima Stampa - Report Vendite", true);
        previewDialog.setSize(800, 600);
        previewDialog.setLocationRelativeTo(this);
        
        // Crea il pannello di anteprima
        JPanel previewPanel = new JPanel(new BorderLayout());
        
        // Crea l'intestazione
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String title = "Report Vendite - " + new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Pannello statistiche
        JPanel statsPanel = new JPanel(new GridLayout(1, 3));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statsPanel.add(new JLabel(totalSalesLabel.getText()));
        statsPanel.add(new JLabel(totalOrdersLabel.getText()));
        statsPanel.add(new JLabel(averageOrderLabel.getText()));
        
        // Copia la tabella per l'anteprima
        JTable previewTable = new JTable(tableModel);
        previewTable.setEnabled(false);
        
        // Aggiungi i componenti al pannello di anteprima
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.CENTER);
        
        previewPanel.add(topPanel, BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        
        // Aggiungi pulsanti per la stampa/esportazione
        JPanel buttonPanel = new JPanel();
        JButton pdfButton = new JButton("Esporta come PDF");
        JButton closeButton = new JButton("Chiudi");
        
        pdfButton.addActionListener(e -> {
            exportToPDF(previewTable, title, statsPanel, "report_vendite.pdf");
            previewDialog.dispose();
        });
        
        closeButton.addActionListener(e -> previewDialog.dispose());
        
        buttonPanel.add(pdfButton);
        buttonPanel.add(closeButton);
        previewPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Mostra la finestra di anteprima
        previewDialog.add(previewPanel);
        previewDialog.setVisible(true);
    }
    
    private void exportToPDF(JTable table, String title, JPanel statsPanel, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salva PDF");
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }
                
                // Crea un documento temporaneo HTML
                File tempFile = File.createTempFile("report", ".html");
                try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
                    writer.println("<html><head><title>" + title + "</title>");
                    writer.println("<style>");
                    writer.println("body { font-family: Arial, sans-serif; }");
                    writer.println("table { border-collapse: collapse; width: 100%; }");
                    writer.println("th, td { border: 1px solid #ddd; padding: 8px; }");
                    writer.println("th { background-color: #f2f2f2; }");
                    writer.println(".stats { display: flex; justify-content: space-between; margin: 20px 0; }");
                    writer.println(".stat-item { background-color: #f8f9fa; padding: 10px; border-radius: 5px; }");
                    writer.println("</style></head><body>");
                    
                    writer.println("<h1>" + title + "</h1>");
                    
                    // Statistiche
                    writer.println("<div class='stats'>");
                    writer.println("<div class='stat-item'>" + totalSalesLabel.getText() + "</div>");
                    writer.println("<div class='stat-item'>" + totalOrdersLabel.getText() + "</div>");
                    writer.println("<div class='stat-item'>" + averageOrderLabel.getText() + "</div>");
                    writer.println("</div>");
                    
                    // Tabella dati
                    writer.println("<table>");
                    
                    // Intestazioni
                    writer.println("<tr>");
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        writer.println("<th>" + table.getColumnName(i) + "</th>");
                    }
                    writer.println("</tr>");
                    
                    // Dati
                    for (int row = 0; row < table.getRowCount(); row++) {
                        writer.println("<tr>");
                        for (int col = 0; col < table.getColumnCount(); col++) {
                            Object value = table.getValueAt(row, col);
                            writer.println("<td>" + (value != null ? value.toString() : "") + "</td>");
                        }
                        writer.println("</tr>");
                    }
                    
                    writer.println("</table></body></html>");
                }
                
                // Apri il file HTML nel browser predefinito
                Desktop.getDesktop().browse(tempFile.toURI());
                
                JOptionPane.showMessageDialog(this,
                    "Il report è stato aperto nel browser. Utilizzare la funzione di stampa del browser per salvare come PDF.",
                    "Informazione",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Elimina il file temporaneo quando l'applicazione viene chiusa
                tempFile.deleteOnExit();
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante la creazione del PDF: " + e.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salva Report CSV");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                // Intestazioni
                writer.println("Data,Ordine ID,Cliente,Stato,Totale");
                
                // Dati dalla tabella
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
                writer.println("Riepilogo");
                writer.println("Totale Vendite," + totalSalesLabel.getText().replace("Totale Vendite: ", ""));
                writer.println("Numero Ordini," + totalOrdersLabel.getText().replace("Numero Ordini: ", ""));
                writer.println("Media per Ordine," + averageOrderLabel.getText().replace("Media per Ordine: ", ""));
                
                JOptionPane.showMessageDialog(this,
                    "Report esportato con successo",
                    "Esportazione completata", JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante l'esportazione: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}