package io.fastbridger.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fastbridger.autoconfigure.FastBridgeProperties;
import io.fastbridger.exception.FastBridgeException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * FastBridger FastAPI Client.
 *
 * <p>A fully-typed, non-blocking HTTP client for calling FastAPI services.
 * Built on Spring's {@link WebClient} with:
 * <ul>
 *   <li>Automatic JSON serialization via Jackson</li>
 *   <li>Exponential-backoff retry for transient failures</li>
 *   <li>Meaningful error messages (not raw WebClientResponseException)</li>
 *   <li>Optional Bearer token / API Key authentication</li>
 *   <li>Both reactive ({@link Mono}/{@link Flux}) and blocking APIs</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Service
 * public class MyService {
 *
 *     private final FastApiClient fastApi;
 *
 *     public MyService(FastApiClient fastApi) {
 *         this.fastApi = fastApi;
 *     }
 *
 *     public UserDto getUser(Long id) {
 *         return fastApi.get("/users/{id}", UserDto.class, id);
 *     }
 *
 *     public UserDto createUser(CreateUserRequest req) {
 *         return fastApi.post("/users", req, UserDto.class);
 *     }
 *
 *     public List<UserDto> getAllUsers() {
 *         return fastApi.getList("/users", UserDto.class);
 *     }
 * }
 * }</pre>
 */
public class FastApiClient {

    private static final Logger log = LoggerFactory.getLogger(FastApiClient.class);

    private final WebClient webClient;
    private final FastBridgeProperties.FastApi props;
    private final ObjectMapper objectMapper;

    public FastApiClient(FastBridgeProperties.FastApi props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.webClient = buildWebClient();
        log.info("[FastBridger] FastApiClient ready → {}", props.getBaseUrl());
    }

    // ─────────────────────────────────────────────
    // Blocking convenience API (sync)
    // ─────────────────────────────────────────────

    /** GET a single resource (blocking). */
    public <T> T get(String path, Class<T> responseType, Object... uriVars) {
        return getAsync(path, responseType, uriVars).block();
    }

    /** GET a list of resources (blocking). */
    public <T> List<T> getList(String path, Class<T> elementType, Object... uriVars) {
        return getListAsync(path, elementType, uriVars).block();
    }

    /** GET with custom query params (blocking). */
    public <T> T getWithParams(String path, Class<T> responseType,
                               MultiValueMap<String, String> queryParams) {
        return getWithParamsAsync(path, responseType, queryParams).block();
    }

    /** POST a body, receive a typed response (blocking). */
    public <REQ, RES> RES post(String path, REQ body, Class<RES> responseType, Object... uriVars) {
        return postAsync(path, body, responseType, uriVars).block();
    }

    /** PUT a body, receive a typed response (blocking). */
    public <REQ, RES> RES put(String path, REQ body, Class<RES> responseType, Object... uriVars) {
        return putAsync(path, body, responseType, uriVars).block();
    }

    /** PATCH a partial body, receive a typed response (blocking). */
    public <REQ, RES> RES patch(String path, REQ body, Class<RES> responseType, Object... uriVars) {
        return patchAsync(path, body, responseType, uriVars).block();
    }

    /** DELETE, optionally receiving a response body (blocking). */
    public <T> T delete(String path, Class<T> responseType, Object... uriVars) {
        return deleteAsync(path, responseType, uriVars).block();
    }

    /** DELETE with no response body (blocking). */
    public void delete(String path, Object... uriVars) {
        deleteAsync(path, Void.class, uriVars).block();
    }

    // ─────────────────────────────────────────────
    // Reactive API (async)
    // ─────────────────────────────────────────────

    /** GET a single resource (reactive). */
    public <T> Mono<T> getAsync(String path, Class<T> responseType, Object... uriVars) {
        return webClient.get()
                .uri(path, uriVars)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .retryWhen(buildRetry("GET", path))
                .doOnError(this::logError);
    }

