import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdvancedStatsWindow extends JDialog {
    private JTabbedPane tabbedPane;
    private SimpleDateFormat dateFormat;
    private JPanel salesChartPanel;
    private JPanel productsChartPanel;
    private JTable topProductsTable;
    private JComboBox<String> periodCombo;
    private Map<String, double[]> monthlySales;
    private List<Object[]> productStats;

    public AdvancedStatsWindow(JFrame parent) {
        super(parent, "Advanced Statistics", true);
        dateFormat = new SimpleDateFormat("MM/yyyy");
        monthlySales = new HashMap<>();
        productStats = new ArrayList<>();

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
        
        // Pannello periodo
        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        periodCombo = new JComboBox<>(new String[]{"Last 6 months", "Last year", "Last 2 years"});
        periodCombo.addActionListener(e -> loadData());
        periodPanel.add(new JLabel("Period: "));
        periodPanel.add(periodCombo);

        // Tab Vendite
        JPanel salesPanel = new JPanel(new BorderLayout());
        salesChartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSalesChart(g);
            }
        };
        salesPanel.add(periodPanel, BorderLayout.NORTH);
        salesPanel.add(salesChartPanel, BorderLayout.CENTER);

        // Tab Prodotti
        JPanel productsPanel = new JPanel(new BorderLayout());
        productsChartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawProductsChart(g);
            }
        };
        String[] columns = {"Product", "Quantity Sold", "Revenue", "% of Total"};
        topProductsTable = new JTable(new Object[0][4], columns);

        JPanel productsTopPanel = new JPanel(new GridLayout(2, 1));
        productsTopPanel.add(productsChartPanel);
        productsTopPanel.add(new JScrollPane(topProductsTable));
        productsPanel.add(productsTopPanel);

        // Aggiunta tabs
        tabbedPane.addTab("Sales Trend", salesPanel);
        tabbedPane.addTab("Product Analysis", productsPanel);

        add(tabbedPane);
    }

    private void loadData() {
        loadSalesData();
        loadProductsData();
        repaint();
    }

    private void loadSalesData() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            // Calcola periodo
            Calendar cal = Calendar.getInstance();
            int months = switch(periodCombo.getSelectedIndex()) {
                case 0 -> 6;
                case 1 -> 12;
                case 2 -> 24;
                default -> 12;
            };
            cal.add(Calendar.MONTH, -months);

            String query = """
                SELECT strftime('%Y-%m', data_ordine) as mese,
                        SUM(totale) as totale,
                        COUNT(*) as num_ordini
                FROM ordini
                WHERE DATE(data_ordine) >= DATE(?)
                GROUP BY mese
                ORDER BY mese
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDate(1, DateUtils.toSqlDate(cal.getTime()));
                ResultSet rs = pstmt.executeQuery();

                monthlySales.clear();
                while (rs.next()) {
                    String month = rs.getString("mese");
                    double total = rs.getDouble("totale");
                    int numOrders = rs.getInt("num_ordini");
                    monthlySales.put(month, new double[]{total, numOrders});
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading sales data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void drawSalesChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 50;

        // Trova valori min/max
        double maxSale = monthlySales.values().stream()
            .mapToDouble(values -> values[0])
            .max().orElse(0);

        // Disegna assi
        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, height - padding, width - padding, height - padding); // X
        g2d.drawLine(padding, padding, padding, height - padding); // Y

        if (monthlySales.isEmpty()) return;

        // Disegna punti e linee
        List<String> months = new ArrayList<>(monthlySales.keySet());
        Collections.sort(months);
        int numPoints = months.size();
        int xStep = (width - 2 * padding) / Math.max(numPoints - 1, 1);
        int x = padding;
        Point prevPoint = null;

        for (String month : months) {
            double[] values = monthlySales.get(month);
            double sale = values[0];
            int y = height - padding - (int)((sale / maxSale) * (height - 2 * padding));

            // Disegna punto
            g2d.setColor(Color.BLUE);
            g2d.fillOval(x - 4, y - 4, 8, 8);

            // Disegna linea
            if (prevPoint != null) {
                g2d.drawLine(prevPoint.x, prevPoint.y, x, y);
            }
            prevPoint = new Point(x, y);

            // Etichetta mese
            g2d.setColor(Color.BLACK);
            String label = month;
            g2d.rotate(Math.PI / 4, x, height - padding + 20);
            g2d.drawString(label, x, height - padding + 20);
            g2d.rotate(-Math.PI / 4, x, height - padding + 20);

            x += xStep;
        }

        // Disegna scala Y
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= 5; i++) {
            int y = height - padding - (i * (height - 2 * padding) / 5);
            double value = (maxSale * i) / 5;
            g2d.drawString(String.format("€ %.2f", value), 5, y);
        }
    }

    private void loadProductsData() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            // Calcola periodo
            Calendar cal = Calendar.getInstance();
            int months = switch(periodCombo.getSelectedIndex()) {
                case 0 -> 6;
                case 1 -> 12;
                case 2 -> 24;
                default -> 12;
            };
            cal.add(Calendar.MONTH, -months);

            String query = """
                SELECT p.nome,
                        SUM(d.quantita) as quantita_totale,
                        SUM(d.quantita * d.prezzo_unitario) as fatturato,
                        COUNT(DISTINCT o.id) as num_ordini
                FROM prodotti p
                JOIN dettagli_ordine d ON p.id = d.prodotto_id
                JOIN ordini o ON d.ordine_id = o.id
                WHERE DATE(o.data_ordine) >= DATE(?)
                GROUP BY p.id, p.nome
                ORDER BY fatturato DESC
                LIMIT 10
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDate(1, DateUtils.toSqlDate(cal.getTime()));
                ResultSet rs = pstmt.executeQuery();

                // Calcola il totale per le percentuali
                double totalRevenue = 0;
                productStats.clear();

                while (rs.next()) {
                    String nome = rs.getString("nome");
                    int quantita = rs.getInt("quantita_totale");
                    double fatturato = rs.getDouble("fatturato");
                    totalRevenue += fatturato;
                    productStats.add(new Object[]{nome, quantita, fatturato});
                }

                // Aggiorna la tabella
                Object[][] data = new Object[productStats.size()][4];
                for (int i = 0; i < productStats.size(); i++) {
                    Object[] row = productStats.get(i);
                    data[i][0] = row[0];  // nome
                    data[i][1] = row[1];  // quantità
                    data[i][2] = String.format("€ %.2f", (double)row[2]); // fatturato
                    data[i][3] = String.format("%.1f%%", ((double)row[2] / totalRevenue) * 100); // percentuale
                }

                topProductsTable.setModel(new javax.swing.table.DefaultTableModel(
                    data,
                    new String[]{"Product", "Quantity Sold", "Revenue", "% of Total"}
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading product data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void drawProductsChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 50;

        if (productStats.isEmpty()) return;

        // Trova il fatturato totale
        double totalRevenue = productStats.stream()
            .mapToDouble(row -> (double)row[2])
            .sum();

        // Disegna il grafico a torta
        int diameter = Math.min(width, height) - 2 * padding;
        int centerX = width / 2;
        int centerY = height / 2;

        double startAngle = 0;
        int legendY = padding;

        for (Object[] row : productStats) {
            String nome = (String)row[0];
            double fatturato = (double)row[2];
            double percentage = (fatturato / totalRevenue);
            double arcAngle = 360 * percentage;

            // Genera un colore casuale ma non troppo chiaro
            Color color = new Color(
                (int)(Math.random() * 156) + 50,
                (int)(Math.random() * 156) + 50,
                (int)(Math.random() * 156) + 50
            );

            g2d.setColor(color);
            g2d.fillArc(centerX - diameter/2, centerY - diameter/2,
                        diameter, diameter,
                        (int)startAngle, (int)arcAngle);

            // Disegna legenda
            g2d.fillRect(width - 200, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            String label = nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
            g2d.drawString(String.format("%s (%.1f%%)",
                label, percentage * 100),
                width - 180, legendY + 12);

            startAngle += arcAngle;
            legendY += 20;
        }
    }
}