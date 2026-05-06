package io.fastbridger.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for FastBridger.
 *
 * <p>Configure via {@code application.yml}:
 * <pre>
 * fastbridger:
 *   cors:
 *     allowed-origins:
 *       - "http://localhost:3000"
 *       - "https://myapp.com"
 *   fastapi:
 *     base-url: "http://localhost:8000"
 *     timeout: 10s
 * </pre>
 */
@ConfigurationProperties(prefix = "fastbridger")
public class FastBridgeProperties {

    private final Cors cors = new Cors();
    private final FastApi fastapi = new FastApi();

    public Cors getCors() { return cors; }
    public FastApi getFastapi() { return fastapi; }

    // ─────────────────────────────────────────────
    // CORS Settings
    // ─────────────────────────────────────────────
    public static class Cors {

        /**
         * Enable FastBridger CORS auto-configuration.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Allowed origins for CORS. Supports wildcard patterns.
         * Default: ["http://localhost:3000", "http://localhost:5173", "http://localhost:4200"]
         */
        private List<String> allowedOrigins = List.of(
                "http://localhost:3000",   // React CRA / Next.js
                "http://localhost:5173",   // Vite
                "http://localhost:4200",   // Angular
                "http://localhost:8080"    // Generic JS dev server
        );

        /**
         * Allowed HTTP methods.
         * Default: GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD
         */
        private List<String> allowedMethods = List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        );

        /**
         * Allowed request headers.
         * Default: * (all headers)
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * Headers to expose to the browser JavaScript.
         */
        private List<String> exposedHeaders = List.of(
                "Content-Disposition", "X-Total-Count", "X-Request-Id"
        );

        /**
         * Whether to allow credentials (cookies, auth headers).
         * Note: cannot be used with allowedOrigins = ["*"]
         */
        private boolean allowCredentials = true;

        /**
         * How long (in seconds) the browser caches CORS preflight responses.
         * Default: 3600 (1 hour)
         */
        private long maxAge = 3600L;

        /**
         * URL patterns to apply CORS to.
         * Default: /** (all paths)
         */
        private List<String> pathPatterns = List.of("/**");

        // Getters & Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        public List<String> getExposedHeaders() { return exposedHeaders; }
        public void setExposedHeaders(List<String> exposedHeaders) { this.exposedHeaders = exposedHeaders; }
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
        public List<String> getPathPatterns() { return pathPatterns; }
        public void setPathPatterns(List<String> pathPatterns) { this.pathPatterns = pathPatterns; }
    }

    // ─────────────────────────────────────────────
    // FastAPI Client Settings
    // ─────────────────────────────────────────────
    public static class FastApi {

        /**
         * Enable the FastAPI client bean.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Base URL of the FastAPI service.
         * Default: http://localhost:8000
         */
        private String baseUrl = "http://localhost:8000";

        /**
         * Global request/response timeout.
         * Default: 10s
         */
        private Duration timeout = Duration.ofSeconds(10);

        /**
         * Connection timeout for TCP handshake.
         * Default: 5s
         */
        private Duration connectTimeout = Duration.ofSeconds(5);

        /**
         * Read timeout for receiving data.
         * Default: 10s
         */
        private Duration readTimeout = Duration.ofSeconds(10);

        /**
         * Maximum number of retry attempts on transient failures.
         * Default: 3
         */
        private int maxRetries = 3;

        /**
         * Initial backoff delay between retries.
         * Default: 200ms
         */
        private Duration retryBackoff = Duration.ofMillis(200);

        /**
         * Maximum in-memory buffer size for response bodies (bytes).
         * Default: 10 MB
         */
        private int maxInMemorySize = 10 * 1024 * 1024;

        /**
         * Bearer token for authenticating with FastAPI (e.g. OAuth2).
         * Optional.
         */
        private String bearerToken;

        /**
         * API key to send in X-API-Key header.
         * Optional.
         */
        private String apiKey;

        /**
         * Whether to log request/response details (WARNING: may log sensitive data).
         * Default: false
         */
        private boolean loggingEnabled = false;

        // Getters & Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public Duration getRetryBackoff() { return retryBackoff; }
        public void setRetryBackoff(Duration retryBackoff) { this.retryBackoff = retryBackoff; }
        public int getMaxInMemorySize() { return maxInMemorySize; }
        public void setMaxInMemorySize(int maxInMemorySize) { this.maxInMemorySize = maxInMemorySize; }
        public String getBearerToken() { return bearerToken; }
        public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isLoggingEnabled() { return loggingEnabled; }
        public void setLoggingEnabled(boolean loggingEnabled) { this.loggingEnabled = loggingEnabled; }
    }
}
