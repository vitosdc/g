import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JMenuBar menuBar;
    private JPanel mainPanel;
    
    public MainWindow() {
        setupWindow();
        setupMenuBar();
        setupMainPanel();
        
        // Initialize the database with loading indicator
        initializeDatabaseAsync();
    }
    
    private void setupWindow() {
        setTitle("WorkGenio - Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        
        // Set application icon if available
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/workgenio-icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // Icon not found, continue without it
        }
        
        // Add shutdown management to correctly close the database
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                shutdownApplication();
            }
        });
        
        // Show splash screen effect
        showSplashEffect();
    }
    
    private void showSplashEffect() {
        // Create temporary splash panel
        JPanel splashPanel = new JPanel(new BorderLayout());
        splashPanel.setBackground(new Color(245, 245, 245));
        
        JLabel logoLabel = new JLabel("WorkGenio", JLabel.CENTER);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 48));
        logoLabel.setForeground(new Color(34, 139, 34));
        
        JLabel loadingLabel = new JLabel("Loading...", JLabel.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        loadingLabel.setForeground(Color.GRAY);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(logoLabel, BorderLayout.CENTER);
        centerPanel.add(loadingLabel, BorderLayout.SOUTH);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(100, 50, 50, 50));
        
        splashPanel.add(centerPanel, BorderLayout.CENTER);
        splashPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(splashPanel);
    }
    
    private void initializeDatabaseAsync() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Simulate loading time for better UX
                    Thread.sleep(1500);
                    DatabaseManager.getInstance().initDatabase();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "Error during database initialization:\n" + e.getMessage(),
                            "WorkGenio - Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    Boolean success = get();
                    if (success) {
                        // Remove splash and show main interface
                        getContentPane().removeAll();
                        setupMenuBar();
                        setupMainPanel();
                        revalidate();
                        repaint();
                        
                        NotificationManager.showSuccess("WorkGenio initialized successfully");
                    } else {
                        // Show error and exit
                        System.exit(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
        
        worker.execute();
    }
    
    private void shutdownApplication() {
        GlobalLoadingManager.showLoading("Closing application...");
        
        SwingWorker<Void, Void> shutdownWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Perform auto backup if enabled
                    BackupManager backupManager = BackupManager.getInstance();
                    if (backupManager.isAutoBackupEnabled()) {
                        try {
                            backupManager.performBackup();
                        } catch (Exception e) {
                            System.err.println("Auto-backup failed on shutdown: " + e.getMessage());
                        }
                    }
                    
                    // Close database connection
                    DatabaseManager.getInstance().closeConnection();
                    
                    Thread.sleep(500); // Give time for operations to complete
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                System.exit(0);
            }
        };
        
        shutdownWorker.execute();
    }
    
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        
        JMenuItem backupItem = new JMenuItem("Create Backup");
        backupItem.setMnemonic('B');
        backupItem.addActionListener(e -> performQuickBackup());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('x');
        exitItem.addActionListener(e -> shutdownApplication());
        
        fileMenu.add(backupItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Management Menu
        JMenu managementMenu = new JMenu("Management");
        managementMenu.setMnemonic('M');
        
        JMenuItem customersItem = new JMenuItem("Customers");
        JMenuItem productsItem = new JMenuItem("Products");
        JMenuItem ordersItem = new JMenuItem("Orders");
        JMenuItem invoicesItem = new JMenuItem("Invoices");
        JMenuItem suppliersItem = new JMenuItem("Suppliers");
        JMenuItem warehouseItem = new JMenuItem("Warehouse");
        
        // Set mnemonics
        customersItem.setMnemonic('C');
        productsItem.setMnemonic('P');
        ordersItem.setMnemonic('O');
        invoicesItem.setMnemonic('I');
        suppliersItem.setMnemonic('S');
        warehouseItem.setMnemonic('W');
        
        // Add action listeners with error handling
        customersItem.addActionListener(e -> openWindowSafely(() -> openCustomersWindow()));
        productsItem.addActionListener(e -> openWindowSafely(() -> openProductsWindow()));
        ordersItem.addActionListener(e -> openWindowSafely(() -> openOrdersWindow()));
        invoicesItem.addActionListener(e -> openWindowSafely(() -> openInvoicesWindow()));
        suppliersItem.addActionListener(e -> openWindowSafely(() -> openSuppliersWindow()));
        warehouseItem.addActionListener(e -> openWindowSafely(() -> openWarehouseWindow()));
        
        managementMenu.add(customersItem);
        managementMenu.add(productsItem);
        managementMenu.add(ordersItem);
        managementMenu.add(invoicesItem);
        managementMenu.add(suppliersItem);
        managementMenu.add(warehouseItem);
        
        // Reports Menu
        JMenu reportMenu = new JMenu("Reports");
        reportMenu.setMnemonic('R');
        
        JMenuItem salesReportItem = new JMenuItem("Sales Report");
        JMenuItem advancedStatsItem = new JMenuItem("Advanced Statistics");
        JMenuItem warehouseReportItem = new JMenuItem("Warehouse Report");
        
        salesReportItem.addActionListener(e -> openWindowSafely(() -> openSalesReport()));
        advancedStatsItem.addActionListener(e -> openWindowSafely(() -> openAdvancedStats()));
        warehouseReportItem.addActionListener(e -> openWindowSafely(() -> openWarehouseReport()));
        
        reportMenu.add(salesReportItem);
        reportMenu.add(advancedStatsItem);
        reportMenu.add(warehouseReportItem);
        
        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic('T');
        
        JMenuItem backupConfigItem = new JMenuItem("Backup Configuration");
        backupConfigItem.addActionListener(e -> openWindowSafely(() -> openBackupConfig()));
        toolsMenu.add(backupConfigItem);
        
        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        
        JMenuItem aboutItem = new JMenuItem("About WorkGenio");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(managementMenu);
        menuBar.add(reportMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    // Wrapper method for safe window opening with error handling
    private void openWindowSafely(Runnable windowOpener) {
        try {
            windowOpener.run();
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening window: " + e.getMessage());
        }
    }
    
    private void performQuickBackup() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                GlobalLoadingManager.showLoading("Creating backup...");
                
                try {
                    BackupManager.getInstance().performBackup();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                GlobalLoadingManager.hideLoading();
                try {
                    Boolean success = get();
                    if (success) {
                        NotificationManager.showSuccess("Backup created successfully");
                    } else {
                        NotificationManager.showError("Backup failed");
                    }
                } catch (Exception e) {
                    NotificationManager.showError("Error during backup: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    private void showAboutDialog() {
        String aboutText = """
            <html>
            <div style='text-align: center; font-family: Arial;'>
                <h2 style='color: #228B22;'>WorkGenio</h2>
                <h3>Professional Business Management System</h3>
                <br>
                <p><b>Version:</b> 2.0 Enhanced</p>
                <p><b>Build:</b> 2024.01</p>
                <br>
                <p>Complete ERP solution for small and medium businesses</p>
                <p>Manage customers, products, orders, invoices, and warehouse operations</p>
                <br>
                <p style='color: #666; font-size: 12px;'>
                    © 2024 WorkGenio Systems. All rights reserved.
                </p>
            </div>
            </html>
        """;
        
        JOptionPane.showMessageDialog(this, aboutText, "About WorkGenio", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void setupMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(248, 249, 250));
        
        // Welcome panel with improved design
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new GridBagLayout());
        welcomePanel.setBackground(new Color(248, 249, 250));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        JLabel welcomeLabel = new JLabel("Welcome to WorkGenio");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 36));
        welcomeLabel.setForeground(new Color(34, 139, 34));
        
        JLabel subtitleLabel = new JLabel("Professional Business Management System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subtitleLabel.setForeground(new Color(105, 105, 105));
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        welcomePanel.add(welcomeLabel, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 30, 0);
        welcomePanel.add(subtitleLabel, gbc);
        
        // Quick stats panel
        JPanel statsPanel = createQuickStatsPanel();
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        welcomePanel.add(statsPanel, gbc);
        
        // Dashboard with improved buttons
        JPanel dashboardPanel = new JPanel(new GridLayout(3, 3, 20, 20));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 60, 30, 60));
        dashboardPanel.setBackground(new Color(248, 249, 250));
        
        // Create enhanced dashboard buttons
        JButton newOrderButton = createDashboardButton("📝", "New Order", new Color(70, 130, 180));
        JButton newCustomerButton = createDashboardButton("👥", "New Customer", new Color(34, 139, 34));
        JButton newProductButton = createDashboardButton("📦", "New Product", new Color(255, 140, 0));
        JButton newInvoiceButton = createDashboardButton("📋", "New Invoice", new Color(220, 20, 60));
        JButton suppliersButton = createDashboardButton("🏭", "Suppliers", new Color(138, 43, 226));
        JButton warehouseButton = createDashboardButton("🏪", "Warehouse", new Color(184, 134, 11));
        JButton statsButton = createDashboardButton("📈", "Statistics", new Color(30, 144, 255));
        JButton reportButton = createDashboardButton("📊", "Sales Report", new Color(50, 205, 50));
        JButton backupButton = createDashboardButton("💾", "Backup", new Color(105, 105, 105));
        
        // Enhanced action listeners with error handling
        newOrderButton.addActionListener(e -> openWindowSafely(() -> openOrdersWindow()));
        newCustomerButton.addActionListener(e -> openWindowSafely(() -> openCustomersWindow()));
        newProductButton.addActionListener(e -> openWindowSafely(() -> openProductsWindow()));
        newInvoiceButton.addActionListener(e -> openWindowSafely(() -> openInvoicesWindow()));
        suppliersButton.addActionListener(e -> openWindowSafely(() -> openSuppliersWindow()));
        warehouseButton.addActionListener(e -> openWindowSafely(() -> openWarehouseWindow()));
        reportButton.addActionListener(e -> openWindowSafely(() -> openSalesReport()));
        statsButton.addActionListener(e -> openWindowSafely(() -> openAdvancedStats()));
        backupButton.addActionListener(e -> openWindowSafely(() -> openBackupConfig()));
        
        // Add buttons to dashboard
        dashboardPanel.add(newOrderButton);
        dashboardPanel.add(newCustomerButton);
        dashboardPanel.add(newProductButton);
        dashboardPanel.add(newInvoiceButton);
        dashboardPanel.add(suppliersButton);
        dashboardPanel.add(warehouseButton);
        dashboardPanel.add(statsButton);
        dashboardPanel.add(reportButton);
        dashboardPanel.add(backupButton);
        
        // System information panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        infoPanel.setBackground(new Color(248, 249, 250));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        JLabel versionLabel = new JLabel("WorkGenio v2.0 Enhanced - Professional Business Management");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        versionLabel.setForeground(new Color(70, 130, 180));
        infoPanel.add(versionLabel);
        
        // Assembly
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        mainPanel.add(dashboardPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createQuickStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(new Color(248, 249, 250));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Create stat cards
        JPanel ordersCard = createStatCard("📋", "Orders", "Loading...", new Color(70, 130, 180));
        JPanel customersCard = createStatCard("👥", "Customers", "Loading...", new Color(34, 139, 34));
        JPanel productsCard = createStatCard("📦", "Products", "Loading...", new Color(255, 140, 0));
        JPanel lowStockCard = createStatCard("⚠️", "Low Stock", "Loading...", new Color(220, 20, 60));
        
        statsPanel.add(ordersCard);
        statsPanel.add(customersCard);
        statsPanel.add(productsCard);
        statsPanel.add(lowStockCard);
        
        // Load stats asynchronously
        loadQuickStats(ordersCard, customersCard, productsCard, lowStockCard);
        
        return statsPanel;
    }
    
    private JPanel createStatCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel iconLabel = new JLabel(icon, JLabel.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(color);
        
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        valueLabel.setName("value"); // For easy access when updating
        
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(iconLabel);
        centerPanel.add(titleLabel);
        centerPanel.add(valueLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void loadQuickStats(JPanel ordersCard, JPanel customersCard, JPanel productsCard, JPanel lowStockCard) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private int ordersCount = 0;
            private int customersCount = 0;
            private int productsCount = 0;
            private int lowStockCount = 0;
            
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    
                    // Get orders count (active orders)
                    String ordersQuery = "SELECT COUNT(*) FROM ordini WHERE stato IN ('New', 'In Progress')";
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(ordersQuery)) {
                        if (rs.next()) ordersCount = rs.getInt(1);
                    }
                    
                    // Get customers count
                    String customersQuery = "SELECT COUNT(*) FROM clienti";
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(customersQuery)) {
                        if (rs.next()) customersCount = rs.getInt(1);
                    }
                    
                    // Get products count
                    String productsQuery = "SELECT COUNT(*) FROM prodotti";
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(productsQuery)) {
                        if (rs.next()) productsCount = rs.getInt(1);
                    }
                    
                    // Get low stock products count
                    String lowStockQuery = """
                        SELECT COUNT(*) FROM prodotti p
                        JOIN scorte_minime sm ON p.id = sm.prodotto_id
                        WHERE p.quantita < sm.quantita_minima
                    """;
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(lowStockQuery)) {
                        if (rs.next()) lowStockCount = rs.getInt(1);
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                // Update UI with loaded stats
                updateStatCard(ordersCard, String.valueOf(ordersCount));
                updateStatCard(customersCard, String.valueOf(customersCount));
                updateStatCard(productsCard, String.valueOf(productsCount));
                updateStatCard(lowStockCard, String.valueOf(lowStockCount));
                
                // Show warning if there are low stock items
                if (lowStockCount > 0) {
                    NotificationManager.showLowStockWarning("Multiple products", lowStockCount);
                }
            }
        };
        
        worker.execute();
    }
    
    private void updateStatCard(JPanel card, String value) {
        Component[] components = ((JPanel)card.getComponent(0)).getComponents();
        for (Component component : components) {
            if (component instanceof JLabel && "value".equals(component.getName())) {
                ((JLabel)component).setText(value);
                break;
            }
        }
    }
    
    private JButton createDashboardButton(String icon, String text, Color color) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(10, 10));
        button.setPreferredSize(new Dimension(220, 140));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon, JLabel.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        
        // Text
        JLabel textLabel = new JLabel(text, JLabel.CENTER);
        textLabel.setFont(new Font("Arial", Font.BOLD, 16));
        textLabel.setForeground(color);
        
        // Assembly
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(iconLabel, BorderLayout.CENTER);
        contentPanel.add(textLabel, BorderLayout.SOUTH);
        
        button.add(contentPanel);
        
        // Enhanced styling with gradient border
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.brighter(), 3),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Enhanced hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                textLabel.setForeground(Color.WHITE);
                iconLabel.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.darker(), 3),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                textLabel.setForeground(color);
                iconLabel.setForeground(Color.BLACK);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.brighter(), 3),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
                ));
            }
        });
        
        return button;
    }
    
    // Window opening methods with enhanced error handling
    private void openCustomersWindow() {
        try {
            CustomersWindow customersWindow = new CustomersWindow(this);
            customersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening customers window: " + e.getMessage());
        }
    }
    
    private void openProductsWindow() {
        try {
            ProductsWindow productsWindow = new ProductsWindow(this);
            productsWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening products window: " + e.getMessage());
        }
    }
    
    private void openOrdersWindow() {
        try {
            OrdersWindow ordersWindow = new OrdersWindow(this);
            ordersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening orders window: " + e.getMessage());
        }
    }
    
    private void openInvoicesWindow() {
        try {
            InvoicesWindow invoicesWindow = new InvoicesWindow(this);
            invoicesWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening invoices window: " + e.getMessage());
        }
    }
    
    private void openSuppliersWindow() {
        try {
            SuppliersWindow suppliersWindow = new SuppliersWindow(this);
            suppliersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening suppliers window: " + e.getMessage());
        }
    }
    
    private void openWarehouseWindow() {
        try {
            WarehouseWindow warehouseWindow = new WarehouseWindow(this);
            warehouseWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening warehouse window: " + e.getMessage());
        }
    }
    
    private void openSalesReport() {
        try {
            SalesReportWindow reportWindow = new SalesReportWindow(this);
            reportWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening sales report: " + e.getMessage());
        }
    }
    
    private void openAdvancedStats() {
        try {
            AdvancedStatsWindow statsWindow = new AdvancedStatsWindow(this);
            statsWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening statistics: " + e.getMessage());
        }
    }
    
    private void openWarehouseReport() {
        try {
            WarehouseReportWindow reportWindow = new WarehouseReportWindow(this);
            reportWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening warehouse report: " + e.getMessage());
        }
    }
    
    private void openBackupConfig() {
        try {
            BackupConfigWindow backupWindow = new BackupConfigWindow(this);
            backupWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showError("Error opening backup configuration: " + e.getMessage());
        }
    }
}