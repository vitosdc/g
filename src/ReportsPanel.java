import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ReportsPanel extends BasePanel {
    private JPanel cardsPanel;
    private CardLayout reportsCardLayout;
    private JButton salesReportButton;
    private JButton advancedStatsButton;
    private JButton warehouseReportButton;
    private JButton backToMenuButton;
    
    // Report components
    private SalesReportComponent salesReportComponent;
    private AdvancedStatsComponent advancedStatsComponent;
    private WarehouseReportComponent warehouseReportComponent;
    
    public ReportsPanel(MainWindow parent) {
        super(parent, "Reports & Analytics");
        setupToolbar();
        setupMainContent();
    }
    
    @Override
    protected void setupToolbar() {
        // Navigation buttons
        salesReportButton = createActionButton("Sales Report", "📊", new Color(50, 205, 50));
        advancedStatsButton = createActionButton("Advanced Statistics", "📈", new Color(30, 144, 255));
        warehouseReportButton = createActionButton("Warehouse Report", "🏪", new Color(184, 134, 11));
        backToMenuButton = createActionButton("Reports Menu", "🏠", new Color(105, 105, 105));
        
        // Action listeners
        salesReportButton.addActionListener(e -> showSalesReport());
        advancedStatsButton.addActionListener(e -> showAdvancedStats());
        warehouseReportButton.addActionListener(e -> showWarehouseReport());
        backToMenuButton.addActionListener(e -> showReportsMenu());
        
        // Initially hide back button
        backToMenuButton.setVisible(false);
        
        // Add to toolbar
        toolbarPanel.add(salesReportButton);
        toolbarPanel.add(advancedStatsButton);
        toolbarPanel.add(warehouseReportButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(backToMenuButton);
    }
    
    @Override
    protected void setupMainContent() {
        reportsCardLayout = new CardLayout();
        cardsPanel = new JPanel(reportsCardLayout);
        
        // Create reports menu
        JPanel menuPanel = createReportsMenu();
        cardsPanel.add(menuPanel, "menu");
        
        // Initialize report components
        salesReportComponent = new SalesReportComponent(this);
        advancedStatsComponent = new AdvancedStatsComponent(this);
        warehouseReportComponent = new WarehouseReportComponent(this);
        
        cardsPanel.add(salesReportComponent, "sales");
        cardsPanel.add(advancedStatsComponent, "stats");
        cardsPanel.add(warehouseReportComponent, "warehouse");
        
        contentPanel.add(cardsPanel, BorderLayout.CENTER);
        
        // Show menu by default
        showReportsMenu();
    }
    
    private JPanel createReportsMenu() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        
        // Title
        JLabel titleLabel = new JLabel("Choose a Report");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(60, 60, 60));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        menuPanel.add(titleLabel, gbc);
        
        // Reset gridwidth
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        
        // Create large report buttons
        JButton largeSalesButton = createLargeReportButton(
            "📊", "Sales Report", 
            "View sales trends, customer analysis and revenue reports",
            new Color(50, 205, 50)
        );
        
        JButton largeStatsButton = createLargeReportButton(
            "📈", "Advanced Statistics", 
            "Detailed analytics with charts and performance metrics",
            new Color(30, 144, 255)
        );
        
        JButton largeWarehouseButton = createLargeReportButton(
            "🏪", "Warehouse Report", 
            "Stock status, movements and inventory analysis",
            new Color(184, 134, 11)
        );
        
        // Add action listeners
        largeSalesButton.addActionListener(e -> showSalesReport());
        largeStatsButton.addActionListener(e -> showAdvancedStats());
        largeWarehouseButton.addActionListener(e -> showWarehouseReport());
        
        // Add to panel
        gbc.gridx = 0;
        menuPanel.add(largeSalesButton, gbc);
        
        gbc.gridx = 1;
        menuPanel.add(largeStatsButton, gbc);
        
        gbc.gridx = 2;
        menuPanel.add(largeWarehouseButton, gbc);
        
        return menuPanel;
    }
    
    private JButton createLargeReportButton(String icon, String title, String description, Color accentColor) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setPreferredSize(new Dimension(280, 200));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        
        // Text panel
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(new Color(60, 60, 60));
        
        JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10));
        
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        button.add(iconLabel, BorderLayout.NORTH);
        button.add(textPanel, BorderLayout.CENTER);
        
        // Default styling
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 2, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        button.setOpaque(true);
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(248, 248, 252));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accentColor, 3, true),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
                iconLabel.setForeground(accentColor);
                titleLabel.setForeground(accentColor);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 2, true),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
                iconLabel.setForeground(Color.BLACK);
                titleLabel.setForeground(new Color(60, 60, 60));
            }
        });
        
        return button;
    }
    
    public void showReportsMenu() {
        reportsCardLayout.show(cardsPanel, "menu");
        backToMenuButton.setVisible(false);
        
        // Update title
        Component[] headerComponents = headerPanel.getComponents();
        for (Component comp : headerComponents) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText("Reports & Analytics");
                break;
            }
        }
    }
    
    public void showSalesReport() {
        reportsCardLayout.show(cardsPanel, "sales");
        backToMenuButton.setVisible(true);
        salesReportComponent.refreshData();
        
        // Update title
        Component[] headerComponents = headerPanel.getComponents();
        for (Component comp : headerComponents) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText("Sales Report");
                break;
            }
        }
    }
    
    public void showAdvancedStats() {
        reportsCardLayout.show(cardsPanel, "stats");
        backToMenuButton.setVisible(true);
        advancedStatsComponent.refreshData();
        
        // Update title
        Component[] headerComponents = headerPanel.getComponents();
        for (Component comp : headerComponents) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText("Advanced Statistics");
                break;
            }
        }
    }
    
    public void showWarehouseReport() {
        reportsCardLayout.show(cardsPanel, "warehouse");
        backToMenuButton.setVisible(true);
        warehouseReportComponent.refreshData();
        
        // Update title
        Component[] headerComponents = headerPanel.getComponents();
        for (Component comp : headerComponents) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText("Warehouse Report");
                break;
            }
        }
    }
    
    @Override
    public void refreshData() {
        // Refresh current visible component
        if (salesReportComponent != null && salesReportComponent.isVisible()) {
            salesReportComponent.refreshData();
        } else if (advancedStatsComponent != null && advancedStatsComponent.isVisible()) {
            advancedStatsComponent.refreshData();
        } else if (warehouseReportComponent != null && warehouseReportComponent.isVisible()) {
            warehouseReportComponent.refreshData();
        }
    }
    
    // Inner classes for report components
    private class SalesReportComponent extends JPanel {
        private ReportsPanel parent;
        
        public SalesReportComponent(ReportsPanel parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            
            // Create embedded sales report button
            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.setBackground(Color.WHITE);
            
            JButton openSalesReportButton = new JButton("Open Sales Report Window");
            openSalesReportButton.setPreferredSize(new Dimension(250, 60));
            openSalesReportButton.setFont(new Font("Arial", Font.BOLD, 14));
            openSalesReportButton.setBackground(new Color(50, 205, 50));
            openSalesReportButton.setForeground(Color.WHITE);
            openSalesReportButton.setBorder(BorderFactory.createRaisedBorderBorder());
            openSalesReportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            openSalesReportButton.addActionListener(e -> {
                // FIXED: Use parentWindow directly instead of getParentWindow()
                SalesReportWindow salesWindow = new SalesReportWindow(parentWindow);
                salesWindow.setVisible(true);
            });
            
            centerPanel.add(openSalesReportButton);
            add(centerPanel, BorderLayout.CENTER);
        }
        
        public void refreshData() {
            // Refresh the sales report data if needed
        }
    }
    
    private class AdvancedStatsComponent extends JPanel {
        private ReportsPanel parent;
        
        public AdvancedStatsComponent(ReportsPanel parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            
            // Create embedded advanced stats button
            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.setBackground(Color.WHITE);
            
            JButton openAdvancedStatsButton = new JButton("Open Advanced Statistics Window");
            openAdvancedStatsButton.setPreferredSize(new Dimension(280, 60));
            openAdvancedStatsButton.setFont(new Font("Arial", Font.BOLD, 14));
            openAdvancedStatsButton.setBackground(new Color(30, 144, 255));
            openAdvancedStatsButton.setForeground(Color.WHITE);
            openAdvancedStatsButton.setBorder(BorderFactory.createRaisedBorderBorder());
            openAdvancedStatsButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            openAdvancedStatsButton.addActionListener(e -> {
                // FIXED: Use parentWindow directly
                AdvancedStatsWindow statsWindow = new AdvancedStatsWindow(parentWindow);
                statsWindow.setVisible(true);
            });
            
            centerPanel.add(openAdvancedStatsButton);
            add(centerPanel, BorderLayout.CENTER);
        }
        
        public void refreshData() {
            // Refresh stats data
        }
    }
    
    private class WarehouseReportComponent extends JPanel {
        private ReportsPanel parent;
        
        public WarehouseReportComponent(ReportsPanel parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            
            // Create embedded warehouse report button
            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.setBackground(Color.WHITE);
            
            JButton openWarehouseReportButton = new JButton("Open Warehouse Report Window");
            openWarehouseReportButton.setPreferredSize(new Dimension(280, 60));
            openWarehouseReportButton.setFont(new Font("Arial", Font.BOLD, 14));
            openWarehouseReportButton.setBackground(new Color(184, 134, 11));
            openWarehouseReportButton.setForeground(Color.WHITE);
            openWarehouseReportButton.setBorder(BorderFactory.createRaisedBorderBorder());
            openWarehouseReportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            openWarehouseReportButton.addActionListener(e -> {
                // FIXED: Use parentWindow directly
                WarehouseReportWindow warehouseWindow = new WarehouseReportWindow(parentWindow);
                warehouseWindow.setVisible(true);
            });
            
            centerPanel.add(openWarehouseReportButton);
            add(centerPanel, BorderLayout.CENTER);
        }
        
        public void refreshData() {
            // Refresh warehouse data
        }
    }
}