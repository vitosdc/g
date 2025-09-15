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
            
            int months = switch(periodCombo.getSelectedIndex()) {
                case 0 -> 6;
                case 1 -> 12;
                case 2 -> 24;
                default -> 12;
            };

            // FIXED: Query with proper period filtering
            String query = "SELECT strftime('%Y-%m', o.data_ordine) as mese, " +
                          "SUM(o.totale) as totale, " +
                          "COUNT(*) as num_ordini " +
                          "FROM ordini o " +
                          "WHERE o.data_ordine IS NOT NULL " +
                          "AND o.data_ordine >= datetime('now', '-" + months + " months') " +
                          "GROUP BY mese " +
                          "ORDER BY mese";

            System.out.println("Loading sales data for last " + months + " months");

            // FIXED: Thread-safe access to monthlySales
            Map<String, double[]> newMonthlySales = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String month = rs.getString("mese");
                    // CONTROLLA CHE IL MESE NON SIA NULL
                    if (month != null && !month.trim().isEmpty()) {
                        double total = rs.getDouble("totale");
                        int numOrders = rs.getInt("num_ordini");
                        newMonthlySales.put(month, new double[]{total, numOrders});
                        System.out.println("Month: " + month + ", Total: €" + total + ", Orders: " + numOrders);
                    }
                }
                
                System.out.println("Loaded " + newMonthlySales.size() + " months of sales data");
            }
            
            // Update reference atomically
            monthlySales = newMonthlySales;

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

        int width = salesChartPanel.getWidth();
        int height = salesChartPanel.getHeight();
        int padding = 50;

        // FIXED: Safe access to monthlySales
        Map<String, double[]> currentSales = monthlySales; // Local reference
        if (currentSales.isEmpty()) {
            // Disegna messaggio "No data"
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "No sales data available";
            int messageWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (width - messageWidth) / 2, height / 2);
            return;
        }

        // Find max value
        double maxSale = currentSales.values().stream()
            .mapToDouble(values -> values[0])
            .max().orElse(0);

        if (maxSale == 0) {
            g2d.setColor(Color.BLACK);
            g2d.drawString("No sales in this period", padding, height / 2);
            return;
        }

        // Draw axes
        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, height - padding, width - padding, height - padding); // X
        g2d.drawLine(padding, padding, padding, height - padding); // Y

        // Draw points and lines
        List<String> months = new ArrayList<>(currentSales.keySet());
        Collections.sort(months);
        int numPoints = months.size();
        
        if (numPoints == 0) return;
        
        int xStep = Math.max(1, (width - 2 * padding) / Math.max(numPoints - 1, 1));
        int x = padding;
        Point prevPoint = null;

        for (String month : months) {
            double[] values = currentSales.get(month);
            double sale = values[0];
            int y = height - padding - (int)((sale / maxSale) * (height - 2 * padding));

            g2d.setColor(Color.BLUE);
            g2d.fillOval(x - 4, y - 4, 8, 8);

            if (prevPoint != null) {
                g2d.drawLine(prevPoint.x, prevPoint.y, x, y);
            }
            prevPoint = new Point(x, y);

            // Draw month label
            g2d.setColor(Color.BLACK);
            String label = month;
            if (label != null && label.length() >= 7) {
                label = label.substring(5, 7) + "/" + label.substring(0, 4); // MM/YYYY
            }
            g2d.rotate(Math.PI / 4, x, height - padding + 20);
            g2d.drawString(label, x - 15, height - padding + 20);
            g2d.rotate(-Math.PI / 4, x, height - padding + 20);

            x += xStep;
        }

        // Draw Y scale
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= 5; i++) {
            int y = height - padding - (i * (height - 2 * padding) / 5);
            double value = (maxSale * i) / 5;
            g2d.drawString(String.format("€ %.0f", value), 5, y + 5);
        }
    }

    private void loadProductsData() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            int months = switch(periodCombo.getSelectedIndex()) {
                case 0 -> 6;
                case 1 -> 12;
                case 2 -> 24;
                default -> 12;
            };

            // FIXED: Properly closed string literal
            String query = "SELECT COALESCE(p.nome, 'Product N/A') as nome, " +
                          "SUM(d.quantita) as quantita_totale, " +
                          "SUM(d.quantita * d.prezzo_unitario) as fatturato, " +
                          "COUNT(DISTINCT o.id) as num_ordini " +
                          "FROM dettagli_ordine d " +
                          "LEFT JOIN prodotti p ON d.prodotto_id = p.id " +
                          "LEFT JOIN ordini o ON d.ordine_id = o.id " +
                          "WHERE o.data_ordine IS NOT NULL " +
                          "AND o.data_ordine >= datetime('now', '-" + months + " months') " +
                          "GROUP BY d.prodotto_id, p.nome " +
                          "ORDER BY fatturato DESC " +
                          "LIMIT 10";

            System.out.println("Loading products data for last " + months + " months");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                double totalRevenue = 0;
                productStats.clear();

                while (rs.next()) {
                    String nome = rs.getString("nome");
                    int quantita = rs.getInt("quantita_totale");
                    double fatturato = rs.getDouble("fatturato");
                    totalRevenue += fatturato;
                    productStats.add(new Object[]{nome, quantita, fatturato});
                }

                System.out.println("Loaded " + productStats.size() + " products, total revenue: €" + totalRevenue);

                // Aggiorna tabella
                Object[][] data = new Object[productStats.size()][4];
                for (int i = 0; i < productStats.size(); i++) {
                    Object[] row = productStats.get(i);
                    data[i][0] = row[0];
                    data[i][1] = row[1];
                    data[i][2] = String.format("€ %.2f", (double)row[2]);
                    if (totalRevenue > 0) {
                        data[i][3] = String.format("%.1f%%", ((double)row[2] / totalRevenue) * 100);
                    } else {
                        data[i][3] = "0.0%";
                    }
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

        int width = productsChartPanel.getWidth();
        int height = productsChartPanel.getHeight();
        int padding = 50;

        if (productStats.isEmpty()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "No product data available";
            int messageWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (width - messageWidth) / 2, height / 2);
            return;
        }

        double totalRevenue = productStats.stream()
            .mapToDouble(row -> (double)row[2])
            .sum();

        if (totalRevenue == 0) {
            g2d.setColor(Color.BLACK);
            g2d.drawString("No revenue data", padding, height / 2);
            return;
        }

        int diameter = Math.min(width - 100, height - 100);
        int centerX = width / 2;
        int centerY = height / 2;

        double startAngle = 0;
        int legendY = padding;

        // Colori predefiniti per i prodotti
        Color[] colors = {
            new Color(255, 99, 132), new Color(54, 162, 235), new Color(255, 205, 86),
            new Color(75, 192, 192), new Color(153, 102, 255), new Color(255, 159, 64),
            new Color(201, 203, 207), new Color(255, 99, 255), new Color(99, 255, 132),
            new Color(132, 99, 255)
        };

        for (int i = 0; i < productStats.size() && i < 10; i++) {
            Object[] row = productStats.get(i);
            String nome = (String)row[0];
            double fatturato = (double)row[2];
            double percentage = (fatturato / totalRevenue);
            double arcAngle = 360 * percentage;

            Color color = colors[i % colors.length];

            g2d.setColor(color);
            g2d.fillArc(centerX - diameter/2, centerY - diameter/2,
                        diameter, diameter,
                        (int)startAngle, (int)arcAngle);

            // Legenda
            if (width > 400) { // Solo se c'è spazio
                g2d.fillRect(width - 200, legendY, 15, 15);
                g2d.setColor(Color.BLACK);
                String label = nome != null && nome.length() > 20 ? nome.substring(0, 17) + "..." : nome;
                g2d.drawString(String.format("%s (%.1f%%)",
                    label, percentage * 100),
                    width - 180, legendY + 12);
                legendY += 20;
            }

            startAngle += arcAngle;
        }
    }
}
