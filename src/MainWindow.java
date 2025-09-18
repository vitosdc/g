import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class MainWindow extends JFrame {
    private JPanel toolbarPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private Map<String, BasePanel> panels;
    private JButton currentActiveButton;
    
    // Panel references
    private CustomersPanel customersPanel;
    private ProductsPanel productsPanel;
    private OrdersPanel ordersPanel;
    private InvoicesPanel invoicesPanel;
    private SuppliersPanel suppliersPanel;
    private WarehousePanel warehousePanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;
    
    public MainWindow() {
        setupWindow();
        setupToolbar();
        setupContentArea();
        initializePanels();
        
        // Initialize the database
        DatabaseManager.getInstance().initDatabase();
        
        // Apply global settings
        SettingsWindow.loadGlobalSettings();
        SettingsWindow.applyGlobalSettings();
        
        // Show default panel
        showPanel("customers");
    }
    
    private void setupWindow() {
        setTitle("WorkGenio - Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Add shutdown management
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DatabaseManager.getInstance().closeConnection();
            }
        });
    }
    
    private void setupToolbar() {
        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));
        toolbarPanel.setBackground(new Color(245, 245, 250));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        // Create toolbar buttons
        addToolbarButton("👥", "Customers", "customers", new Color(70, 130, 180));
        addToolbarButton("📦", "Products", "products", new Color(255, 140, 0));
        addToolbarButton("📝", "Orders", "orders", new Color(34, 139, 34));
        addToolbarButton("📋", "Invoices", "invoices", new Color(220, 20, 60));
        addToolbarButton("🏭", "Suppliers", "suppliers", new Color(138, 43, 226));
        addToolbarButton("🏪", "Warehouse", "warehouse", new Color(184, 134, 11));
        addToolbarButton("📊", "Reports", "reports", new Color(50, 205, 50));
        addToolbarButton("⚙️", "Settings", "settings", new Color(105, 105, 105));
        
        add(toolbarPanel, BorderLayout.NORTH);
    }
    
    private void addToolbarButton(String icon, String text, String panelKey, Color accentColor) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setPreferredSize(new Dimension(120, 70));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Text
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.BOLD, 12));
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setForeground(new Color(60, 60, 60));
        
        button.add(iconLabel, BorderLayout.CENTER);
        button.add(textLabel, BorderLayout.SOUTH);
        
        // Default styling
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        button.setOpaque(true);
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button != currentActiveButton) {
                    button.setBackground(new Color(248, 248, 252));
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 2, true),
                        BorderFactory.createEmptyBorder(7, 7, 7, 7)
                    ));
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button != currentActiveButton) {
                    button.setBackground(Color.WHITE);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    ));
                }
            }
        });
        
        // Click action
        button.addActionListener(e -> {
            setActiveButton(button, accentColor);
            showPanel(panelKey);
        });
        
        toolbarPanel.add(button);
        
        // Set first button as default active
        if (currentActiveButton == null) {
            currentActiveButton = button;
            setActiveButtonStyle(button, accentColor);
        }
    }
    
    private void setActiveButton(JButton button, Color accentColor) {
        // Reset previous active button
        if (currentActiveButton != null && currentActiveButton != button) {
            resetButtonStyle(currentActiveButton);
        }
        
        // Set new active button
        currentActiveButton = button;
        setActiveButtonStyle(button, accentColor);
    }
    
    private void setActiveButtonStyle(JButton button, Color accentColor) {
        button.setBackground(accentColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor.darker(), 2, true),
            BorderFactory.createEmptyBorder(7, 7, 7, 7)
        ));
        
        // Change text color to white for better contrast
        Component[] components = button.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel && comp.getName() == null) { // Text label (icon doesn't have name)
                comp.setForeground(Color.WHITE);
            }
        }
    }
    
    private void resetButtonStyle(JButton button) {
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        // Reset text color
        Component[] components = button.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel && comp.getName() == null) {
                comp.setForeground(new Color(60, 60, 60));
            }
        }
    }
    
    private void setupContentArea() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(contentPanel, BorderLayout.CENTER);
        
        panels = new HashMap<>();
    }
    
    private void initializePanels() {
        try {
            // Initialize all panels
            customersPanel = new CustomersPanel(this);
            panels.put("customers", customersPanel);
            contentPanel.add(customersPanel, "customers");
            
            productsPanel = new ProductsPanel(this);
            panels.put("products", productsPanel);
            contentPanel.add(productsPanel, "products");
            
            ordersPanel = new OrdersPanel(this);
            panels.put("orders", ordersPanel);
            contentPanel.add(ordersPanel, "orders");
            
            invoicesPanel = new InvoicesPanel(this);
            panels.put("invoices", invoicesPanel);
            contentPanel.add(invoicesPanel, "invoices");
            
            suppliersPanel = new SuppliersPanel(this);
            panels.put("suppliers", suppliersPanel);
            contentPanel.add(suppliersPanel, "suppliers");
            
            warehousePanel = new WarehousePanel(this);
            panels.put("warehouse", warehousePanel);
            contentPanel.add(warehousePanel, "warehouse");
            
            reportsPanel = new ReportsPanel(this);
            panels.put("reports", reportsPanel);
            contentPanel.add(reportsPanel, "reports");
            
            settingsPanel = new SettingsPanel(this);
            panels.put("settings", settingsPanel);
            contentPanel.add(settingsPanel, "settings");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error initializing panels: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void showPanel(String panelKey) {
        try {
            BasePanel panel = panels.get(panelKey);
            if (panel != null) {
                // Refresh panel data before showing
                panel.refreshData();
                cardLayout.show(contentPanel, panelKey);
                
                // Update window title
                setTitle("WorkGenio - " + panel.getPanelTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error showing panel: " + e.getMessage(),
                "WorkGenio - Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void refreshCurrentPanel() {
        try {
            for (BasePanel panel : panels.values()) {
                if (panel.isVisible()) {
                    panel.refreshData();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void refreshAllPanels() {
        try {
            for (BasePanel panel : panels.values()) {
                panel.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Utility methods for panels to communicate
    public void showCustomersPanel() {
        showPanel("customers");
    }
    
    public void showProductsPanel() {
        showPanel("products");
    }
    
    public void showOrdersPanel() {
        showPanel("orders");
    }
    
    public void showInvoicesPanel() {
        showPanel("invoices");
    }
    
    public void showSuppliersPanel() {
        showPanel("suppliers");
    }
    
    public void showWarehousePanel() {
        showPanel("warehouse");
    }
    
    public void showReportsPanel() {
        showPanel("reports");
    }
    
    public void showSettingsPanel() {
        showPanel("settings");
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