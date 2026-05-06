package io.fastbridger.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Base exception for all FastBridger errors.
 */
public class FastBridgeException extends RuntimeException {

    public FastBridgeException(String message) {
        super(message);
    }

    public FastBridgeException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ─────────────────────────────────────────────────────────────────────────────

class FastApiClientException extends FastBridgeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public FastApiClientException(HttpStatusCode statusCode, String responseBody) {
        super(String.format("FastAPI returned HTTP %s: %s", statusCode.value(), responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() { return statusCode; }
    public String getResponseBody() { return responseBody; }
}

class FastApiConnectionException extends FastBridgeException {

    public FastApiConnectionException(String baseUrl, Throwable cause) {
        super(String.format("Could not connect to FastAPI at '%s'. Is it running?", baseUrl), cause);
    }
}

class FastApiTimeoutException extends FastBridgeException {

    public FastApiTimeoutException(String method, String path) {
        super(String.format("FastAPI request timed out: %s %s", method, path));
    }
}

class FastApiSerializationException extends FastBridgeException {

    public FastApiSerializationException(String message, Throwable cause) {
        super("Failed to serialize/deserialize FastAPI payload: " + message, cause);
    }
}
