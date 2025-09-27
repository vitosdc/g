import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFPreviewDialog extends JDialog {
    private Invoice invoice;
    private Customer customer;
    private CompanyData companyData;
    private boolean generatePDF = false;
    private SimpleDateFormat dateFormat;
    
    public PDFPreviewDialog(JDialog parent, Invoice invoice, Customer customer) {
        super(parent, "PDF Preview - Invoice " + invoice.getNumero(), true);
        this.invoice = invoice;
        this.customer = customer;
        this.companyData = CompanyData.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
    }
    
    public PDFPreviewDialog(JFrame parent, Invoice invoice, Customer customer) {
        super(parent, "PDF Preview - Invoice " + invoice.getNumero(), true);
        this.invoice = invoice;
        this.customer = customer;
        this.companyData = CompanyData.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        setupWindow();
        initComponents();
    }
    
    private void setupWindow() {
        setSize(700, 800);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Preview panel
        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
        previewPanel.setBackground(Color.WHITE);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Invoice Preview"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Header section
        JPanel headerPanel = createHeaderPanel();
        previewPanel.add(headerPanel);
        previewPanel.add(Box.createVerticalStrut(20));
        
        // Company and customer info
        JPanel infoPanel = createInfoPanel();
        previewPanel.add(infoPanel);
        previewPanel.add(Box.createVerticalStrut(20));
        
        // Invoice details
        JPanel detailsPanel = createDetailsPanel();
        previewPanel.add(detailsPanel);
        previewPanel.add(Box.createVerticalStrut(20));
        
        // Items table
        JPanel itemsPanel = createItemsPanel();
        previewPanel.add(itemsPanel);
        previewPanel.add(Box.createVerticalStrut(20));
        
        // Totals
        JPanel totalsPanel = createTotalsPanel();
        previewPanel.add(totalsPanel);
        
        // Scroll pane for preview
        JScrollPane scrollPane = new JScrollPane(previewPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton generateButton = new JButton("Generate PDF");
        JButton cancelButton = new JButton("Cancel");
        
        generateButton.setPreferredSize(new Dimension(120, 35));
        cancelButton.setPreferredSize(new Dimension(100, 35));
        
        generateButton.setBackground(new Color(51, 122, 183));
        generateButton.setForeground(Color.WHITE);
        generateButton.setFont(generateButton.getFont().deriveFont(Font.BOLD));
        
        generateButton.addActionListener(e -> {
            generatePDF = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);
        
        // Assembly
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Company name
        JLabel titleLabel = new JLabel("INVOICE", SwingConstants.RIGHT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(51, 122, 183));
        
        JLabel companyLabel = new JLabel(companyData.getCompanyName(), SwingConstants.LEFT);
        companyLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        panel.add(companyLabel, BorderLayout.WEST);
        panel.add(titleLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        
        // Company info
        JPanel companyPanel = new JPanel();
        companyPanel.setLayout(new BoxLayout(companyPanel, BoxLayout.Y_AXIS));
        companyPanel.setBorder(BorderFactory.createTitledBorder("From"));
        
        addInfoLine(companyPanel, companyData.getCompanyName(), true);
        addInfoLine(companyPanel, companyData.getAddress(), false);
        addInfoLine(companyPanel, companyData.getCity() + " " + companyData.getPostalCode(), false);
        addInfoLine(companyPanel, companyData.getCountry(), false);
        addInfoLine(companyPanel, "VAT: " + companyData.getVatNumber(), false);
        addInfoLine(companyPanel, "Phone: " + companyData.getPhone(), false);
        addInfoLine(companyPanel, "Email: " + companyData.getEmail(), false);
        
        // Customer info
        JPanel customerPanel = new JPanel();
        customerPanel.setLayout(new BoxLayout(customerPanel, BoxLayout.Y_AXIS));
        customerPanel.setBorder(BorderFactory.createTitledBorder("Bill To"));
        
        addInfoLine(customerPanel, customer.getNome() + " " + customer.getCognome(), true);
        addInfoLine(customerPanel, customer.getEmail(), false);
        addInfoLine(customerPanel, customer.getTelefono(), false);
        addInfoLine(customerPanel, customer.getIndirizzo(), false);
        
        panel.add(companyPanel);
        panel.add(customerPanel);
        
        return panel;
    }
    
    private void addInfoLine(JPanel parent, String text, boolean bold) {
        if (text != null && !text.trim().isEmpty() && 
            !text.equals("VAT: ") && !text.equals("Phone: ") && !text.equals("Email: ")) {
            JLabel label = new JLabel(text);
            if (bold) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            parent.add(label);
        }
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Invoice Details"));
        
        panel.add(new JLabel("Invoice Number:"));
        panel.add(new JLabel(invoice.getNumero()));
        panel.add(new JLabel("Date:"));
        panel.add(new JLabel(dateFormat.format(invoice.getData())));
        
        return panel;
    }
    
    private JPanel createItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Items"));
        
        String[] columns = {"Code", "Description", "Qty", "Unit Price", "VAT %", "Total"};
        Object[][] data = new Object[invoice.getItems().size()][6];
        
        for (int i = 0; i < invoice.getItems().size(); i++) {
            InvoiceItem item = invoice.getItems().get(i);
            data[i][0] = item.getProdottoCodice();
            data[i][1] = item.getProdottoNome();
            data[i][2] = item.getQuantita();
            data[i][3] = String.format("€ %.2f", item.getPrezzoUnitario());
            data[i][4] = String.format("%.1f%%", item.getAliquotaIva());
            data[i][5] = String.format("€ %.2f", item.getTotale());
        }
        
        JTable table = new JTable(data, columns);
        table.setEnabled(false);
        table.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 150));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTotalsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Totals"));
        
        panel.add(new JLabel("Subtotal:", SwingConstants.RIGHT));
        panel.add(new JLabel(String.format("€ %.2f", invoice.getImponibile())));
        
        panel.add(new JLabel("VAT:", SwingConstants.RIGHT));
        panel.add(new JLabel(String.format("€ %.2f", invoice.getIva())));
        
        JLabel totalLabel = new JLabel("TOTAL:", SwingConstants.RIGHT);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        JLabel totalValueLabel = new JLabel(String.format("€ %.2f", invoice.getTotale()));
        totalValueLabel.setFont(totalValueLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        panel.add(totalLabel);
        panel.add(totalValueLabel);
        
        return panel;
    }
    
    public boolean shouldGeneratePDF() {
        return generatePDF;
    }
    
    public static boolean showPreview(Component parent, Invoice invoice, Customer customer) {
        PDFPreviewDialog dialog;
        
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        if (parentWindow instanceof JFrame) {
            dialog = new PDFPreviewDialog((JFrame) parentWindow, invoice, customer);
        } else {
            dialog = new PDFPreviewDialog((JDialog) parentWindow, invoice, customer);
        }
        
        dialog.setVisible(true);
        return dialog.shouldGeneratePDF();
    }
}