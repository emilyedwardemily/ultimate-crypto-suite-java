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
 * ULTIMATE CRYPTO SUITE - REGISTER SCREEN [V14.0 PRODUCTION]
 * PURPOSE: Creating secure identities on Node.js/MongoDB Gateway
 */
public class RegisterScreen extends VBox {
    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Button regBtn;
    private Stage primaryStage;

    public RegisterScreen(Stage stage) {
        this.primaryStage = stage;
        setSpacing(15);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        
        // Cyber-style background (Match na Kali Linux environment)
        setStyle("-fx-background-color: #050505; -fx-border-color: #39FF14; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label("ESTABLISH SECURE IDENTITY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        usernameField = createStyledField("New Username");
        emailField = createStyledField("Valid Email Address");
        
        passwordField = new PasswordField();
        passwordField.setPromptText("Strong Password");
        passwordField.setPrefHeight(45);
        passwordField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");

        regBtn = new Button("CREATE ACCOUNT");
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.setPrefHeight(45);
        regBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        regBtn.setOnAction(e -> handleRegistration());

        Button backBtn = new Button("<< Abort & Back to Login");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58a6ff; -fx-cursor: hand; -fx-font-size: 12px;");
        backBtn.setOnAction(e -> primaryStage.getScene().setRoot(new LoginScreen(primaryStage)));

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.YELLOW);
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(title, usernameField, emailField, passwordField, regBtn, backBtn, statusLabel);
    }

    private TextField createStyledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefHeight(45);
        f.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");
        return f;
    }

    private void handleRegistration() {
        String user = usernameField.getText().trim();
        String mail = emailField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
            updateStatus("⚠️ Access Denied: All intelligence fields required!", Color.RED);
            return;
        }

        regBtn.setDisable(true);
        updateStatus("📡 Transmitting to Cloud Gateway...", Color.CYAN);

        try {
            JSONObject data = new JSONObject();
            data.put("username", user);
            data.put("email", mail);
            data.put("password", pass);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            // URL ya Node.js Register Endpoint kule Render
            String registerUrl = "https://ultimate-crypto-node-gateway.onrender.com/api/auth/register";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registerUrl))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", app.AppConfig.API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 201 || res.statusCode() == 200) {
                        updateStatus("✅ Identity Established! You can now Login.", Color.LIME);
                    } else {
                        updateStatus("❌ Rejected: Email exists or server error (" + res.statusCode() + ")", Color.RED);
                        regBtn.setDisable(false);
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace(); 
                    updateStatus("🌐 Connection Failed: Server is waking up, try again in 30s.", Color.ORANGE);
                    regBtn.setDisable(false);
                    return null;
                });
        } catch (Exception e) {
            updateStatus("Fatal: System Kernel Failure during registration.", Color.RED);
            regBtn.setDisable(false);
            e.printStackTrace();
        }
    }

    private void updateStatus(String message, Color color) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
    }
}