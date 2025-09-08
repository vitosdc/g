import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JMenuBar menuBar;
    private JPanel mainPanel;
    
    public MainWindow() {
        setupWindow();
        setupMenuBar();
        setupMainPanel();
        
        // Inizializza il database
        DatabaseManager.getInstance().initDatabase();
    }
    
    private void setupWindow() {
        setTitle("Sistema Gestionale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        
        // Aggiungi gestione della chiusura per chiudere correttamente il database
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DatabaseManager.getInstance().closeConnection();
            }
        });
    }
    
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // Menu File
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Esci");
        exitItem.addActionListener(e -> {
            DatabaseManager.getInstance().closeConnection();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        
        // Menu Gestione
        JMenu managementMenu = new JMenu("Gestione");
        JMenuItem clientiItem = new JMenuItem("Clienti");
        JMenuItem prodottiItem = new JMenuItem("Prodotti");
        JMenuItem ordiniItem = new JMenuItem("Ordini");
        JMenuItem fattureItem = new JMenuItem("Fatture");
        JMenuItem fornitoriItem = new JMenuItem("Fornitori");
        JMenuItem magazzinoItem = new JMenuItem("Magazzino");
        
        // Aggiungi action listener per i menu item
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
        
        // Menu Report
        JMenu reportMenu = new JMenu("Report");
        JMenuItem salesReportItem = new JMenuItem("Report Vendite");
        JMenuItem advancedStatsItem = new JMenuItem("Statistiche Avanzate");
        JMenuItem warehouseReportItem = new JMenuItem("Report Magazzino");
        
        salesReportItem.addActionListener(e -> openSalesReport());
        advancedStatsItem.addActionListener(e -> openAdvancedStats());
        warehouseReportItem.addActionListener(e -> openWarehouseReport());
        
        reportMenu.add(salesReportItem);
        reportMenu.add(advancedStatsItem);
        reportMenu.add(warehouseReportItem);
        
        // Menu Strumenti
        JMenu toolsMenu = new JMenu("Strumenti");
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
        
        // Pannello di benvenuto
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new GridBagLayout());
        JLabel welcomeLabel = new JLabel("Benvenuto nel Sistema Gestionale");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel);
        
        // Pannello informazioni sistema
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel versionLabel = new JLabel("Versione 1.0");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(versionLabel);
        
        // Dashboard con pulsanti rapidi
        JPanel dashboardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Creazione pulsanti con icone (usando emoji come placeholder)
        JButton newOrderButton = new JButton("📝 Nuovo Ordine");
        JButton newCustomerButton = new JButton("👥 Nuovo Cliente");
        JButton newProductButton = new JButton("📦 Nuovo Prodotto");
        JButton newInvoiceButton = new JButton("📋 Nuova Fattura");
        JButton suppliersButton = new JButton("🏭 Fornitori");
        JButton reportButton = new JButton("📊 Report Vendite");
        JButton warehouseButton = new JButton("🏪 Magazzino");
        JButton statsButton = new JButton("📈 Statistiche");
        JButton backupButton = new JButton("💾 Backup");
        
        // Stile pulsanti
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
        
        // Aggiunta pulsanti al pannello
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
                "Errore nell'apertura della finestra clienti: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della finestra prodotti: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della finestra ordini: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della finestra fatture: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della finestra fornitori: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della finestra magazzino: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura del report vendite: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura delle statistiche: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura del report magazzino: " + e.getMessage(),
                "Errore",
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
                "Errore nell'apertura della configurazione backup: " + e.getMessage(),
                "Errore",
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