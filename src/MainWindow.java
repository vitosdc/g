import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainWindow extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JToolBar toolBar;
    
    // Panel instances
    private CustomersPanel customersPanel;
    private ProductsPanel productsPanel;
    private OrdersPanel ordersPanel;
    private InvoicesPanel invoicesPanel;
    private SuppliersPanel suppliersPanel;
    private WarehousePanel warehousePanel;
    private SalesReportPanel salesReportPanel;
    private AdvancedStatsPanel advancedStatsPanel;
    private WarehouseReportPanel warehouseReportPanel;
    
    // Current panel name for reference
    private String currentPanel = "HOME";
    
    public MainWindow() {
        setupWindow();
        setupToolBar();
        setupMainPanel();
        setupPanels();
        
        // Initialize the database
        DatabaseManager.getInstance().initDatabase();
        
        // Apply global settings on startup
        SettingsWindow.loadGlobalSettings();
        SettingsWindow.applyGlobalSettings();
    }
    
    private void setupWindow() {
        setTitle("WorkGenio - Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Add shutdown management to correctly close the database
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DatabaseManager.getInstance().closeConnection();
            }
        });
    }
    
    private void setupToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
        // Home button
        JButton homeButton = createToolBarButton("🏠", "Home", "Go to Dashboard");
        homeButton.addActionListener(e -> showPanel("HOME"));
        toolBar.add(homeButton);
        
        toolBar.addSeparator();
        
        // Management buttons
        JButton customersButton = createToolBarButton("👥", "Customers", "Customer Management");
        customersButton.addActionListener(e -> showPanel("CUSTOMERS"));
        toolBar.add(customersButton);
        
        JButton productsButton = createToolBarButton("📦", "Products", "Product Management");
        productsButton.addActionListener(e -> showPanel("PRODUCTS"));
        toolBar.add(productsButton);
        
        JButton ordersButton = createToolBarButton("📝", "Orders", "Order Management");
        ordersButton.addActionListener(e -> showPanel("ORDERS"));
        toolBar.add(ordersButton);
        
        JButton invoicesButton = createToolBarButton("📋", "Invoices", "Invoice Management");
        invoicesButton.addActionListener(e -> showPanel("INVOICES"));
        toolBar.add(invoicesButton);
        
        JButton suppliersButton = createToolBarButton("🏭", "Suppliers", "Supplier Management");
        suppliersButton.addActionListener(e -> showPanel("SUPPLIERS"));
        toolBar.add(suppliersButton);
        
        JButton warehouseButton = createToolBarButton("🏪", "Warehouse", "Warehouse Management");
        warehouseButton.addActionListener(e -> showPanel("WAREHOUSE"));
        toolBar.add(warehouseButton);
        
        toolBar.addSeparator();
        
        // Reports buttons
        JButton salesReportButton = createToolBarButton("📊", "Sales Report", "Sales Analysis");
        salesReportButton.addActionListener(e -> showPanel("SALES_REPORT"));
        toolBar.add(salesReportButton);
        
        JButton statsButton = createToolBarButton("📈", "Statistics", "Advanced Statistics");
        statsButton.addActionListener(e -> showPanel("ADVANCED_STATS"));
        toolBar.add(statsButton);
        
        JButton warehouseReportButton = createToolBarButton("📋", "Warehouse Report", "Warehouse Analysis");
        warehouseReportButton.addActionListener(e -> showPanel("WAREHOUSE_REPORT"));
        toolBar.add(warehouseReportButton);
        
        toolBar.addSeparator();
        
        // Tools buttons
        JButton backupButton = createToolBarButton("💾", "Backup", "Backup Configuration");
        backupButton.addActionListener(e -> openBackupConfig());
        toolBar.add(backupButton);
        
        JButton settingsButton = createToolBarButton("⚙️", "Settings", "Application Settings");
        settingsButton.addActionListener(e -> openSettings());
        toolBar.add(settingsButton);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private JButton createToolBarButton(String icon, String text, String tooltip) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Text
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        button.add(iconLabel, BorderLayout.CENTER);
        button.add(textLabel, BorderLayout.SOUTH);
        
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(80, 60));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(true);
                button.setBackground(new Color(230, 230, 230));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });
        
        return button;
    }
    
    private void setupMainPanel() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create home panel (dashboard)
        JPanel homePanel = createHomePanel();
        mainPanel.add(homePanel, "HOME");
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHomePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        // Welcome panel
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new GridBagLayout());
        JLabel welcomeLabel = new JLabel("Welcome to WorkGenio");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        welcomeLabel.setForeground(new Color(34, 139, 34));
        
        JLabel subtitleLabel = new JLabel("Professional Business Management System");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        subtitleLabel.setForeground(new Color(105, 105, 105));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 0, 0);
        welcomePanel.add(welcomeLabel, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 20, 0);
        welcomePanel.add(subtitleLabel, gbc);
        
        // Quick stats or info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel versionLabel = new JLabel("WorkGenio v1.0 - Use the toolbar above to navigate between modules");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        versionLabel.setForeground(new Color(70, 130, 180));
        infoPanel.add(versionLabel);
        
        panel.add(welcomePanel, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupPanels() {
        // Initialize panels lazily to improve startup time
        // Panels will be created when first accessed
    }
    
    private void showPanel(String panelName) {
        try {
            // Create panel if it doesn't exist
            if (!isPanelCreated(panelName)) {
                createPanel(panelName);
            }
            
            // Switch to the panel
            cardLayout.show(mainPanel, panelName);
            currentPanel = panelName;
            
            // Update window title
            updateWindowTitle(panelName);
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening " + panelName + ": " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean isPanelCreated(String panelName) {
        for (Component comp : mainPanel.getComponents()) {
            if (panelName.equals(comp.getName())) {
                return true;
            }
        }
        return false;
    }
    
    private void createPanel(String panelName) {
        JPanel panel = null;
        
        switch (panelName) {
            case "CUSTOMERS":
                if (customersPanel == null) {
                    customersPanel = new CustomersPanel();
                }
                panel = customersPanel;
                break;
                
            case "PRODUCTS":
                if (productsPanel == null) {
                    productsPanel = new ProductsPanel();
                }
                panel = productsPanel;
                break;
                
            case "ORDERS":
                if (ordersPanel == null) {
                    ordersPanel = new OrdersPanel();
                }
                panel = ordersPanel;
                break;
                
            case "INVOICES":
                if (invoicesPanel == null) {
                    invoicesPanel = new InvoicesPanel();
                }
                panel = invoicesPanel;
                break;
                
            case "SUPPLIERS":
                if (suppliersPanel == null) {
                    suppliersPanel = new SuppliersPanel();
                }
                panel = suppliersPanel;
                break;
                
            case "WAREHOUSE":
                if (warehousePanel == null) {
                    warehousePanel = new WarehousePanel();
                }
                panel = warehousePanel;
                break;
                
            case "SALES_REPORT":
                if (salesReportPanel == null) {
                    salesReportPanel = new SalesReportPanel();
                }
                panel = salesReportPanel;
                break;
                
            case "ADVANCED_STATS":
                if (advancedStatsPanel == null) {
                    advancedStatsPanel = new AdvancedStatsPanel();
                }
                panel = advancedStatsPanel;
                break;
                
            case "WAREHOUSE_REPORT":
                if (warehouseReportPanel == null) {
                    warehouseReportPanel = new WarehouseReportPanel();
                }
                panel = warehouseReportPanel;
                break;
        }
        
        if (panel != null) {
            panel.setName(panelName);
            mainPanel.add(panel, panelName);
        }
    }
    
    private void updateWindowTitle(String panelName) {
        String title = "WorkGenio - ";
        switch (panelName) {
            case "HOME": title += "Dashboard"; break;
            case "CUSTOMERS": title += "Customer Management"; break;
            case "PRODUCTS": title += "Product Management"; break;
            case "ORDERS": title += "Order Management"; break;
            case "INVOICES": title += "Invoice Management"; break;
            case "SUPPLIERS": title += "Supplier Management"; break;
            case "WAREHOUSE": title += "Warehouse Management"; break;
            case "SALES_REPORT": title += "Sales Report"; break;
            case "ADVANCED_STATS": title += "Advanced Statistics"; break;
            case "WAREHOUSE_REPORT": title += "Warehouse Report"; break;
            default: title += "Management System";
        }
        setTitle(title);
    }
    
    // Methods for dialogs that remain as dialogs
    private void openBackupConfig() {
        try {
            BackupConfigWindow backupWindow = new BackupConfigWindow(this);
            backupWindow.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error opening backup configuration: " + e.getMessage(),
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