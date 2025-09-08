// File: BackupManager.java
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import java.util.Properties;

public class BackupManager {
    private static BackupManager instance;
    private Properties config;
    private static final String CONFIG_FILE = "backup.properties";
    private static final String DEFAULT_BACKUP_DIR = "backups";
    
    private BackupManager() {
        loadConfig();
    }
    
    public static BackupManager getInstance() {
        if (instance == null) {
            instance = new BackupManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        config = new Properties();
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            } else {
                // Impostazioni predefinite
                config.setProperty("backup.directory", DEFAULT_BACKUP_DIR);
                config.setProperty("backup.autobackup", "true");
                config.setProperty("backup.retention", "7");
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "Backup Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void performBackup() {
        try {
            String backupDir = config.getProperty("backup.directory", DEFAULT_BACKUP_DIR);
            // Crea la directory di backup se non esiste
            Files.createDirectories(Paths.get(backupDir));
            
            // Nome file di backup con timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String backupFileName = "gestionale_" + sdf.format(new Date()) + ".db";
            String backupPath = Paths.get(backupDir, backupFileName).toString();
            
            // Copia il file del database
            Files.copy(Paths.get("gestionale.db"), Paths.get(backupPath), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // Pulizia vecchi backup
            cleanOldBackups();
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Errore durante il backup: " + e.getMessage());
        }
    }
    
    private void cleanOldBackups() {
        try {
            String backupDir = config.getProperty("backup.directory", DEFAULT_BACKUP_DIR);
            int retentionDays = Integer.parseInt(config.getProperty("backup.retention", "7"));
            
            File dir = new File(backupDir);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".db"));
                if (files != null) {
                    long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
                    for (File file : files) {
                        if (file.lastModified() < cutoffTime) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void restoreBackup(String backupFile) {
        try {
            // Verifica che il file di backup esista
            if (!Files.exists(Paths.get(backupFile))) {
                throw new FileNotFoundException("File di backup non trovato: " + backupFile);
            }
            
            // Chiudi la connessione al database
            DatabaseManager.getInstance().closeConnection();
            
            // Backup del database corrente prima del ripristino
            String currentBackup = "gestionale_pre_restore_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".db";
            Files.copy(Paths.get("gestionale.db"), Paths.get(currentBackup), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // Ripristina il backup
            Files.copy(Paths.get(backupFile), Paths.get("gestionale.db"), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // Riapri la connessione al database
            DatabaseManager.getInstance().initDatabase();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore durante il ripristino: " + e.getMessage());
        }
    }
    
    public String getBackupDirectory() {
        return config.getProperty("backup.directory", DEFAULT_BACKUP_DIR);
    }
    
    public void setBackupDirectory(String directory) {
        config.setProperty("backup.directory", directory);
        saveConfig();
    }
    
    public boolean isAutoBackupEnabled() {
        return Boolean.parseBoolean(config.getProperty("backup.autobackup", "true"));
    }
    
    public void setAutoBackupEnabled(boolean enabled) {
        config.setProperty("backup.autobackup", String.valueOf(enabled));
        saveConfig();
    }
    
    public int getRetentionDays() {
        return Integer.parseInt(config.getProperty("backup.retention", "7"));
    }
    
    public void setRetentionDays(int days) {
        config.setProperty("backup.retention", String.valueOf(days));
        saveConfig();
    }
    
    public File[] listBackups() {
        File dir = new File(getBackupDirectory());
        if (dir.exists() && dir.isDirectory()) {
            return dir.listFiles((d, name) -> name.endsWith(".db"));
        }
        return new File[0];
    }
}