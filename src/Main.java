// File: Main.java
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Imposta il look and feel del sistema operativo
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Imposta proprietà globali dell'applicazione
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WorkGenio");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Crea e mostra la finestra principale
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}