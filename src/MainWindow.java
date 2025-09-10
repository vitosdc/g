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
    }
    
    private void setupWindow() {
        setTitle("Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
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
        backupItem.addActionListener(e -> openBackupConfig());
        toolsMenu.add(backupItem);
        
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
        JLabel welcomeLabel = new JLabel("Welcome to the Management System");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel);
        
        // System information panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel versionLabel = new JLabel("Version 1.0");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(versionLabel);
        
        // Dashboard with quick buttons
        JPanel dashboardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create buttons with icons (using emojis as placeholders)
        JButton newOrderButton = new JButton("📝 New Order");
        JButton newCustomerButton = new JButton("👥 New Customer");
        JButton newProductButton = new JButton("📦 New Product");
        JButton newInvoiceButton = new JButton("📋 New Invoice");
        JButton suppliersButton = new JButton("🏭 Suppliers");
        JButton reportButton = new JButton("📊 Sales Report");
        JButton warehouseButton = new JButton("🏪 Warehouse");
        JButton statsButton = new JButton("📈 Statistics");
        JButton backupButton = new JButton("💾 Backup");
        
        // Button style
        Dimension buttonSize = new Dimension(150, 80);
        Font buttonFont = new Font("Arial", Font.PLAIN, 14);
        
        Component[] buttons = {
            newOrderButton, newCustomerButton, newProductButton,
            newInvoiceButton, suppliersButton, warehouseButton, 
            statsButton, reportButton, backupButton
        };
        
        for (Component button : buttons) {
            button.setPreferredSize(buttonSize);
            ((JButton)button).setFont(buttonFont);
        }
        
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
    
    private void openCustomersWindow() {
        try {
            CustomersWindow customersWindow = new CustomersWindow(this);
            customersWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening the customers window: " + e.getMessage(),
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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
                "Error",
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