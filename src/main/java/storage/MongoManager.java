package storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONObject;

/**
 * ULTIMATE CRYPTO SUITE - STORAGE MANAGER (V14.0 - PRODUCTION)
 * STRATEGY: Cloud Sync via Render Production URL
 * SECURITY: Header-based Authentication & SSL Encryption
 */
public class MongoManager {

    // USALAMA: Inasomwa kutoka AppConfig (env UC_API_KEY / API_SECRET_KEY, au fallback ya Render)
    private static final String API_SECRET_KEY = app.AppConfig.API_KEY;
    
    // URL MPYA: Sasa inatumia link yako ya Render badala ya localhost
    private static final String CLOUD_URL = "https://ultimate-crypto-node-gateway.onrender.com/api/vault/sync";
    
    // Client yenye Timeout kuzuia UI Freezing
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * SYNC: Inatuma data iliyofichwa (Encrypted) kwenda MongoDB kupitia Render Server
     */
    public static void sync(String userId, String type, String data) throws Exception {
        try {
            // 1. Tengeneza JSON Payload
            JSONObject payload = new JSONObject();
            payload.put("userId", userId);
            payload.put("service", "Vault");
            payload.put("encryptedData", data);
            payload.put("type", type);

            // 2. Jenga Request ya POST kwenda kwenye URL ya Render
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header(app.AppConfig.API_HEADER, API_SECRET_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // 3. Tuma data (Background)
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenAccept(response -> {
                      if (response.statusCode() == 201 || response.statusCode() == 200) {
                          System.out.println("☁️ [CLOUD] Sync Successful: " + type);
                      } else {
                          System.err.println("⚠️ [CLOUD REJECTED]: " + response.body());
                      }
                  })
                  .exceptionally(ex -> {
                      System.err.println("❌ [NETWORK ERROR]: Check internet or Render Logs. " + ex.getMessage());
                      return null;
                  });

        } catch (Exception e) {
            System.err.println("❌ [STORAGE FAULT]: " + e.getMessage());
            throw e; 
        }
    }
}