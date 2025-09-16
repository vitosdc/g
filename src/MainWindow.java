import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JMenuBar menuBar;
    private JPanel mainPanel;
    
    public MainWindow() {
        setupWindow();
        setupMenuBar();
        setupMainPanel();
        
        // Initialize the database
        DatabaseManager.getInstance().initDatabase();
        
        // Apply global settings on startup
        SettingsWindow.loadGlobalSettings();
        SettingsWindow.applyGlobalSettings();
    }
    
    private void setupWindow() {
        setTitle("WorkGenio - Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850); // Aumentata la dimensione della finestra
        setLocationRelativeTo(null);
        
        // Add shutdown management to correctly close the database
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DatabaseManager.getInstance().closeConnection();
            }
        });
    }
    
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            DatabaseManager.getInstance().closeConnection();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        
        // Management Menu
        JMenu managementMenu = new JMenu("Management");
        JMenuItem clientiItem = new JMenuItem("Customers");
        JMenuItem prodottiItem = new JMenuItem("Products");
        JMenuItem ordiniItem = new JMenuItem("Orders");
        JMenuItem fattureItem = new JMenuItem("Invoices");
        JMenuItem fornitoriItem = new JMenuItem("Suppliers");
        JMenuItem magazzinoItem = new JMenuItem("Warehouse");
        
        // Add action listener for menu items
        clientiItem.addActionListener(e -> openCustomersWindow());
        prodottiItem.addActionListener(e -> openProductsWindow());
        ordiniItem.addActionListener(e -> openOrdersWindow());
        fattureItem.addActionListener(e -> openInvoicesWindow());
        fornitoriItem.addActionListener(e -> openSuppliersWindow());
        magazzinoItem.addActionListener(e -> openWarehouseWindow());
        
        managementMenu.add(clientiItem);
        managementMenu.add(prodottiItem);
        managementMenu.add(ordiniItem);
        managementMenu.add(fattureItem);
        managementMenu.add(fornitoriItem);
        managementMenu.add(magazzinoItem);
        
        // Reports Menu
        JMenu reportMenu = new JMenu("Reports");
        JMenuItem salesReportItem = new JMenuItem("Sales Report");
        JMenuItem advancedStatsItem = new JMenuItem("Advanced Statistics");
        JMenuItem warehouseReportItem = new JMenuItem("Warehouse Report");
        
        salesReportItem.addActionListener(e -> openSalesReport());
        advancedStatsItem.addActionListener(e -> openAdvancedStats());
        warehouseReportItem.addActionListener(e -> openWarehouseReport());
        
        reportMenu.add(salesReportItem);
        reportMenu.add(advancedStatsItem);
        reportMenu.add(warehouseReportItem);
        
        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem backupItem = new JMenuItem("Backup");
        JMenuItem settingsItem = new JMenuItem("Settings");
        
        backupItem.addActionListener(e -> openBackupConfig());
        settingsItem.addActionListener(e -> openSettings());
        
        toolsMenu.add(backupItem);
        toolsMenu.add(settingsItem);
        
        menuBar.add(fileMenu);
        menuBar.add(managementMenu);
        menuBar.add(reportMenu);
        menuBar.add(toolsMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void setupMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        // Welcome panel
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new GridBagLayout());
        JLabel welcomeLabel = new JLabel("Welcome to WorkGenio");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32)); // Aumentato font size
        welcomeLabel.setForeground(new Color(34, 139, 34)); // Verde professionale
        
        JLabel subtitleLabel = new JLabel("A Management System");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 18)); // Sottotitolo più grande
        subtitleLabel.setForeground(new Color(105, 105, 105)); // Grigio elegante
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 0, 0);
        welcomePanel.add(welcomeLabel, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 20, 0);
        welcomePanel.add(subtitleLabel, gbc);
        
        // System information panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel versionLabel = new JLabel("WorkGenio v1.0 - Professional Business Management");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 14)); // Font più grande
        versionLabel.setForeground(new Color(70, 130, 180)); // Blu acciaio
        infoPanel.add(versionLabel);
        
        // Dashboard with quick buttons
        JPanel dashboardPanel = new JPanel(new GridLayout(3, 3, 15, 15)); // Aumentato spacing
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40)); // Più padding
        
        // Create buttons with larger icons and text
        JButton newOrderButton = createDashboardButton("📝", "New Order", new Color(70, 130, 180));
        JButton newCustomerButton = createDashboardButton("👥", "New Customer", new Color(34, 139, 34));
        JButton newProductButton = createDashboardButton("📦", "New Product", new Color(255, 140, 0));
        JButton newInvoiceButton = createDashboardButton("📋", "New Invoice", new Color(220, 20, 60));
        JButton suppliersButton = createDashboardButton("🏭", "Suppliers", new Color(138, 43, 226));
        JButton warehouseButton = createDashboardButton("🏪", "Warehouse", new Color(184, 134, 11));
        JButton statsButton = createDashboardButton("📈", "Statistics", new Color(30, 144, 255));
        JButton reportButton = createDashboardButton("📊", "Sales Report", new Color(50, 205, 50));
        JButton backupButton = createDashboardButton("💾", "Backup", new Color(105, 105, 105));
        
        // Action listeners
        newOrderButton.addActionListener(e -> openOrdersWindow());
        newCustomerButton.addActionListener(e -> openCustomersWindow());
        newProductButton.addActionListener(e -> openProductsWindow());
        newInvoiceButton.addActionListener(e -> openInvoicesWindow());
        suppliersButton.addActionListener(e -> openSuppliersWindow());
        warehouseButton.addActionListener(e -> openWarehouseWindow());
        reportButton.addActionListener(e -> openSalesReport());
        statsButton.addActionListener(e -> openAdvancedStats());
        backupButton.addActionListener(e -> openBackupConfig());
        
        // Add buttons to the panel
        dashboardPanel.add(newOrderButton);
        dashboardPanel.add(newCustomerButton);
        dashboardPanel.add(newProductButton);
        dashboardPanel.add(newInvoiceButton);
        dashboardPanel.add(suppliersButton);
        dashboardPanel.add(warehouseButton);
        dashboardPanel.add(statsButton);
        dashboardPanel.add(reportButton);
        dashboardPanel.add(backupButton);
        
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        mainPanel.add(dashboardPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    // Metodo per creare pulsanti della dashboard con stile uniforme
    private JButton createDashboardButton(String icon, String text, Color color) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        
        // Icona grande
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48)); // Icona molto più grande
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Testo
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Testo più grande e grassetto
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        button.add(iconLabel, BorderLayout.CENTER);
        button.add(textLabel, BorderLayout.SOUTH);
        
        // Styling del pulsante
        button.setPreferredSize(new Dimension(200, 120)); // Pulsanti più grandi
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Effetti hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                textLabel.setForeground(Color.WHITE);
                iconLabel.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                textLabel.setForeground(Color.BLACK);
                iconLabel.setForeground(Color.BLACK);
            }
        });
        
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    private void openCustomersWindow() {
        try {
            CustomersWindow customersWindow = new CustomersWindow(this);
            customersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the customers window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openProductsWindow() {
        try {
            ProductsWindow productsWindow = new ProductsWindow(this);
            productsWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the products window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openOrdersWindow() {
        try {
            OrdersWindow ordersWindow = new OrdersWindow(this);
            ordersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the orders window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openInvoicesWindow() {
        try {
            InvoicesWindow invoicesWindow = new InvoicesWindow(this);
            invoicesWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the invoices window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openSuppliersWindow() {
        try {
            SuppliersWindow suppliersWindow = new SuppliersWindow(this);
            suppliersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the suppliers window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openWarehouseWindow() {
        try {
            WarehouseWindow warehouseWindow = new WarehouseWindow(this);
            warehouseWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the warehouse window: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openSalesReport() {
        try {
            SalesReportWindow reportWindow = new SalesReportWindow(this);
            reportWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the sales report: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openAdvancedStats() {
        try {
            AdvancedStatsWindow statsWindow = new AdvancedStatsWindow(this);
            statsWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the statistics: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openWarehouseReport() {
        try {
            WarehouseReportWindow reportWindow = new WarehouseReportWindow(this);
            reportWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the warehouse report: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openBackupConfig() {
        try {
            BackupConfigWindow backupWindow = new BackupConfigWindow(this);
            backupWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the backup configuration: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void openSettings() {
        try {
            SettingsWindow settingsWindow = new SettingsWindow(this);
            settingsWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening settings: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}