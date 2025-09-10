import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NotificationManager {
    
    public enum NotificationType {
        SUCCESS(new Color(76, 175, 80), "✓"),
        WARNING(new Color(255, 152, 0), "⚠"),
        ERROR(new Color(244, 67, 54), "✗"),
        INFO(new Color(33, 150, 243), "ℹ");
        
        private final Color color;
        private final String icon;
        
        NotificationType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }
        
        public Color getColor() { return color; }
        public String getIcon() { return icon; }
    }
    
    public static void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS, 3000);
    }
    
    public static void showWarning(String message) {
        showNotification(message, NotificationType.WARNING, 5000);
    }
    
    public static void showError(String message) {
        showNotification(message, NotificationType.ERROR, 7000);
    }
    
    public static void showInfo(String message) {
        showNotification(message, NotificationType.INFO, 4000);
    }
    
    private static void showNotification(String message, NotificationType type, int duration) {
        SwingUtilities.invokeLater(() -> {
            // Find main window
            Frame parentFrame = getMainFrame();
            
            JWindow notification = new JWindow(parentFrame);
            setupNotificationWindow(notification, message, type);
            
            // Position in top-right corner
            positionNotification(notification, parentFrame);
            
            // Entry animation
            animateIn(notification);
            
            // Auto-hide timer
            Timer hideTimer = new Timer(duration, e -> animateOut(notification));
            hideTimer.setRepeats(false);
            hideTimer.start();
            
            // Click to close
            notification.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    hideTimer.stop();
                    animateOut(notification);
                }
            });
        });
    }
    
    private static Frame getMainFrame() {
        for (Frame frame : Frame.getFrames()) {
            if (frame.isDisplayable() && frame.getTitle().contains("WorkGenio")) {
                return frame;
            }
        }
        return null;
    }
    
    private static void setupNotificationWindow(JWindow notification, String message, NotificationType type) {
        notification.setSize(350, 70);
        notification.setAlwaysOnTop(true);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(type.getColor());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(type.getColor().darker(), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Icon
        JLabel iconLabel = new JLabel(type.getIcon());
        iconLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        
        // Message
        JLabel messageLabel = new JLabel("<html>" + wrapText(message, 40) + "</html>");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 13f));
        
        // Close button
        JLabel closeLabel = new JLabel("×");
        closeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        closeLabel.setForeground(Color.WHITE);
        closeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeLabel.setToolTipText("Close");
        
        mainPanel.add(iconLabel, BorderLayout.WEST);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        mainPanel.add(closeLabel, BorderLayout.EAST);
        
        notification.add(mainPanel);
        
        // Add shadow effect
        addShadowEffect(notification);
    }
    
    private static void positionNotification(JWindow notification, Frame parentFrame) {
        if (parentFrame != null) {
            int x = parentFrame.getX() + parentFrame.getWidth() - notification.getWidth() - 20;
            int y = parentFrame.getY() + 50;
            notification.setLocation(x, y);
        } else {
            // Center on screen if no parent
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width - notification.getWidth() - 20;
            int y = 50;
            notification.setLocation(x, y);
        }
    }
    
    private static void animateIn(JWindow notification) {
        notification.setOpacity(0.0f);
        notification.setVisible(true);
        
        Timer fadeIn = new Timer(30, null);
        fadeIn.addActionListener(new ActionListener() {
            private float opacity = 0.0f;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.1f;
                if (opacity >= 0.95f) {
                    opacity = 0.95f;
                    fadeIn.stop();
                }
                notification.setOpacity(opacity);
            }
        });
        fadeIn.start();
    }
    
    private static void animateOut(JWindow notification) {
        Timer fadeOut = new Timer(30, null);
        fadeOut.addActionListener(new ActionListener() {
            private float opacity = 0.95f;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= 0.1f;
                if (opacity <= 0.0f) {
                    fadeOut.stop();
                    notification.dispose();
                    return;
                }
                notification.setOpacity(opacity);
            }
        });
        fadeOut.start();
    }
    
    private static void addShadowEffect(JWindow notification) {
        // Simple shadow effect
        notification.getRootPane().setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 4, 4),
                BorderFactory.createLineBorder(Color.GRAY, 1)
            )
        );
    }
    
    private static String wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        
        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split(" ");
        int currentLength = 0;
        
        for (String word : words) {
            if (currentLength + word.length() > maxLength) {
                wrapped.append("<br>");
                currentLength = 0;
            }
            wrapped.append(word).append(" ");
            currentLength += word.length() + 1;
        }
        
        return wrapped.toString().trim();
    }
    
    // Helper methods for common messages
    public static void showSaveSuccess(String entityName) {
        showSuccess(entityName + " saved successfully");
    }
    
    public static void showDeleteSuccess(String entityName) {
        showSuccess(entityName + " deleted successfully");
    }
    
    public static void showValidationError(String message) {
        showError("Validation error: " + message);
    }
    
    public static void showDatabaseError(String operation) {
        showError("Database error during: " + operation + ". Please retry.");
    }
    
    public static void showLowStockWarning(String productName, int currentStock) {
        showWarning("Low stock for " + productName + " (remaining: " + currentStock + ")");
    }
}