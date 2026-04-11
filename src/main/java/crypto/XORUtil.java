package crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * ULTIMATE CRYPTO SUITE - XOR BITWISE ENGINE [V14.0 PRODUCTION]
 * Strategy: High-speed bitwise obfuscation with Base64 safety layering.
 * Purpose: Fast obfuscation for non-sensitive UI elements or logs.
 */
public class XORUtil {

    /**
     * ENCRYPT: Inafanya XOR na kufunga matokeo kwenye Base64 kuzuia data corruption.
     */
    public static String encrypt(String data, String key) {
        // Usalama: Zuia data tupu au key tupu zisivunje mfumo
        if (data == null || data.isEmpty() || key == null || key.isEmpty()) {
            return data; 
        }

        try {
            byte[] dBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] kBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] res = new byte[dBytes.length];

            // Rolling Key Logic: Inazungusha key kulingana na urefu wa data
            for (int i = 0; i < dBytes.length; i++) {
                // XOR Operation: Bitwise level protection
                res[i] = (byte) (dBytes[i] ^ kBytes[i % kBytes.length]);
            }

            // Inarudisha Base64 ili iweze kuhifadhiwa salama kwenye Database/Files
            return Base64.getEncoder().encodeToString(res);

        } catch (Exception e) {
            System.err.println("❌ XOR Encryption Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * DECRYPT: Inatoa data kwenye Base64 na kurudisha asili ya XOR.
     */
    public static String decrypt(String base64Data, String key) {
        if (base64Data == null || base64Data.isEmpty() || key == null || key.isEmpty()) {
            return base64Data;
        }

        try {
            // 1. Decode kutoka Base64 kwanza
            byte[] encryptedBytes = Base64.getDecoder().decode(base64Data);
            byte[] kBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] res = new byte[encryptedBytes.length];

            // 2. Reverse XOR operation (Bitwise)
            for (int i = 0; i < encryptedBytes.length; i++) {
                res[i] = (byte) (encryptedBytes[i] ^ kBytes[i % kBytes.length]);
            }

            // 3. Reconstruct string kwa kutumia UTF-8 safety
            return new String(res, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            // Inatokea kama data imekatika au imekosewa wakati wa kunakili
            return "🛑 Error: Malformed Base64 data. Integrity check failed.";
        } catch (Exception e) {
            return "🛑 Error: Decryption failed. Possible key mismatch.";
        }
    }
}