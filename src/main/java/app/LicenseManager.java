package app;

import java.net.NetworkInterface;
import java.util.Collections;

/**
 * ULTIMATE CRYPTO SUITE - IDENTITY MANAGER (V15.0)
 * STATUS: ACCOUNT-BASED IDENTITY ACTIVE
 * REMOVED: verifyWithServer (Handled by LoginController/DatabaseManager)
 */
public class LicenseManager {

    /**
     * getHardwareID: Inazalisha Fingerprint ya kipekee ya PC.
     * Hii ni muhimu kwa ajili ya Admin Dashboard kuona ni PC gani imetumika.
     */
    public static String getHardwareID() {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Inapata MAC Address ya kwanza inayofanya kazi
            var interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface net : interfaces) {
                if (!net.isLoopback() && net.isUp() && net.getHardwareAddress() != null) {
                    byte[] mac = net.getHardwareAddress();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    break; 
                }
            }
            
            // Inachanganya na specs za System kwa usalama zaidi
            String systemSpecs = System.getProperty("os.name") + 
                                 System.getProperty("os.arch") + 
                                 System.getProperty("user.name") +
                                 Runtime.getRuntime().availableProcessors();
            
            String rawId = sb.toString() + "-" + systemSpecs.hashCode();
            
            // Inarudisha ID safi ya kifaa
            return "UC-NODE-" + Integer.toHexString(rawId.hashCode()).toUpperCase();
            
        } catch (Exception e) {
            // Fallback ikiwa Network Drivers zina shida
            return "UC-GENERIC-" + System.getProperty("user.name").toUpperCase();
        }
    }

    /**
     * NOTE: Method ya 'verifyWithServer' imeondolewa.
     * Software sasa inatumia DatabaseManager.authenticate(email, password)
     * inayopatikana kwenye LoginController.
     */
}