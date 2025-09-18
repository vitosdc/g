import javax.swing.*;
import java.awt.*;

/**
 * Base abstract class for all application panels
 * Provides common functionality and structure for all panels
 */
public abstract class BasePanel extends JPanel {
    protected MainWindow parentWindow;
    protected String panelTitle;
    protected JPanel headerPanel;
    protected JPanel contentPanel;
    protected JPanel toolbarPanel;
    
    public BasePanel(MainWindow parent, String title) {
        this.parentWindow = parent;
        this.panelTitle = title;
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // Header panel with title
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        headerPanel.setBackground(getBackground());
        
        JLabel titleLabel = new JLabel(panelTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Optional subtitle area
        JPanel subtitlePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        subtitlePanel.setBackground(getBackground());
        setupSubtitleArea(subtitlePanel);
        headerPanel.add(subtitlePanel, BorderLayout.EAST);
        
        // Toolbar panel for action buttons
        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbarPanel.setBackground(new Color(248, 248, 252));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        // Main content panel
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        contentPanel.setBackground(Color.WHITE);
    }
    
    private void setupLayout() {
        add(headerPanel, BorderLayout.NORTH);
        
        // Middle panel contains toolbar and content
        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.add(toolbarPanel, BorderLayout.NORTH);
        middlePanel.add(contentPanel, BorderLayout.CENTER);
        
        add(middlePanel, BorderLayout.CENTER);
    }
    
    /**
     * Create a standard action button with consistent styling
     */
    protected JButton createActionButton(String text, String icon, Color accentColor) {
        JButton button = new JButton(text);
        
        if (icon != null && !icon.isEmpty()) {
            // For emoji icons
            button.setText(icon + " " + text);
        }
        
        button.setPreferredSize(new Dimension(120, 35));
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(Color.WHITE);
        button.setForeground(accentColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(accentColor);
                button.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setForeground(accentColor);
            }
        });
        
        return button;
    }
    
    /**
     * Create a standard search panel with consistent styling
     */
    protected JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setBackground(toolbarPanel.getBackground());
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JTextField searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(250, 30));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        
        JButton searchButton = createActionButton("Search", null, new Color(70, 130, 180));
        searchButton.setPreferredSize(new Dimension(80, 30));
        
        JButton clearButton = createActionButton("Clear", null, new Color(150, 150, 150));
        clearButton.setPreferredSize(new Dimension(80, 30));
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        
        // Store references for subclasses
        setupSearchHandlers(searchField, searchButton, clearButton);
        
        return searchPanel;
    }
    
    /**
     * Create a standard table with consistent styling
     */
    protected JScrollPane createStandardTable(JTable table) {
        // Table styling
        table.setRowHeight(35);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 245));
        table.getTableHeader().setForeground(new Color(60, 60, 60));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)));
        
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(70, 130, 180, 50));
        table.setSelectionForeground(Color.BLACK);
        
        // Alternating row colors
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(248, 248, 252));
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        return scrollPane;
    }
    
    /**
     * Show success message with consistent styling
     */
    protected void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Show error message with consistent styling
     */
    protected void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Show warning message with consistent styling
     */
    protected void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Show confirmation dialog with consistent styling
     */
    protected boolean showConfirmDialog(String message, String title) {
        int result = JOptionPane.showConfirmDialog(this, message, title, 
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
    
    // Abstract methods that subclasses must implement
    public abstract void refreshData();
    
    protected abstract void setupToolbar();
    
    protected abstract void setupMainContent();
    
    // Optional methods that subclasses can override
    protected void setupSubtitleArea(JPanel subtitlePanel) {
        // Default: empty implementation
    }
    
    protected void setupSearchHandlers(JTextField searchField, JButton searchButton, JButton clearButton) {
        // Default: empty implementation - subclasses should override
    }
    
    // Getters
    public String getPanelTitle() {
        return panelTitle;
    }
    
    public MainWindow getParentWindow() {
        return parentWindow;
    }
    
    protected JPanel getToolbarPanel() {
        return toolbarPanel;
    }
    
    protected JPanel getContentPanel() {
        return contentPanel;
    }
}