package app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.json.JSONObject;

/**
 * ULTIMATE CRYPTO SUITE - DATABASE & AUTH MANAGER [V14.0 PRODUCTION]
 * SECURITY STRATEGY: Cloud Sync via Render + API Gateway
 */
public class DatabaseManager {
    
    // Siri inasomwa kutoka AppConfig: env UC_API_KEY, else API_SECRET_KEY, else fallback.
    // Hakikisha ume-set hii kwenye Kali Linux kwa kutumia: export UC_API_KEY="Emily_Crypto_Secure_2026_KIU"
    private static final String API_SECRET_KEY = AppConfig.API_KEY;
    
    // API Endpoints - Zimeelekezwa kwenye Render Production
    private static final String LICENSE_API = "https://ultimate-crypto-python.onrender.com/verify-license";
    private static final String CLOUD_GATEWAY = "https://ultimate-crypto-node-gateway.onrender.com/api/vault/sync";
    
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)) 
            .build();

    /**
     * ALIAS METHOD: Inaitumia authenticate() ndani yake.
     */
    public static boolean verifyWithCloud(String email, String licenseKey) {
        return authenticate(email, licenseKey);
    }

    /**
     * AUTHENTICATE: Inahakiki leseni kwa kutumia HWID kupitia Python Backend ya Render.
     */
    public static boolean authenticate(String email, String licenseKey) {
        if (email == null || !email.contains("@")) return false;

        String hwid = LicenseManager.getHardwareID().trim();

        try {
            JSONObject payload = new JSONObject();
            payload.put("license_key", licenseKey.trim());
            payload.put("hardware_id", hwid);
            payload.put("client_timestamp", System.currentTimeMillis());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LICENSE_API))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", API_SECRET_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject resObj = new JSONObject(response.body());
                return "success".equalsIgnoreCase(resObj.optString("status"));
            } else {
                System.err.println("⚠️ [AUTH REJECTED] Code: " + response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ [AUTH CRITICAL ERROR]: Check Internet. " + e.getMessage());
            return false;
        }
    }

    /**
     * SYNC DATA: Inatuma data iliyofichwa kwenda Cloud kupitia Node.js Gateway ya Render.
     */
    public static void syncData(String userId, String moduleType, String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) return;

        try {
            JSONObject cloudJson = new JSONObject();
            cloudJson.put("userId", userId);
            cloudJson.put("service", "Vault");
            cloudJson.put("type", moduleType);
            cloudJson.put("encryptedData", encryptedData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_GATEWAY))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", API_SECRET_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(cloudJson.toString(), StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenAccept(res -> {
                      if(res.statusCode() == 201 || res.statusCode() == 200) 
                          System.out.println("☁️ [CLOUD] Sync Successful to Render Gateway.");
                      else 
                          System.err.println("⚠️ [CLOUD REJECTED] Check Render Logs. Code: " + res.statusCode());
                  })
                  .exceptionally(ex -> {
                      System.err.println("❌ [CLOUD ASYNC ERROR]: " + ex.getMessage());
                      return null;
                  });

        } catch (Exception e) {
            System.err.println("❌ [SYNC FAULT]: " + e.getMessage());
        }
    }
}