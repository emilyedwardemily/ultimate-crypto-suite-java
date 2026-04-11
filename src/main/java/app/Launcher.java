package app;

import javafx.application.Application;

/**
 * ULTIMATE CRYPTO SUITE - SECURE LAUNCHER (V15.0)
 * STRATEGY: Direct Account-Based Launch
 * REMOVED: Local License Key Verification (Now Cloud-Auth)
 */
public class Launcher {

    public static void main(String[] args) {
        // Maelezo ya kuanzia kwenye Terminal (Forensics)
        System.out.println("=====================================================");
        System.out.println("   ULTIMATE CRYPTO SUITE - V15.0 PROFESSIONAL");
        System.out.println("   STATUS: SECURE CLOUD KERNEL INITIALIZING...");
        System.out.println("   GATEWAY: https://ultimate-crypto-node-gateway.onrender.com");
        System.out.println("=====================================================");

        // 1. Hatua ya kuanzisha JavaFX
        // Launcher sasa hivi haihitaji ku-verify chochote, 
        // kazi ya kuhakiki mtumiaji itafanywa na LoginScreen baada ya UI kufunguka.
        try {
            System.out.println("📡 Launching UI Gateway...");
            Application.launch(MainApp.class, args); 
        } catch (Exception e) {
            System.err.println("❌ CRITICAL UI FAILURE: JavaFX thread could not be initialized.");
            System.err.println("CAUSE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}