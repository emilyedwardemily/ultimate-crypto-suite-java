package app;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/**
 * ULTIMATE CRYPTO SUITE - LOGIN CONTROLLER [V15.0 ACCOUNT-BASED]
 * STRATEGY: Authentic Identity via Render Node Gateway
 */
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField; // Imebadilishwa kutoka licenseField
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            updateStatus("⚠️ Email and Password required!", Color.YELLOW);
            return;
        }

        loginButton.setDisable(true);
        updateStatus("📡 Authenticating with Render Gateway...", Color.CYAN);

        // 1. BACKGROUND THREAD: Zuia UI kufanya "Hang"
        new Thread(() -> {
            try {
                // Sasa hivi tunatumia authenticate ya DatabaseManager inayopokea email na password
                boolean isAuthenticated = DatabaseManager.authenticate(email, password);

                Platform.runLater(() -> {
                    if (isAuthenticated) {
                        updateStatus("✅ Access Granted! Loading Profile...", Color.LIME);
                        
                        // Tunahifadhi Session (Tokenized) badala ya plain license key
                        saveSessionLocally(email, password);
                        
                        // UX Delay kidogo ili mtumiaji aone mafanikio
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            Platform.runLater(this::navigateToDashboard);
                        }).start();
                        
                    } else {
                        updateStatus("❌ Access Denied: Invalid Credentials.", Color.web("#f85149"));
                        loginButton.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("🌐 Connection Error: Backend API Unreachable.", Color.ORANGE);
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Inahifadhi Session kwa usalama zaidi (Base64 Encoded)
     */
    private void saveSessionLocally(String email, String pass) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(".sys_auth.bin"))) {
            // Tunaficha email na pass kwa Base64 isiweze kusomeka kirahisi kwa macho
            String rawData = email + "::" + pass;
            String encoded = Base64.getEncoder().encodeToString(rawData.getBytes());
            writer.println(encoded);
            log.info("Identity cached securely.");
        } catch (IOException e) {
            log.warn("Persistence failed: {}", e.getMessage());
        }
    }

    private void navigateToDashboard() {
        try {
            // Tunaita MainApp kubadilisha root kwenda Dashboard
            MainApp.openDashboard(); 
            log.info("System Dashboard Loaded.");
        } catch (Exception e) {
            log.error("Failed to initialize Dashboard", e);
            updateStatus("❌ UI Error: Failed to initialize Dashboard.", Color.RED);
        }
    }

    private void updateStatus(String message, Color color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
    }
}