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
 * ULTIMATE CRYPTO SUITE - LOGIN SCREEN [V14.5 PROFESSIONAL]
 * STRATEGY: Role-Based Cloud Access (No License Required at Login)
 */
public class LoginScreen extends VBox {
    private TextField emailField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Button loginBtn;
    private Stage primaryStage;

    // GLOBAL SESSION DATA: Hii ndiyo inayotofautisha Feature za Biashara ukiwa ndani
    public static String SESSION_TOKEN = "";
    public static String USER_ROLE = "FREE"; // Default ni FREE
    public static String USERNAME = "";
    public static String SESSION_EMAIL = "";

    public LoginScreen(Stage stage) {
        this.primaryStage = stage;
        setSpacing(20);
        setPadding(new Insets(50));
        setAlignment(Pos.CENTER);
        
        // Cyber-style background (Kali Linux aesthetic)
        setStyle("-fx-background-color: #050505; -fx-border-color: #39FF14; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label("ULTIMATE CRYPTO GATEWAY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        VBox form = new VBox(12);
        form.setAlignment(Pos.CENTER);

        emailField = createStyledField("Operator Email (Email yako)");
        passwordField = new PasswordField();
        passwordField.setPromptText("Security Password");
        passwordField.setPrefHeight(45);
        passwordField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");

        loginBtn = new Button("AUTHORIZE ACCESS");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(50);
        loginBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        loginBtn.setOnAction(e -> handleLogin());

        Hyperlink registerLink = new Hyperlink("Unauthorized? Create a secure identity here");
        registerLink.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 12px;");
        registerLink.setOnAction(e -> primaryStage.getScene().setRoot(new RegisterScreen(primaryStage)));

        Hyperlink forgotLink = new Hyperlink("Forgot your security password? Recover here");
        forgotLink.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        forgotLink.setOnAction(e -> primaryStage.getScene().setRoot(new ForgotPasswordScreen(primaryStage)));

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.web("#ff7b72"));
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        form.getChildren().addAll(emailField, passwordField, loginBtn, registerLink, forgotLink);
        getChildren().addAll(title, form, statusLabel);
    }

    private TextField createStyledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefHeight(45);
        f.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-prompt-text-fill: #555;");
        return f;
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            updateStatus("⚠️ Access Denied: Credentials required!", Color.RED);
            return;
        }

        loginBtn.setDisable(true);
        updateStatus("📡 Initiating Secure Handshake with Render...", Color.CYAN);

        try {
            JSONObject loginData = new JSONObject();
            loginData.put("email", email);
            loginData.put("password", password);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String nodeApiUrl = "https://ultimate-crypto-node-gateway.onrender.com/api/auth/login";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(nodeApiUrl))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", app.AppConfig.API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(loginData.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    String body = response.body();
                    System.out.println("DEBUG: Server Response -> " + body); // Itatusaidia kuona shida kwenye Terminal
                    
                    JSONObject resJson = new JSONObject(body);
                    
                    // IMEBORESHWA: Inakubali kama status ni Success AU kama kuna token imerudi
                    if (response.statusCode() == 200 && (resJson.optString("status").equalsIgnoreCase("Success") || resJson.has("token"))) {
                        
                        // 1. Save Session Token
                        SESSION_TOKEN = resJson.optString("token", "");
                        
                        // 2. Extract User Data
                        JSONObject userObj = resJson.getJSONObject("user");
                        USERNAME = userObj.optString("username", "Operator");
                        SESSION_EMAIL = userObj.optString("email", email);
                        
                        // MABADILIKO MUHIMU: Inasoma Role yoyote (ADMIN/PRO/FREE) na kuigeuza kuwa herufi kubwa
                        USER_ROLE = userObj.optString("role", "FREE").toUpperCase(); 

                        javafx.application.Platform.runLater(() -> {
                            // Sasa hapa itakubali hata kama wewe ni ADMIN
                            updateStatus("✅ Access Granted! Identity: " + USERNAME + " [" + USER_ROLE + "]", Color.LIME);
                            
                            try {
                                // Tunachelewa kidogo (Delay) ili uone ujumbe wa mafanikio
                                Thread.sleep(500); 
                                Dashboard dashboard = new Dashboard(); 
                                primaryStage.getScene().setRoot(dashboard);
                                primaryStage.setTitle("UC-Suite Pro | " + USERNAME + " (" + USER_ROLE + ")");
                            } catch (Exception e) {
                                updateStatus("❌ UI Error: Check Dashboard.java", Color.ORANGE);
                                loginBtn.setDisable(false);
                            }
                        });
                    } else {
                        String msg = resJson.optString("message", "Invalid Credentials");
                        updateStatus("❌ Unauthorized: " + msg, Color.RED);
                        loginBtn.setDisable(false);
                    }
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        updateStatus("🌐 Server Sleep: Render is waking up. Try again in 20s.", Color.ORANGE);
                        loginBtn.setDisable(false);
                    });
                    return null;
                });

        } catch (Exception ex) {
            updateStatus("Fatal Error: Encryption Bridge Failure.", Color.RED);
            loginBtn.setDisable(false);
        }
    }

    private void updateStatus(String message, Color color) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
    }
}