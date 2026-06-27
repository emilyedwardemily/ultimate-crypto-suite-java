package app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * ULTIMATE CRYPTO SUITE - PYTHON BRIDGE (V14.0)
 * STRATEGY: Secure Communication with Render Python Backend
 */
public class ApiClient {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) 
            .build();

    // URL MPYA: Sasa inatumia link yako ya Render (Python Backend)
    private static final String BASE_URL = "https://ultimate-crypto-python.onrender.com";

    // Hii key lazima ifanane na ile uliyoweka kwenye Environment Variables ya Render
    private static final String API_SECRET = "Emily_Crypto_Secure_2026_KIU";

    public static String callPython(String endpoint, String data, String key) throws Exception {
        // 1. Tengeneza Payload ya JSON
        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("key", key);

        // 2. Jenga Ombi (Request) kwa Usalama kwenda Render
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + endpoint))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_SECRET) // Ulinzi wa API Key
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build();

        // 3. Tuma na Pokea Majibu (Synchronous kwa ajili ya Encryption Logic)
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Handle Makosa ya Kiulinzi
        if (response.statusCode() == 401) {
            throw new Exception("Security Breach: Invalid API Token!");
        }
        
        if (response.statusCode() == 404) {
            throw new Exception("Server Error: Endpoint not found on Render!");
        }

        JSONObject responseBody = new JSONObject(response.body());
        
        if (responseBody.has("status") && responseBody.getString("status").equals("success")) {
            return responseBody.getString("result");
        } else {
            throw new Exception("Server Error: " + responseBody.optString("detail", "Unknown Error"));
        }
    }

    public static JSONObject splitSecret(String secret, int n, int k) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("secret", secret);
        payload.put("n", n);
        payload.put("k", k);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/split"))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_SECRET)
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new Exception("Security Breach: Invalid API Token!");
        }

        JSONObject resJson = new JSONObject(response.body());
        if (!resJson.optString("status").equals("success")) {
            throw new Exception("Split failed: " + resJson.optString("detail", "Unknown Error"));
        }
        return resJson;
    }

    public static String reconstructSecret(JSONArray shares) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("shares", shares);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reconstruct"))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_SECRET)
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new Exception("Security Breach: Invalid API Token!");
        }

        JSONObject resJson = new JSONObject(response.body());
        if (!resJson.optString("status").equals("success")) {
            throw new Exception("Reconstruction failed: " + resJson.optString("detail", "Unknown Error"));
        }
        return resJson.getString("secret");
    }
}