package crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * ULTIMATE CRYPTO SUITE - RSA ENGINE [V14.0 PRODUCTION]
 * Algorithm: RSA 2048-bit (Asymmetric)
 * Padding: OAEP with SHA-256
 */
public class RSAUtil {

    private static final String ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * GENERATE KEYPAIR: Inatengeneza Public na Private Key za 2048-bit.
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); 
        return generator.generateKeyPair();
    }

    /**
     * ENCRYPT: Inaficha data kwa kutumia Public Key.
     */
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        if (data == null || data.isEmpty()) return "";
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        
        // OAEP Parameter Spec: Inahakikisha ulinzi dhidi ya mashambulizi ya kisasa
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * DECRYPT: Inafungua data kwa kutumia Private Key.
     */
    public static String decrypt(String encryptedData, PrivateKey privateKey) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) return "";
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData.replaceAll("\\s+", ""));
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // --- UTILITIES ZA ZIADA KWA AJILI YA CLOUD SYNC ---

    /**
     * Inabadilisha Public Key kwenda String ili uweze kuisave MongoDB.
     */
    public static String publicKeyToString(PublicKey pubKey) {
        return Base64.getEncoder().encodeToString(pubKey.getEncoded());
    }

    /**
     * Inabadilisha Private Key kwenda String (PKCS8) kwa ajili ya kuonyesha/kuhifadhi.
     */
    public static String privateKeyToString(PrivateKey privKey) {
        return Base64.getEncoder().encodeToString(privKey.getEncoded());
    }

    /**
     * Inabadilisha String ya Base64 kurudi kuwa PublicKey object.
     */
    public static PublicKey stringToPublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.replaceAll("\\s+", ""));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}