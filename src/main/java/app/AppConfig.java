package app;

import java.net.http.HttpRequest;

/**
 * ULTIMATE CRYPTO SUITE - GLOBAL API CONFIGURATION (V20.4)
 * Central single source of truth for backend auth credentials.
 *
 * The Node.js gateway (ultimate-crypto-node-gateway.onrender.com) validates every
 * request via the `x-api-key` header (see src/server/index.js). This class keeps
 * that header name + value in ONE place so all clients stay in sync.
 *
 * Priority for the key value:
 *   1. System env var UC_API_KEY   (set via: export UC_API_KEY="...")
 *   2. System env var API_SECRET_KEY
 *
 * The key is read ONLY from the environment. No secret is embedded in source.
 */
public final class AppConfig {

    /** Header name expected by the Node gateway / Python backend (case-insensitive). */
    public static final String API_HEADER = "x-api-key";

    /** API key resolved from the environment. Empty if neither env var is set. */
    public static final String API_KEY = resolveApiKey();

    private AppConfig() { }

    private static String resolveApiKey() {
        String fromEnv = System.getenv("UC_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        String fromAlt = System.getenv("API_SECRET_KEY");
        if (fromAlt != null && !fromAlt.isBlank()) return fromAlt.trim();
        return "";
    }

    /** Appends the authentication header to any HttpRequest.Builder (GET/POST/OPTIONS/...). */
    public static HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        return builder.header(API_HEADER, API_KEY);
    }
}
