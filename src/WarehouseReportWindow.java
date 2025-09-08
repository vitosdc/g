import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Date;
import java.io.*;

public class WarehouseReportWindow extends JDialog {
    private JTabbedPane tabbedPane;
    private JTable productsTable;
    private JTable movementsTable;
    private DefaultTableModel productsModel;
    private DefaultTableModel movementsModel;
    private SimpleDateFormat dateFormat;
    private JTextField startDateField;
    private JTextField endDateField;
    
    public WarehouseReportWindow(JFrame parent) {
        super(parent, "Report Magazzino", true);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
        loadData();
    }
    
    private void setupWindow() {
        setSize(1000, 700);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        
        // Tab Situazione Prodotti
        JPanel productsPanel = createProductsPanel();
        tabbedPane.addTab("Situazione Prodotti", productsPanel);
        
        // Tab Movimenti
        JPanel movementsPanel = createMovementsPanel();
        tabbedPane.addTab("Analisi Movimenti", movementsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Statistiche generali
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistiche Generali"));
        
        JLabel totalProductsLabel = new JLabel("Totale Prodotti: 0");
        JLabel totalValueLabel = new JLabel("Valore Totale: € 0,00");
        JLabel lowStockLabel = new JLabel("Prodotti Sotto Scorta: 0");
        JLabel outOfStockLabel = new JLabel("Prodotti Esauriti: 0");
        
        statsPanel.add(totalProductsLabel);
        statsPanel.add(totalValueLabel);
        statsPanel.add(lowStockLabel);
        statsPanel.add(outOfStockLabel);
        
        // Tabella prodotti
        String[] columns = {"Codice", "Prodotto", "Quantità", "Valore Unit.", "Valore Totale", "Stato"};
        productsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(productsModel);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton printButton = new JButton("Stampa Report");
        JButton exportButton = new JButton("Esporta CSV");
        JButton refreshButton = new JButton("Aggiorna");
        
        printButton.addActionListener(e -> printProductsReport());
        exportButton.addActionListener(e -> exportProductsToCSV());
        refreshButton.addActionListener(e -> loadProductsData());
        
        buttonPanel.add(printButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(refreshButton);
        
        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(productsTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createMovementsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Filtri
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtri"));
        
        startDateField = new JTextField(10);
        endDateField = new JTextField(10);
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"Tutti", "CARICO", "SCARICO"});
        
        // Imposta date predefinite (ultimo mese)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        endDateField.setText(dateFormat.format(cal.getTime()));
        cal.add(java.util.Calendar.MONTH, -1);
        startDateField.setText(dateFormat.format(cal.getTime()));
        
        filterPanel.add(new JLabel("Da:"));
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("A:"));
        filterPanel.add(endDateField);
        filterPanel.add(new JLabel("Tipo:"));
        filterPanel.add(tipoCombo);
        
        JButton applyButton = new JButton("Applica");
        applyButton.addActionListener(e -> loadMovementsData());
        filterPanel.add(applyButton);
        
        // Tabella movimenti
        String[] columns = {"Data", "Prodotto", "Tipo", "Quantità", "Causale", "Documento"};
        movementsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movementsTable = new JTable(movementsModel);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel();
        JButton printButton = new JButton("Stampa Report");
        JButton exportButton = new JButton("Esporta CSV");
        
        printButton.addActionListener(e -> printMovementsReport());
        exportButton.addActionListener(e -> exportMovementsToCSV());
        
        buttonPanel.add(printButton);
        buttonPanel.add(exportButton);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(movementsTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadData() {
        loadProductsData();
        loadMovementsData();
    }
    
    private void loadProductsData() {
        productsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT p.*, COALESCE(sm.quantita_minima, 0) as quantita_minima
                FROM prodotti p
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                ORDER BY p.nome
            """;
            
            int totalProducts = 0;
            double totalValue = 0;
            int lowStock = 0;
            int outOfStock = 0;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("codice"));
                    row.add(rs.getString("nome"));
                    
                    int quantita = rs.getInt("quantita");
                    double prezzo = rs.getDouble("prezzo");
                    double valoreTotale = quantita * prezzo;
                    int quantitaMinima = rs.getInt("quantita_minima");
                    
                    row.add(quantita);
                    row.add(String.format("%.2f", prezzo));
                    row.add(String.format("%.2f", valoreTotale));
                    
                    // Determina lo stato
                    String stato;
                    if (quantita <= 0) {
                        stato = "ESAURITO";
                        outOfStock++;
                    } else if (quantitaMinima > 0 && quantita < quantitaMinima) {
                        stato = "SOTTO SCORTA";
                        lowStock++;
                    } else {
                        stato = "OK";
                    }
                    row.add(stato);
                    
                    productsModel.addRow(row);
                    totalProducts++;
                    totalValue += valoreTotale;
                }
            }
            
            // Aggiorna le statistiche
            Component[] components = ((JPanel)((JPanel)tabbedPane.getComponentAt(0))
                .getComponent(0)).getComponents();
            
            ((JLabel)components[0]).setText("Totale Prodotti: " + totalProducts);
            ((JLabel)components[1]).setText(String.format("Valore Totale: € %.2f", totalValue));
            ((JLabel)components[2]).setText("Prodotti Sotto Scorta: " + lowStock);
            ((JLabel)components[3]).setText("Prodotti Esauriti: " + outOfStock);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei dati: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadMovementsData() {
        movementsModel.setRowCount(0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT m.*, p.nome as prodotto_nome
                FROM movimenti_magazzino m
                JOIN prodotti p ON m.prodotto_id = p.id
                WHERE m.data BETWEEN ? AND ?
                ORDER BY m.data DESC
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDate(1, new java.sql.Date(dateFormat.parse(startDateField.getText()).getTime()));
                pstmt.setDate(2, new java.sql.Date(dateFormat.parse(endDateField.getText()).getTime()));
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(dateFormat.format(rs.getTimestamp("data")));
                    row.add(rs.getString("prodotto_nome"));
                    row.add(rs.getString("tipo"));
                    row.add(rs.getInt("quantita"));
                    row.add(rs.getString("causale"));
                    
                    String documento = rs.getString("documento_tipo");
                    if (documento != null && !documento.isEmpty()) {
                        documento += " " + rs.getString("documento_numero");
                    }
                    row.add(documento);
                    
                    movementsModel.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Errore durante il caricamento dei movimenti: " + e.getMessage(),
                "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void printProductsReport() {
        // Crea una nuova finestra per l'anteprima di stampa
        JDialog previewDialog = new JDialog(this, "Anteprima Stampa - Report Prodotti", true);
        previewDialog.setSize(800, 600);
        previewDialog.setLocationRelativeTo(this);
        
        // Crea il pannello di anteprima
        JPanel previewPanel = new JPanel(new BorderLayout());
        
        // Crea l'intestazione
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Report Situazione Magazzino - " + 
            new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Copia la tabella per l'anteprima
        JTable previewTable = new JTable(productsModel);
        previewTable.setEnabled(false);
        
        // Aggiungi i componenti al pannello di anteprima
        previewPanel.add(headerPanel, BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        
        // Aggiungi pulsanti per la stampa/esportazione
        JPanel buttonPanel = new JPanel();
        JButton pdfButton = new JButton("Esporta come PDF");
        JButton closeButton = new JButton("Chiudi");
        
        pdfButton.addActionListener(e -> {
            exportToPDF(previewTable, titleLabel.getText(), "report_prodotti.pdf");
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
    
    private void printMovementsReport() {
        // Crea una nuova finestra per l'anteprima di stampa
        JDialog previewDialog = new JDialog(this, "Anteprima Stampa - Report Movimenti", true);
        previewDialog.setSize(800, 600);
        previewDialog.setLocationRelativeTo(this);
        
        // Crea il pannello di anteprima
        JPanel previewPanel = new JPanel(new BorderLayout());
        
        // Crea l'intestazione
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String title = "Report Movimenti Magazzino - Dal " + startDateField.getText() +
                      " al " + endDateField.getText();
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Copia la tabella per l'anteprima
        JTable previewTable = new JTable(movementsModel);
        previewTable.setEnabled(false);
        
        // Aggiungi i componenti al pannello di anteprima
        previewPanel.add(headerPanel, BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        
        // Aggiungi pulsanti per la stampa/esportazione
        JPanel buttonPanel = new JPanel();
        JButton pdfButton = new JButton("Esporta come PDF");
        JButton closeButton = new JButton("Chiudi");
        
        pdfButton.addActionListener(e -> {
            exportToPDF(previewTable, titleLabel.getText(), "report_movimenti.pdf");
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
    
    private void exportToPDF(JTable table, String title, String defaultFileName) {
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
                    writer.println("<html><head><title>" + title + "</title></head><body>");
                    writer.println("<h1>" + title + "</h1>");
                    writer.println("<table border='1' cellpadding='5'>");
                    
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
    
    private void exportProductsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salva Report CSV");
        fileChooser.setSelectedFile(new File("report_magazzino.csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                // Intestazioni
                for (int i = 0; i < productsModel.getColumnCount(); i++) {
                    writer.print(productsModel.getColumnName(i));
                    writer.print(i < productsModel.getColumnCount() - 1 ? "," : "\n");
                }
                
                // Dati
                for (int row = 0; row < productsModel.getRowCount(); row++) {
                    for (int col = 0; col < productsModel.getColumnCount(); col++) {
                        String value = productsModel.getValueAt(row, col).toString();
                        // Gestisce le virgole nel testo
                        if (value.contains(",")) {
                            value = "\"" + value + "\"";
                        }
                        writer.print(value);
                        writer.print(col < productsModel.getColumnCount() - 1 ? "," : "\n");
                    }
                }
                
                JOptionPane.showMessageDialog(this,
                    "Report esportato con successo",
                    "Esportazione completata",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante l'esportazione: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportMovementsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salva Report Movimenti CSV");
        fileChooser.setSelectedFile(new File("report_movimenti.csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                // Intestazioni
                writer.println("Report Movimenti Magazzino");
                writer.println("Periodo: " + startDateField.getText() + " - " + endDateField.getText());
                writer.println();
                
                // Intestazioni colonne
                for (int i = 0; i < movementsModel.getColumnCount(); i++) {
                    writer.print(movementsModel.getColumnName(i));
                    writer.print(i < movementsModel.getColumnCount() - 1 ? "," : "\n");
                }
                
                // Dati
                for (int row = 0; row < movementsModel.getRowCount(); row++) {
                    for (int col = 0; col < movementsModel.getColumnCount(); col++) {
                        String value = movementsModel.getValueAt(row, col).toString();
                        // Gestisce le virgole nel testo
                        if (value.contains(",")) {
                            value = "\"" + value + "\"";
                        }
                        writer.print(value);
                        writer.print(col < movementsModel.getColumnCount() - 1 ? "," : "\n");
                    }
                }
                
                JOptionPane.showMessageDialog(this,
                    "Report movimenti esportato con successo",
                    "Esportazione completata",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Errore durante l'esportazione: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}