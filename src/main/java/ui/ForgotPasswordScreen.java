package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ULTIMATE CRYPTO SUITE - FORGOT PASSWORD SCREEN [V15.0]
 * PURPOSE: Secure password recovery via Email OTP on Node.js/MongoDB Gateway
 * FLOW: Email -> Send OTP -> Verify OTP + Set New Password
 */
public class ForgotPasswordScreen extends VBox {
    private TextField emailField;
    private TextField otpField;
    private PasswordField newPasswordField;
    private Label statusLabel;
    private Button sendOtpBtn;
    private Button resetBtn;
    private Stage primaryStage;

    private static final String GATEWAY = "https://ultimate-crypto-node-gateway.onrender.com";

    public ForgotPasswordScreen(Stage stage) {
        this.primaryStage = stage;
        setSpacing(15);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        setStyle("-fx-background-color: #050505; -fx-border-color: #39FF14; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label("RECOVER SECURE IDENTITY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        Label hint = new Label("Enter your registered email. We will send a one-time security code.");
        hint.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        hint.setWrapText(true);

        emailField = createStyledField("Registered Email Address");

        otpField = createStyledField("6-Digit Security Code");
        otpField.setVisible(false);
        otpField.setManaged(false);

        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Strong Password");
        newPasswordField.setPrefHeight(45);
        newPasswordField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);

        sendOtpBtn = new Button("SEND SECURITY CODE");
        sendOtpBtn.setMaxWidth(Double.MAX_VALUE);
        sendOtpBtn.setPrefHeight(45);
        sendOtpBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        sendOtpBtn.setOnAction(e -> handleSendOtp());

        resetBtn = new Button("RESET PASSWORD");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setPrefHeight(45);
        resetBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        resetBtn.setVisible(false);
        resetBtn.setManaged(false);
        resetBtn.setOnAction(e -> handleReset());

        Button backBtn = new Button("<< Abort & Back to Login");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58a6ff; -fx-cursor: hand; -fx-font-size: 12px;");
        backBtn.setOnAction(e -> primaryStage.getScene().setRoot(new LoginScreen(primaryStage)));

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.YELLOW);
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(title, hint, emailField, sendOtpBtn, otpField, newPasswordField, resetBtn, backBtn, statusLabel);
    }

    private TextField createStyledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefHeight(45);
        f.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");
        return f;
    }

    private void handleSendOtp() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            updateStatus("⚠️ Enter a valid registered email!", Color.RED);
            return;
        }

        sendOtpBtn.setDisable(true);
        updateStatus("📡 Requesting security code from Cloud Gateway...", Color.CYAN);

        try {
            JSONObject data = new JSONObject();
            data.put("email", email);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY + "/api/auth/send-otp"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) {
                        javafx.application.Platform.runLater(() -> {
                            updateStatus("✅ Code sent! Check your email inbox, then enter it below.", Color.LIME);
                            otpField.setVisible(true);
                            otpField.setManaged(true);
                            newPasswordField.setVisible(true);
                            newPasswordField.setManaged(true);
                            resetBtn.setVisible(true);
                            resetBtn.setManaged(true);
                            sendOtpBtn.setVisible(false);
                            sendOtpBtn.setManaged(false);
                            sendOtpBtn.setDisable(false);
                        });
                    } else {
                        updateStatus("❌ Failed to send code (" + res.statusCode() + "). Try again.", Color.RED);
                        sendOtpBtn.setDisable(false);
                    }
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        updateStatus("🌐 Gateway unreachable: Render is waking up, try again in 30s.", Color.ORANGE);
                        sendOtpBtn.setDisable(false);
                    });
                    return null;
                });
        } catch (Exception ex) {
            updateStatus("Fatal: System Kernel Failure during OTP request.", Color.RED);
            sendOtpBtn.setDisable(false);
        }
    }

    private void handleReset() {
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        String password = newPasswordField.getText().trim();

        if (email.isEmpty() || otp.isEmpty() || password.isEmpty()) {
            updateStatus("⚠️ Fill in the code and your new password!", Color.RED);
            return;
        }
        if (password.length() < 6) {
            updateStatus("⚠️ New password must be at least 6 characters!", Color.RED);
            return;
        }

        resetBtn.setDisable(true);
        updateStatus("🔐 Verifying code and updating identity...", Color.CYAN);

        try {
            JSONObject data = new JSONObject();
            data.put("email", email);
            data.put("otp", otp);
            data.put("password", password);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY + "/api/auth/reset-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) {
                        javafx.application.Platform.runLater(() -> {
                            updateStatus("✅ Password updated! Redirecting to login...", Color.LIME);
                            try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            primaryStage.getScene().setRoot(new LoginScreen(primaryStage));
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            updateStatus("❌ Reset failed: invalid code or server error (" + res.statusCode() + ").", Color.RED);
                            resetBtn.setDisable(false);
                        });
                    }
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        updateStatus("🌐 Gateway unreachable: Render is waking up, try again in 30s.", Color.ORANGE);
                        resetBtn.setDisable(false);
                    });
                    return null;
                });
        } catch (Exception ex) {
            updateStatus("Fatal: System Kernel Failure during password reset.", Color.RED);
            resetBtn.setDisable(false);
        }
    }

    private void updateStatus(String message, Color color) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
    }
}
