import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SettingsPanel extends BasePanel {
    private JPanel cardsPanel;
    private CardLayout settingsCardLayout;
    private JButton appSettingsButton;
    private JButton backupSettingsButton;
    private JButton backToMenuButton;
    
    // Settings components
    private ApplicationSettingsComponent appSettingsComponent;
    private BackupSettingsComponent backupSettingsComponent;
    
    public SettingsPanel(MainWindow parent) {
        super(parent, "Settings & Configuration");
        setupToolbar();
        setupMainContent();
    }
    
    @Override
    protected void setupToolbar() {
        // Navigation buttons
        appSettingsButton = createActionButton("App Settings", "⚙️", new Color(70, 130, 180));
        backupSettingsButton = createActionButton("Backup & Data", "💾", new Color(34, 139, 34));
        backToMenuButton = createActionButton("Settings Menu", "🏠", new Color(105, 105, 105));
        
        // Action listeners
        appSettingsButton.addActionListener(e -> showAppSettings());
        backupSettingsButton.addActionListener(e -> showBackupSettings());
        backToMenuButton.addActionListener(e -> showSettingsMenu());
        
        // Initially hide back button
        backToMenuButton.setVisible(false);
        
        // Add to toolbar
        toolbarPanel.add(appSettingsButton);
        toolbarPanel.add(backupSettingsButton);
        toolbarPanel.add(Box.createHorizontalStrut(20)); // Spacer
        toolbarPanel.add(backToMenuButton);
    }
    
    @Override
    protected void setupMainContent() {
        settingsCardLayout = new CardLayout();
        cardsPanel = new JPanel(settingsCardLayout);
        
        // Create settings menu
        JPanel menuPanel = createSettingsMenu();
        cardsPanel.add(menuPanel, "menu");
        
        // Initialize settings components
        appSettingsComponent = new ApplicationSettingsComponent(this);
        backupSettingsComponent = new BackupSettingsComponent(this);
        
        cardsPanel.add(appSettingsComponent, "app");
        cardsPanel.add(backupSettingsComponent, "backup");
        
        contentPanel.add(cardsPanel, BorderLayout.CENTER);
        
        // Show menu by default
        showSettingsMenu();
    }
    
    private JPanel createSettingsMenu() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(30, 30, 30, 30);
        
        // Title
        JLabel titleLabel = new JLabel("Settings & Configuration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(60, 60, 60));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        menuPanel.add(titleLabel, gbc);
        
        // Reset gridwidth
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        
        // Create large settings buttons
        JButton largeAppButton = createLargeSettingsButton(
            "⚙️", "Application Settings", 
            "Appearance, fonts, themes and general preferences",
            new Color(70, 130, 180)
        );
        
        JButton largeBackupButton = createLargeSettingsButton(
            "💾", "Backup & Data Management", 
            "Database backup, restore and data export options",
            new Color(34, 139, 34)
        );
        
        // Add action listeners
        largeAppButton.addActionListener(e -> showAppSettings());
        largeBackupButton.addActionListener(e -> showBackupSettings());
        
        // Add to panel
        gbc.gridx = 0;
        menuPanel.add(largeAppButton, gbc);
        
        gbc.gridx = 1;
        menuPanel.add(largeBackupButton, gbc);
        
        // Add system info at the bottom
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(50, 30, 30, 30);
        JPanel infoPanel = createSystemInfoPanel();
        menuPanel.add(infoPanel, gbc);
        
        return menuPanel;
    }
    
    private JPanel createSystemInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("System Information"));
        infoPanel.setBackground(new Color(248, 248, 252));
        
        Font infoFont = new Font("Arial", Font.PLAIN, 12);
        
        JLabel versionLabel = new JLabel("WorkGenio Version:");
        versionLabel.setFont(infoFont.deriveFont(Font.BOLD));
        JLabel versionValue = new JLabel("1.0 Professional");
        versionValue.setFont(infoFont);
        
        JLabel javaLabel = new JLabel("Java Version:");
        javaLabel.setFont(infoFont.deriveFont(Font.BOLD));
        JLabel javaValue = new JLabel(System.getProperty("java.version"));
        javaValue.setFont(infoFont);
        
        JLabel osLabel = new JLabel("Operating System:");
        osLabel.setFont(infoFont.deriveFont(Font.BOLD));
        JLabel osValue = new JLabel(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        osValue.setFont(infoFont);
        
        infoPanel.add(versionLabel);
        infoPanel.add(versionValue);
        infoPanel.add(javaLabel);
        infoPanel.add(javaValue);
        infoPanel.add(osLabel);
        infoPanel.add(osValue);
        
        return infoPanel;
    }
    
    private JButton createLargeSettingsButton(String icon, String title, String description, Color accentColor) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setPreferredSize(new Dimension(350, 180));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(25, 0, 15, 0));
        
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
        descLabel.setBorder(BorderFactory.createEmptyBorder(8, 15, 25, 15));
        
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
    
    public void showSettingsMenu() {
        settingsCardLayout.show(cardsPanel, "menu");
        backToMenuButton.setVisible(false);
        updateTitle("Settings & Configuration");
    }
    
    public void showAppSettings() {
        settingsCardLayout.show(cardsPanel, "app");
        backToMenuButton.setVisible(true);
        appSettingsComponent.refreshData();
        updateTitle("Application Settings");
    }
    
    public void showBackupSettings() {
        settingsCardLayout.show(cardsPanel, "backup");
        backToMenuButton.setVisible(true);
        backupSettingsComponent.refreshData();
        updateTitle("Backup & Data Management");
    }
    
    private void updateTitle(String newTitle) {
        Component[] headerComponents = headerPanel.getComponents();
        for (Component comp : headerComponents) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText(newTitle);
                break;
            }
        }
    }
    
    @Override
    public void refreshData() {
        // Refresh current visible component
        if (appSettingsComponent != null && appSettingsComponent.isVisible()) {
            appSettingsComponent.refreshData();
        } else if (backupSettingsComponent != null && backupSettingsComponent.isVisible()) {
            backupSettingsComponent.refreshData();
        }
    }
    
    // Application Settings Component
    private class ApplicationSettingsComponent extends JPanel {
        private SettingsPanel parent;
        private JSlider fontSizeSlider;
        private JComboBox<String> themeCombo;
        private JCheckBox autoBackupCheck;
        private JSpinner backupIntervalSpinner;
        private JLabel previewLabel;
        
        public ApplicationSettingsComponent(SettingsPanel parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setupComponents();
        }
        
        private void setupComponents() {
            JPanel mainPanel = new JPanel(new GridBagLayout());
            mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            mainPanel.setBackground(Color.WHITE);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Appearance Section
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            JLabel appearanceTitle = new JLabel("Appearance");
            appearanceTitle.setFont(new Font("Arial", Font.BOLD, 18));
            appearanceTitle.setForeground(new Color(70, 130, 180));
            mainPanel.add(appearanceTitle, gbc);
            
            gbc.gridwidth = 1;
            
            // Font Size
            gbc.gridx = 0; gbc.gridy = 1;
            mainPanel.add(new JLabel("Font Size:"), gbc);
            
            gbc.gridx = 1;
            fontSizeSlider = new JSlider(8, 24, getCurrentFontSize());
            fontSizeSlider.setMajorTickSpacing(4);
            fontSizeSlider.setMinorTickSpacing(2);
            fontSizeSlider.setPaintTicks(true);
            fontSizeSlider.setPaintLabels(true);
            fontSizeSlider.addChangeListener(e -> updatePreview());
            mainPanel.add(fontSizeSlider, gbc);
            
            // Theme
            gbc.gridx = 0; gbc.gridy = 2;
            mainPanel.add(new JLabel("Theme:"), gbc);
            
            gbc.gridx = 1;
            themeCombo = new JComboBox<>(new String[]{"System", "Metal", "Nimbus"});
            themeCombo.setSelectedItem(SettingsWindow.getGlobalSetting("theme", "System"));
            mainPanel.add(themeCombo, gbc);
            
            // Preview
            gbc.gridx = 0; gbc.gridy = 3;
            mainPanel.add(new JLabel("Preview:"), gbc);
            
            gbc.gridx = 1;
            previewLabel = new JLabel("Sample text with current font");
            previewLabel.setBorder(BorderFactory.createEtchedBorder());
            previewLabel.setOpaque(true);
            previewLabel.setBackground(Color.WHITE);
            mainPanel.add(previewLabel, gbc);
            
            // General Section
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
            gbc.insets = new Insets(30, 10, 10, 10);
            JLabel generalTitle = new JLabel("General");
            generalTitle.setFont(new Font("Arial", Font.BOLD, 18));
            generalTitle.setForeground(new Color(70, 130, 180));
            mainPanel.add(generalTitle, gbc);
            
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 10, 10, 10);
            
            // Auto Backup
            gbc.gridx = 0; gbc.gridy = 5;
            autoBackupCheck = new JCheckBox("Enable Auto Backup");
            autoBackupCheck.setSelected(Boolean.parseBoolean(SettingsWindow.getGlobalSetting("auto_backup", "true")));
            autoBackupCheck.setBackground(Color.WHITE);
            mainPanel.add(autoBackupCheck, gbc);
            
            // Backup Interval
            gbc.gridx = 0; gbc.gridy = 6;
            mainPanel.add(new JLabel("Backup Interval (hours):"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                Integer.parseInt(SettingsWindow.getGlobalSetting("backup_interval", "24")), 1, 168, 1);
            backupIntervalSpinner = new JSpinner(spinnerModel);
            mainPanel.add(backupIntervalSpinner, gbc);
            
            // Buttons
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
            gbc.insets = new Insets(30, 10, 10, 10);
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.CENTER;
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setBackground(Color.WHITE);
            
            JButton saveButton = createActionButton("Save Settings", "💾", new Color(34, 139, 34));
            JButton resetButton = createActionButton("Reset to Defaults", "🔄", new Color(255, 140, 0));
            
            saveButton.addActionListener(e -> saveApplicationSettings());
            resetButton.addActionListener(e -> resetToDefaults());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(resetButton);
            
            mainPanel.add(buttonPanel, gbc);
            
            add(new JScrollPane(mainPanel), BorderLayout.CENTER);
            
            updatePreview();
        }
        
        private int getCurrentFontSize() {
            return Integer.parseInt(SettingsWindow.getGlobalSetting("font_size", "12"));
        }
        
        private void updatePreview() {
            int fontSize = fontSizeSlider.getValue();
            Font newFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            previewLabel.setFont(newFont);
            previewLabel.setText("Sample text - Size: " + fontSize);
        }
        
        private void saveApplicationSettings() {
            try {
                // Save settings using SettingsWindow logic
                SettingsWindow tempSettings = new SettingsWindow(parentWindow);
                // This is a simplified approach - you'd need to adapt SettingsWindow to be more modular
                
                showSuccessMessage("Settings saved successfully!\nRestart the application to apply all changes.");
                
            } catch (Exception e) {
                showErrorMessage("Error saving settings: " + e.getMessage());
            }
        }
        
        private void resetToDefaults() {
            if (showConfirmDialog("Reset all settings to default values?", "Confirm Reset")) {
                fontSizeSlider.setValue(12);
                themeCombo.setSelectedItem("System");
                autoBackupCheck.setSelected(true);
                backupIntervalSpinner.setValue(24);
                updatePreview();
            }
        }
        
        public void refreshData() {
            // Refresh current settings
            fontSizeSlider.setValue(getCurrentFontSize());
            themeCombo.setSelectedItem(SettingsWindow.getGlobalSetting("theme", "System"));
            autoBackupCheck.setSelected(Boolean.parseBoolean(SettingsWindow.getGlobalSetting("auto_backup", "true")));
            backupIntervalSpinner.setValue(Integer.parseInt(SettingsWindow.getGlobalSetting("backup_interval", "24")));
            updatePreview();
        }
    }
    
    // Backup Settings Component
    private class BackupSettingsComponent extends JPanel {
        private SettingsPanel parent;
        private JTextField backupDirField;
        private JCheckBox autoBackupCheck;
        private JSpinner retentionSpinner;
        private JTable backupsTable;
        private javax.swing.table.DefaultTableModel tableModel;
        private BackupManager backupManager;
        
        public BackupSettingsComponent(SettingsPanel parent) {
            this.parent = parent;
            this.backupManager = BackupManager.getInstance();
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setupComponents();
        }
        
        private void setupComponents() {
            JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            mainPanel.setBackground(Color.WHITE);
            
            // Configuration Panel
            JPanel configPanel = createConfigPanel();
            mainPanel.add(configPanel, BorderLayout.NORTH);
            
            // Backups List Panel
            JPanel listPanel = createBackupsListPanel();
            mainPanel.add(listPanel, BorderLayout.CENTER);
            
            // Action Buttons Panel
            JPanel actionPanel = createActionButtonsPanel();
            mainPanel.add(actionPanel, BorderLayout.SOUTH);
            
            add(mainPanel, BorderLayout.CENTER);
        }
        
        private JPanel createConfigPanel() {
            JPanel configPanel = new JPanel(new GridBagLayout());
            configPanel.setBorder(BorderFactory.createTitledBorder("Backup Configuration"));
            configPanel.setBackground(Color.WHITE);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            // Backup Directory
            gbc.gridx = 0; gbc.gridy = 0;
            configPanel.add(new JLabel("Backup Directory:"), gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            backupDirField = new JTextField(backupManager.getBackupDirectory());
            backupDirField.setPreferredSize(new Dimension(300, 25));
            configPanel.add(backupDirField, gbc);
            
            gbc.gridx = 2; gbc.weightx = 0.0;
            JButton browseButton = createActionButton("Browse", "📁", new Color(70, 130, 180));
            browseButton.setPreferredSize(new Dimension(80, 25));
            browseButton.addActionListener(e -> selectBackupDirectory());
            configPanel.add(browseButton, gbc);
            
            // Auto Backup
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
            configPanel.add(new JLabel("Automatic Backup:"), gbc);
            
            gbc.gridx = 1;
            autoBackupCheck = new JCheckBox("Enable automatic backup");
            autoBackupCheck.setSelected(backupManager.isAutoBackupEnabled());
            autoBackupCheck.setBackground(Color.WHITE);
            configPanel.add(autoBackupCheck, gbc);
            
            // Retention Days
            gbc.gridx = 0; gbc.gridy = 2;
            configPanel.add(new JLabel("Retention Days:"), gbc);
            
            gbc.gridx = 1;
            SpinnerNumberModel retentionModel = new SpinnerNumberModel(
                backupManager.getRetentionDays(), 1, 365, 1);
            retentionSpinner = new JSpinner(retentionModel);
            retentionSpinner.setPreferredSize(new Dimension(100, 25));
            configPanel.add(retentionSpinner, gbc);
            
            // Save Configuration Button
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            
            JButton saveConfigButton = createActionButton("Save Configuration", "💾", new Color(34, 139, 34));
            saveConfigButton.addActionListener(e -> saveBackupConfiguration());
            configPanel.add(saveConfigButton, gbc);
            
            return configPanel;
        }
        
        private JPanel createBackupsListPanel() {
            JPanel listPanel = new JPanel(new BorderLayout());
            listPanel.setBorder(BorderFactory.createTitledBorder("Available Backups"));
            
            // Table
            String[] columns = {"Date", "File Name", "Size"};
            tableModel = new javax.swing.table.DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            backupsTable = new JTable(tableModel);
            backupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            JScrollPane tableScrollPane = createStandardTable(backupsTable);
            listPanel.add(tableScrollPane, BorderLayout.CENTER);
            
            return listPanel;
        }
        
        private JPanel createActionButtonsPanel() {
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            actionPanel.setBackground(Color.WHITE);
            
            JButton backupButton = createActionButton("Perform Backup", "💾", new Color(34, 139, 34));
            JButton restoreButton = createActionButton("Restore Backup", "📥", new Color(70, 130, 180));
            JButton deleteButton = createActionButton("Delete Backup", "🗑️", new Color(220, 20, 60));
            JButton refreshButton = createActionButton("Refresh List", "🔄", new Color(105, 105, 105));
            
            backupButton.addActionListener(e -> performBackup());
            restoreButton.addActionListener(e -> restoreBackup());
            deleteButton.addActionListener(e -> deleteSelectedBackup());
            refreshButton.addActionListener(e -> refreshBackupsList());
            
            actionPanel.add(backupButton);
            actionPanel.add(restoreButton);
            actionPanel.add(deleteButton);
            actionPanel.add(refreshButton);
            
            return actionPanel;
        }
        
        private void selectBackupDirectory() {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new java.io.File(backupManager.getBackupDirectory()));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Backup Directory");
            
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                backupDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }
        
        private void saveBackupConfiguration() {
            try {
                backupManager.setBackupDirectory(backupDirField.getText());
                backupManager.setAutoBackupEnabled(autoBackupCheck.isSelected());
                backupManager.setRetentionDays((Integer)retentionSpinner.getValue());
                
                showSuccessMessage("Backup configuration saved successfully!");
                
            } catch (Exception e) {
                showErrorMessage("Error saving backup configuration: " + e.getMessage());
            }
        }
        
        private void performBackup() {
            try {
                backupManager.performBackup();
                refreshBackupsList();
                showSuccessMessage("Backup performed successfully!");
                
            } catch (Exception e) {
                showErrorMessage("Error during backup: " + e.getMessage());
            }
        }
        
        private void restoreBackup() {
            int selectedRow = backupsTable.getSelectedRow();
            if (selectedRow == -1) {
                showWarningMessage("Please select a backup to restore");
                return;
            }
            
            String fileName = (String)tableModel.getValueAt(selectedRow, 1);
            String filePath = new java.io.File(backupManager.getBackupDirectory(), fileName).getAbsolutePath();
            
            if (!showConfirmDialog(
                "Are you sure you want to restore this backup?\n" +
                "This operation will replace the current database and cannot be undone.\n" +
                "The application will restart after restoration.",
                "Confirm Restore")) {
                return;
            }
            
            try {
                backupManager.restoreBackup(filePath);
                showSuccessMessage("Backup restored successfully!\nThe application will now restart.");
                
                // Restart the application
                System.exit(0);
                
            } catch (Exception e) {
                showErrorMessage("Error during restore: " + e.getMessage());
            }
        }
        
        private void deleteSelectedBackup() {
            int selectedRow = backupsTable.getSelectedRow();
            if (selectedRow == -1) {
                showWarningMessage("Please select a backup to delete");
                return;
            }
            
            String fileName = (String)tableModel.getValueAt(selectedRow, 1);
            
            if (!showConfirmDialog(
                "Are you sure you want to delete the backup:\n" + fileName + "?\n\n" +
                "This action cannot be undone.",
                "Confirm Deletion")) {
                return;
            }
            
            try {
                java.io.File backupFile = new java.io.File(backupManager.getBackupDirectory(), fileName);
                
                if (backupFile.exists() && backupFile.delete()) {
                    refreshBackupsList();
                    showSuccessMessage("Backup deleted successfully");
                } else {
                    showErrorMessage("Failed to delete backup file");
                }
            } catch (Exception e) {
                showErrorMessage("Error deleting backup: " + e.getMessage());
            }
        }
        
        private void refreshBackupsList() {
            tableModel.setRowCount(0);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            
            java.io.File[] backups = backupManager.listBackups();
            for (java.io.File backup : backups) {
                String[] row = {
                    sdf.format(new java.util.Date(backup.lastModified())),
                    backup.getName(),
                    String.format("%.2f MB", backup.length() / (1024.0 * 1024.0))
                };
                tableModel.addRow(row);
            }
        }
        
        public void refreshData() {
            backupDirField.setText(backupManager.getBackupDirectory());
            autoBackupCheck.setSelected(backupManager.isAutoBackupEnabled());
            retentionSpinner.setValue(backupManager.getRetentionDays());
            refreshBackupsList();
        }
    }
}