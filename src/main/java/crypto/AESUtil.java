package crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays; 
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * ULTIMATE CRYPTO SUITE - AES ENGINE [V14.0 PRODUCTION]
 * Algorithm: AES-256-GCM (Authenticated Encryption)
 * Key Derivation: PBKDF2 with HMAC-SHA256
 */
public class AESUtil {
    private static final int IV_SIZE = 12; 
    private static final int TAG_BIT_LENGTH = 128; 
    private static final int ITERATIONS = 65536; 
    private static final int KEY_LENGTH = 256;
    
    // Salt ya mfumo: Hakikisha hii haibadiliki ili uweze kufungua data zako baadae
    private static final byte[] SALT = "UC_SYSTEM_FIXED_SALT_2026_KIU".getBytes(StandardCharsets.UTF_8);

    /**
     * PBKDF2: Inabadilisha password kuwa ufunguo imara wa AES-256.
     */
    private static SecretKeySpec deriveKey(String password) throws Exception {
        char[] passwordChars = password.toCharArray();
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passwordChars, SALT, ITERATIONS, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            // USALAMA: Futa password kwenye RAM
            Arrays.fill(passwordChars, ' '); 
        }
    }

    /**
     * ENCRYPT: Inaficha data na kuongeza Authentication Tag.
     */
    public static String encrypt(String data, String password) throws Exception {
        if (data == null || data.isEmpty()) return "";
        if (password == null || password.isEmpty()) throw new Exception("Security Key is missing!");

        // 1. IV ya kipekee kwa kila encryption
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv); 
        
        // 2. Setup Cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password), new GCMParameterSpec(TAG_BIT_LENGTH, iv));
        
        // 3. Piga Encryption (Tunatumia StandardCharsets kuhakikisha Swahili characters haziharibiki)
        byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // 4. Unganisha IV + CipherText
        byte[] combined = new byte[IV_SIZE + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, IV_SIZE);
        System.arraycopy(cipherText, 0, combined, IV_SIZE, cipherText.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * DECRYPT: Inahakiki na kurudisha data.
     */
    public static String decrypt(String encryptedData, String password) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) return "";
        if (password == null || password.isEmpty()) throw new Exception("Security Key is missing!");

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            
            if (combined.length < IV_SIZE) throw new Exception("Data is corrupted or too short!");

            // 1. Tenganisha IV
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            
            // 2. Setup Cipher kwa Decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password), new GCMParameterSpec(TAG_BIT_LENGTH, iv));
            
            // 3. Decrypt
            byte[] plainText = cipher.doFinal(combined, IV_SIZE, combined.length - IV_SIZE);
            
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Hii inatokea kama Password ni mbaya au data imebadilishwa
            throw new Exception("Decryption Failed: Invalid Key or Tampered Data.");
        }
    }
}