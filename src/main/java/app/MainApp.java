package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.Dashboard;
import ui.LoginScreen;

/**
 * ULTIMATE CRYPTO SUITE - V15.0 [PRODUCTION ACCOUNT EDITION]
 * STATUS: LOGIN-BASED AUTHENTICATION ACTIVE
 */
public class MainApp extends Application {

    public static final String SECURE_TOKEN = ".sys_auth.bin";
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        System.out.println("🛡️ UC-Suite Kernel: Initializing Secure Gateway...");

        // Hatua ya 1: Angalia kama mtumiaji tayari ana session ya zamani
        if (checkPersistentLogin()) {
            System.out.println("🔓 Persistent Session Found: Loading Dashboard...");
            openDashboard();
        } else {
            System.out.println("🔐 No Session: Redirecting to Login...");
            showLogin();
        }
    }

    /**
     * Inafungua Dashboard
     */
    public static void openDashboard() {
        Platform.runLater(() -> {
            try {
                if (primaryStage == null) return;
                
                Dashboard root = new Dashboard();
                Scene scene = new Scene(root, 1200, 800);
                
                // Jaribu kuweka CSS kama ipo
                try {
                    String css = MainApp.class.getResource("/style.css").toExternalForm();
                    scene.getStylesheets().add(css);
                } catch (Exception ignored) {}

                primaryStage.setTitle("UC-Suite PRO | Operator: " + LoginScreen.USERNAME + " [" + LoginScreen.USER_ROLE + "]");
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();
                
            } catch (Exception e) {
                System.err.println("❌ UI CRASH: " + e.getMessage());
                showLogin(); // Ikifeli dashboard, rudi login
            }
        });
    }

    /**
     * Inatumia LoginScreen.java tuliyotengeneza awali
     */
    public static void showLogin() {
        Platform.runLater(() -> {
            LoginScreen loginRoot = new LoginScreen(primaryStage);
            Scene loginScene = new Scene(loginRoot, 600, 700);
            
            primaryStage.setScene(loginScene);
            primaryStage.setTitle("UC-Suite | Gateway Handshake");
            primaryStage.centerOnScreen();
            primaryStage.show();
        });
    }

    /**
     * Inahakiki kama mtumiaji alishawahi ku-login hivi karibuni
     */
    private static boolean checkPersistentLogin() {
        try {
            Path path = Paths.get(SECURE_TOKEN);
            if (!Files.exists(path)) return false;
            
            String encoded = Files.readString(path).trim();
            if (encoded.isEmpty()) return false;
            
            String data = new String(Base64.getDecoder().decode(encoded));
            String[] p = data.split("::");
            
            // Kumbuka: Hapa p[0] ni email na p[1] ni password
            // Tunaiweka kwenye LoginScreen ili iweze kutumika kwenye Dashboard
            if (p.length >= 2) {
                // Hapa unaweza kuita method ya login kimya kimya (Silent Login)
                // Kwa sasa turudishe false ili kila mtu alazimike ku-login kwa usalama
                return false; 
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}