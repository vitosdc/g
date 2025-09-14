// File: BackupConfigWindow.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableModel;
import java.util.Date;

public class BackupConfigWindow extends JDialog {
    private JTextField backupDirField;
    private JCheckBox autoBackupCheck;
    private JSpinner retentionSpinner;
    private JTable backupsTable;
    private DefaultTableModel tableModel;
    private BackupManager backupManager;
    
    public BackupConfigWindow(JFrame parent) {
        super(parent, "Backup Configuration", true);
        backupManager = BackupManager.getInstance();
        
        setupWindow();
        initComponents();
        loadBackupsList();
    }
    
    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
    }
    
    private void initComponents() {
        // Pannello configurazione
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Directory backup
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Backup Directory:"), gbc);
        
        gbc.gridx = 1;
        backupDirField = new JTextField(backupManager.getBackupDirectory(), 20);
        configPanel.add(backupDirField, gbc);
        
        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> selectBackupDirectory());
        configPanel.add(browseButton, gbc);
        
        // Auto backup
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Automatic Backup:"), gbc);
        
        gbc.gridx = 1;
        autoBackupCheck = new JCheckBox("Enable", backupManager.isAutoBackupEnabled());
        configPanel.add(autoBackupCheck, gbc);
        
        // Giorni di retention
        gbc.gridx = 0; gbc.gridy = 2;
        configPanel.add(new JLabel("Retention Days:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
            backupManager.getRetentionDays(), 1, 365, 1);
        retentionSpinner = new JSpinner(spinnerModel);
        configPanel.add(retentionSpinner, gbc);
        
        // Lista backup
        String[] columns = {"Date", "File Name", "Size"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        backupsTable = new JTable(tableModel);
        
        // Pannello pulsanti
        JPanel buttonPanel = new JPanel();
        JButton backupButton = new JButton("Perform Backup");
        JButton restoreButton = new JButton("Restore Backup");
        JButton saveButton = new JButton("Save Configuration");
        JButton closeButton = new JButton("Close");
        
        backupButton.addActionListener(e -> performBackup());
        restoreButton.addActionListener(e -> restoreBackup());
        saveButton.addActionListener(e -> saveConfiguration());
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(backupButton);
        buttonPanel.add(restoreButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        
        // Layout principale
        add(configPanel, BorderLayout.NORTH);
        add(new JScrollPane(backupsTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void selectBackupDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(backupManager.getBackupDirectory()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Backup Directory");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            backupDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void loadBackupsList() {
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        
        File[] backups = backupManager.listBackups();
        for (File backup : backups) {
            String[] row = {
                sdf.format(new Date(backup.lastModified())),
                backup.getName(),
                String.format("%.2f MB", backup.length() / (1024.0 * 1024.0))
            };
            tableModel.addRow(row);
        }
    }
    
    private void performBackup() {
        try {
            backupManager.performBackup();
            loadBackupsList();
            JOptionPane.showMessageDialog(this,
                "Backup performed successfully",
                "Backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error during backup: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void restoreBackup() {
        int selectedRow = backupsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Select a backup to restore",
                "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String fileName = (String)tableModel.getValueAt(selectedRow, 1);
        String filePath = new File(backupManager.getBackupDirectory(), fileName).getAbsolutePath();
        
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to restore this backup?\n" +
            "This operation cannot be undone.",
            "Confirm Restore",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            try {
                backupManager.restoreBackup(filePath);
                JOptionPane.showMessageDialog(this,
                    "Backup restored successfully.\n" +
                    "The program will now restart.",
                    "Restore Completed",
                    JOptionPane.INFORMATION_MESSAGE);
                    
                // Riavvia l'applicazione
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error during restore: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveConfiguration() {
        try {
            backupManager.setBackupDirectory(backupDirField.getText());
            backupManager.setAutoBackupEnabled(autoBackupCheck.isSelected());
            backupManager.setRetentionDays((Integer)retentionSpinner.getValue());
            
            JOptionPane.showMessageDialog(this,
                "Configuration saved successfully",
                "Configuration", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error saving configuration: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}