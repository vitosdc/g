import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import java.io.*;

public class SettingsWindow extends JDialog {
    private static final String SETTINGS_FILE = "app_settings.properties";
    private static Properties settings = new Properties();
    
    private JSlider fontSizeSlider;
    private JComboBox<String> themeCombo;
    private JCheckBox autoBackupCheck;
    private JSpinner backupIntervalSpinner;
    private JLabel previewLabel;
    
    public SettingsWindow(JFrame parent) {
        super(parent, "Settings", true);
        loadSettings();
        setupWindow();
        initComponents();
        applyCurrentSettings();
    }
    
    private void setupWindow() {
        setSize(500, 400);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Appearance Tab
        JPanel appearancePanel = createAppearancePanel();
        tabbedPane.addTab("Appearance", appearancePanel);
        
        // General Tab
        JPanel generalPanel = createGeneralPanel();
        tabbedPane.addTab("General", generalPanel);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton applyButton = new JButton("Apply");
        
        okButton.addActionListener(e -> { saveSettings(); dispose(); });
        cancelButton.addActionListener(e -> dispose());
        applyButton.addActionListener(e -> saveSettings());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createAppearancePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Font Size
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Font Size:"), gbc);
        
        gbc.gridx = 1;
        fontSizeSlider = new JSlider(8, 24, getCurrentFontSize());
        fontSizeSlider.setMajorTickSpacing(4);
        fontSizeSlider.setMinorTickSpacing(2);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);
        fontSizeSlider.addChangeListener(e -> updatePreview());
        panel.add(fontSizeSlider, gbc);
        
        // Theme
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Theme:"), gbc);
        
        gbc.gridx = 1;
        themeCombo = new JComboBox<>(new String[]{"System", "Metal", "Nimbus"});
        themeCombo.setSelectedItem(getSetting("theme", "System"));
        panel.add(themeCombo, gbc);
        
        // Preview
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Preview:"), gbc);
        
        gbc.gridx = 1;
        previewLabel = new JLabel("Sample text with current font");
        previewLabel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(previewLabel, gbc);
        
        return panel;
    }
    
    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Auto Backup
        gbc.gridx = 0; gbc.gridy = 0;
        autoBackupCheck = new JCheckBox("Enable Auto Backup");
        autoBackupCheck.setSelected(Boolean.parseBoolean(getSetting("auto_backup", "true")));
        panel.add(autoBackupCheck, gbc);
        
        // Backup Interval
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Backup Interval (hours):"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
            Integer.parseInt(getSetting("backup_interval", "24")), 1, 168, 1);
        backupIntervalSpinner = new JSpinner(spinnerModel);
        panel.add(backupIntervalSpinner, gbc);
        
        return panel;
    }
    
    private void updatePreview() {
        int fontSize = fontSizeSlider.getValue();
        Font newFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        previewLabel.setFont(newFont);
        previewLabel.setText("Sample text - Size: " + fontSize);
    }
    
    private void saveSettings() {
        // Save font size
        settings.setProperty("font_size", String.valueOf(fontSizeSlider.getValue()));
        
        // Save theme
        settings.setProperty("theme", (String)themeCombo.getSelectedItem());
        
        // Save general settings
        settings.setProperty("auto_backup", String.valueOf(autoBackupCheck.isSelected()));
        settings.setProperty("backup_interval", String.valueOf(backupIntervalSpinner.getValue()));
        
        // Write to file
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(fos, "Application Settings");
            applySettings();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", 
                "Settings", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSettings() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            settings.load(fis);
        } catch (IOException e) {
            // File doesn't exist or error reading - use defaults
            setDefaultSettings();
        }
    }
    
    private void setDefaultSettings() {
        settings.setProperty("font_size", "12");
        settings.setProperty("theme", "System");
        settings.setProperty("auto_backup", "true");
        settings.setProperty("backup_interval", "24");
    }
    
    private void applyCurrentSettings() {
        updatePreview();
    }
    
    private void applySettings() {
        // Apply font size globally
        int fontSize = Integer.parseInt(getSetting("font_size", "12"));
        updateGlobalFont(fontSize);
        
        // Apply theme
        String theme = getSetting("theme", "System");
        updateLookAndFeel(theme);
    }
    
    private void updateGlobalFont(int size) {
        Font newFont = new Font(Font.SANS_SERIF, Font.PLAIN, size);
        
        // Update UIManager defaults
        UIManager.put("Label.font", newFont);
        UIManager.put("Button.font", newFont);
        UIManager.put("TextField.font", newFont);
        UIManager.put("Table.font", newFont);
        UIManager.put("Menu.font", newFont);
        UIManager.put("MenuItem.font", newFont);
        
        // Refresh all open windows
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
    
    private void updateLookAndFeel(String theme) {
        try {
            switch (theme) {
                case "System":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    break;
                case "Nimbus":
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                    break;
            }
            
            // Refresh all windows
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private int getCurrentFontSize() {
        return Integer.parseInt(getSetting("font_size", "12"));
    }
    
    private String getSetting(String key, String defaultValue) {
        return settings.getProperty(key, defaultValue);
    }
    
    // Static methods for global access
    public static String getGlobalSetting(String key, String defaultValue) {
        if (settings.isEmpty()) {
            loadGlobalSettings();
        }
        return settings.getProperty(key, defaultValue);
    }
    
    public static void loadGlobalSettings() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            settings.load(fis);
        } catch (IOException e) {
            // Use defaults
        }
    }
    
    public static void applyGlobalSettings() {
        int fontSize = Integer.parseInt(getGlobalSetting("font_size", "12"));
        Font newFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        
        UIManager.put("Label.font", newFont);
        UIManager.put("Button.font", newFont);
        UIManager.put("TextField.font", newFont);
        UIManager.put("Table.font", newFont);
        UIManager.put("Menu.font", newFont);
        UIManager.put("MenuItem.font", newFont);
    }
}