    /** GET a list of resources (reactive). */
    public <T> Mono<List<T>> getListAsync(String path, Class<T> elementType, Object... uriVars) {
        ParameterizedTypeReference<List<T>> ref = ParameterizedTypeReference.forType(
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
        return webClient.get()
                .uri(path, uriVars)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(ref)
                .retryWhen(buildRetry("GET", path))
                .doOnError(this::logError);
    }

    /** GET with custom query params (reactive). */
    public <T> Mono<T> getWithParamsAsync(String path, Class<T> responseType,
                                           MultiValueMap<String, String> queryParams) {
        return webClient.get()
                .uri(u -> u.path(path).queryParams(queryParams).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .retryWhen(buildRetry("GET", path))
                .doOnError(this::logError);
    }

    /** POST a body, receive a typed response (reactive). */
    public <REQ, RES> Mono<RES> postAsync(String path, REQ body, Class<RES> responseType,
                                           Object... uriVars) {
        return webClient.post()
                .uri(path, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .doOnError(this::logError);
    }

    /** PUT a body, receive a typed response (reactive). */
    public <REQ, RES> Mono<RES> putAsync(String path, REQ body, Class<RES> responseType,
                                          Object... uriVars) {
        return webClient.put()
                .uri(path, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .doOnError(this::logError);
    }

    /** PATCH a partial body (reactive). */
    public <REQ, RES> Mono<RES> patchAsync(String path, REQ body, Class<RES> responseType,
                                            Object... uriVars) {
        return webClient.patch()
                .uri(path, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .doOnError(this::logError);
    }

    /** DELETE, receive typed response (reactive). */
    public <T> Mono<T> deleteAsync(String path, Class<T> responseType, Object... uriVars) {
        return webClient.delete()
                .uri(path, uriVars)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .retryWhen(buildRetry("DELETE", path))
                .doOnError(this::logError);
    }

    /**
     * Escape hatch: returns the raw {@link WebClient} for advanced use cases
     * (streaming, multipart uploads, custom headers, etc.).
     */
    public WebClient rawClient() {
        return webClient;
    }

    // ─────────────────────────────────────────────
    // Internal Helpers
    // ─────────────────────────────────────────────

    private WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) props.getConnectTimeout().toMillis())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(props.getReadTimeout().toMillis(),
                                TimeUnit.MILLISECONDS)));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(props.getMaxInMemorySize()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client", "FastBridger/1.0");

        // Optional auth headers
        if (props.getBearerToken() != null && !props.getBearerToken().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getBearerToken());
        }
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.defaultHeader("X-API-Key", props.getApiKey());
        }

        // Optional request/response logging
        if (props.isLoggingEnabled()) {
            builder.filter(loggingFilter());
        }

        // Error-normalizing filter
        builder.filter(errorNormalizingFilter());

        return builder.build();
    }

    private Retry buildRetry(String method, String path) {
        return Retry.backoff(props.getMaxRetries(), props.getRetryBackoff())
                .filter(this::isRetryable)
                .doBeforeRetry(sig -> log.warn("[FastBridger] Retrying {} {} (attempt {})",
                        method, path, sig.totalRetries() + 1));
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof FastBridgeException) return false; // already wrapped 4xx/5xx
        // Retry on connection refused, timeout, etc.
        return t instanceof java.net.ConnectException
                || t instanceof java.util.concurrent.TimeoutException
                || t.getCause() instanceof java.net.ConnectException;
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("[empty body]")
                .flatMap(body -> {
                    HttpStatusCode status = response.statusCode();
                    String msg = String.format(
                            "FastAPI responded with HTTP %d for request. Body: %s",
                            status.value(), body);
                    log.error("[FastBridger] {}", msg);
                    return Mono.error(new FastBridgeException(msg));
                });
    }

    private ExchangeFilterFunction errorNormalizingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new FastBridgeException(
                                "FastAPI server error " + clientResponse.statusCode().value()
                                        + ": " + body)));
            }
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("[FastBridger] --> {} {}", req.method(), req.url());
            req.headers().forEach((name, vals) ->
                    log.debug("[FastBridger]   Header: {}={}", name, vals));
            return Mono.just(req);
        });
    }

    private void logError(Throwable t) {
        log.error("[FastBridger] Request failed: {}", t.getMessage());
    }
}
