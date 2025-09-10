import javax.swing.*;
import java.awt.*;

public class GlobalLoadingManager {
    private static JDialog loadingDialog;
    private static JProgressBar progressBar;
    private static JLabel messageLabel;
    
    public static void showLoading(String message) {
        SwingUtilities.invokeLater(() -> {
            if (loadingDialog == null) {
                createLoadingDialog();
            }
            messageLabel.setText(message);
            loadingDialog.setVisible(true);
        });
    }
    
    public static void hideLoading() {
        SwingUtilities.invokeLater(() -> {
            if (loadingDialog != null) {
                loadingDialog.setVisible(false);
            }
        });
    }
    
    private static void createLoadingDialog() {
        // Find main window to center dialog
        Frame parentFrame = null;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isDisplayable() && frame.getTitle().contains("WorkGenio")) {
                parentFrame = frame;
                break;
            }
        }
        
        loadingDialog = new JDialog(parentFrame, "Loading", true);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setSize(350, 120);
        loadingDialog.setLocationRelativeTo(parentFrame);
        loadingDialog.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Loading icon
        JLabel iconLabel = new JLabel("⏳");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Message
        messageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 14f));
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(iconLabel, BorderLayout.WEST);
        centerPanel.add(messageLabel, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        
        loadingDialog.add(mainPanel);
    }
    
    public static void showLoadingWithProgress(String message, int progress) {
        SwingUtilities.invokeLater(() -> {
            if (loadingDialog == null) {
                createLoadingDialog();
            }
            messageLabel.setText(message);
            progressBar.setIndeterminate(false);
            progressBar.setValue(progress);
            progressBar.setStringPainted(true);
            progressBar.setString(progress + "%");
            loadingDialog.setVisible(true);
        });
    }
}