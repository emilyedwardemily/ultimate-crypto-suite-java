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

    /**
     * Generic request builder: auto-injects the x-api-key header on EVERY method
     * (GET/POST/OPTIONS...) so no caller can forget authentication.
     */
    private static HttpRequest.Builder baseRequest(String endpoint) {
        return AppConfig.withAuth(HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + endpoint))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(20)));
    }

    /** Raw synchronous call with auth header injected + clear auth/HTTP errors. */
    private static HttpResponse<String> sendWithAuth(HttpRequest.Builder builder) throws Exception {
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ApiException(extractError(response.body(), "Invalid API Key"), response.statusCode());
        }
        if (response.statusCode() == 404) {
            throw new ApiException("Server Error: Endpoint not found on Render!", 404);
        }
        if (response.statusCode() >= 500) {
            throw new ApiException("Server Error (HTTP " + response.statusCode() + "): " + extractError(response.body(), "Backend unavailable"), response.statusCode());
        }
        return response;
    }

    /** Inachimba ujumbe halisi wa server (message/detail/error) kutoka kwenye error body. */
    private static String extractError(String body, String fallback) {
        if (body == null || body.isBlank()) return fallback;
        try {
            JSONObject j = new JSONObject(body);
            if (j.has("message")) return j.optString("message", fallback);
            if (j.has("detail")) return j.optString("detail", fallback);
            if (j.has("error")) return j.optString("error", fallback);
        } catch (Exception ignored) { }
        return fallback;
    }

    public static String callPython(String endpoint, String data, String key) throws Exception {
        // 1. Tengeneza Payload ya JSON
        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("key", key);

        // 2. Tuma na Pokea Majibu (Synchronous kwa ajili ya Encryption Logic) — auth header inajichomeka moja kwa moja
        HttpResponse<String> response = sendWithAuth(baseRequest(endpoint).POST(HttpRequest.BodyPublishers.ofString(json.toString())));

        JSONObject responseBody = new JSONObject(response.body());
        
        if (responseBody.has("status") && responseBody.getString("status").equals("success")) {
            return responseBody.getString("result");
        } else {
            throw new ApiException("Server Error: " + responseBody.optString("detail", "Unknown Error"), response.statusCode());
        }
    }

    /** GET with automatic x-api-key header injection. */
    public static String callPythonGet(String endpoint) throws Exception {
        HttpResponse<String> response = sendWithAuth(baseRequest(endpoint).GET());
        return response.body();
    }

    /** OPTIONS (CORS preflight / capability probe) with automatic auth header injection. */
    public static String callPythonOptions(String endpoint) throws Exception {
        HttpResponse<String> response = sendWithAuth(baseRequest(endpoint).method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        return response.body();
    }

    public static JSONObject splitSecret(String secret, int n, int k) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("secret", secret);
        payload.put("n", n);
        payload.put("k", k);

        HttpResponse<String> response = sendWithAuth(baseRequest("/split").POST(HttpRequest.BodyPublishers.ofString(payload.toString())));

        JSONObject resJson = new JSONObject(response.body());
        if (!resJson.optString("status").equals("success")) {
            throw new ApiException("Split failed: " + resJson.optString("detail", "Unknown Error"), response.statusCode());
        }
        return resJson;
    }

    public static String reconstructSecret(JSONArray shares) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("shares", shares);

        HttpResponse<String> response = sendWithAuth(baseRequest("/reconstruct").POST(HttpRequest.BodyPublishers.ofString(payload.toString())));

        JSONObject resJson = new JSONObject(response.body());
        if (!resJson.optString("status").equals("success")) {
            throw new ApiException("Reconstruction failed: " + resJson.optString("detail", "Unknown Error"), response.statusCode());
        }
        return resJson.getString("secret");
    }
}