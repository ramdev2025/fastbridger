# FastBridger Spring Boot Starter

> Stop CORS crashes. Talk to FastAPI. Zero boilerplate.

FastBridger is a drop-in Spring Boot starter that solves two of the most
common pain points in modern Java backends:

1. **CORS crashes** — JavaScript frontends (React, Vue, Angular, Svelte) get
   blocked by the browser before they even hit your API. FastBridger configures
   a dual-layer CORS setup that *actually* works.

2. **FastAPI connectivity** — calling a Python/FastAPI microservice from
   Spring Boot normally means writing a boilerplate `WebClient` setup with
   manual retry and error handling. FastBridger gives you a typed, ready-to-inject
   `FastApiClient` bean.

---

## Quick Start

### 1. Add the dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.fastbridger</groupId>
    <artifactId>fastbridger-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure `application.yml`

```yaml
fastbridger:
  cors:
    allowed-origins:
      - "http://localhost:3000"    # React dev server
      - "http://localhost:5173"    # Vite dev server
      - "https://myapp.com"       # Production

  fastapi:
    base-url: "http://localhost:8000"
```

### 3. Done. Inject and use.

```java
@Service
public class UserService {

    private final FastApiClient fastApi;

    public UserService(FastApiClient fastApi) {
        this.fastApi = fastApi;
    }

    // Blocking (synchronous) calls
    public UserDto getUser(Long id) {
        return fastApi.get("/users/{id}", UserDto.class, id);
    }

    public List<UserDto> getAllUsers() {
        return fastApi.getList("/users", UserDto.class);
    }

    public UserDto createUser(CreateUserRequest req) {
        return fastApi.post("/users", req, UserDto.class);
    }

    public UserDto updateUser(Long id, UpdateUserRequest req) {
        return fastApi.put("/users/{id}", req, UserDto.class, id);
    }

    public void deleteUser(Long id) {
        fastApi.delete("/users/{id}", id);
    }

    // Reactive (async) calls — returns Mono/Flux
    public Mono<UserDto> getUserAsync(Long id) {
        return fastApi.getAsync("/users/{id}", UserDto.class, id);
    }
}
```

---

## CORS — How It Works

Most CORS setups fail because they configure *either* the `CorsFilter`
*or* the `WebMvcConfigurer` — not both. FastBridger registers **both**:

```
Browser preflight (OPTIONS)
       │
       ▼
  CorsFilter  ──────────────────► Returns 200 + headers immediately
  (Servlet level, before Security)
       │
       ▼
  WebMvcConfigurer  ────────────► Adds headers to all MVC responses
  (Handler level)
```

**Credentials + Wildcard fix:** When `allow-credentials: true`, browsers
reject `Access-Control-Allow-Origin: *`. FastBridger detects this and
automatically switches to `allowedOriginPatterns` which echoes back the
exact origin while still supporting wildcard patterns.

### With Spring Security

If you use Spring Security, wire the `CorsConfigurationSource` bean
into your security config:

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            // ... rest of your config
        return http.build();
    }
}
```

---

## FastAPI Client — Full API Reference

### Blocking (synchronous)

| Method | Signature | Notes |
|--------|-----------|-------|
| `get` | `get(path, Class<T>, Object... uriVars)` | GET single resource |
| `getList` | `getList(path, Class<T>, Object... uriVars)` | GET `List<T>` |
| `getWithParams` | `getWithParams(path, Class<T>, MultiValueMap<String,String>)` | GET with query params |
| `post` | `post(path, body, Class<RES>, Object... uriVars)` | POST with body |
| `put` | `put(path, body, Class<RES>, Object... uriVars)` | PUT with body |
| `patch` | `patch(path, body, Class<RES>, Object... uriVars)` | PATCH with body |
| `delete` | `delete(path, Class<T>, Object... uriVars)` | DELETE with response |
| `delete` | `delete(path, Object... uriVars)` | DELETE, no response |
| `rawClient` | `rawClient()` | Raw `WebClient` for advanced use |

### Reactive (async)

All blocking methods have an `Async` variant returning `Mono<T>` or `Mono<List<T>>`.

### Query parameters

```java
MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
params.add("status", "active");
params.add("page", "1");

List<UserDto> users = fastApi.getWithParams("/users", UserDto.class, params);
// → GET /users?status=active&page=1
```

### Advanced: raw WebClient

```java
// Multipart file upload to FastAPI
MultipartBodyBuilder builder = new MultipartBodyBuilder();
builder.part("file", fileResource);

fastApi.rawClient()
    .post()
    .uri("/upload")
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .body(BodyInserters.fromMultipartData(builder.build()))
    .retrieve()
    .bodyToMono(UploadResult.class)
    .block();
```

---

## Authentication

### Bearer Token (JWT / OAuth2)

```yaml
fastbridger:
  fastapi:
    bearer-token: "eyJhbGciOiJSUzI1NiJ9..."
```

### API Key

```yaml
fastbridger:
  fastapi:
    api-key: "sk-my-secret-key"
    # Sent as: X-API-Key: sk-my-secret-key
```

### Dynamic tokens (per-request)

Use the raw client for per-request auth:

```java
fastApi.rawClient()
    .get()
    .uri("/secure/resource")
    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.currentToken())
    .retrieve()
    .bodyToMono(SecureDto.class)
    .block();
```

---

## Health Check

With Spring Boot Actuator on the classpath, FastBridger automatically
registers a health indicator:

```
GET /actuator/health/fastapi

{
  "status": "UP",
  "details": {
    "fastapi.url": "http://localhost:8000",
    "fastapi.response": "ok",
    "checkedAt": "2024-05-05T12:00:00Z"
  }
}
```

---

## All Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `fastbridger.cors.enabled` | `true` | Enable CORS auto-config |
| `fastbridger.cors.allowed-origins` | `[localhost:3000, :5173, :4200, :8080]` | Allowed JS origins |
| `fastbridger.cors.allowed-methods` | `[GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]` | Allowed HTTP methods |
| `fastbridger.cors.allowed-headers` | `[*]` | Allowed request headers |
| `fastbridger.cors.exposed-headers` | `[Content-Disposition, X-Total-Count, X-Request-Id]` | Headers browser can read |
| `fastbridger.cors.allow-credentials` | `true` | Allow cookies/auth headers |
| `fastbridger.cors.max-age` | `3600` | Preflight cache (seconds) |
| `fastbridger.cors.path-patterns` | `[/**]` | URL patterns to apply CORS to |
| `fastbridger.fastapi.enabled` | `true` | Enable FastAPI client bean |
| `fastbridger.fastapi.base-url` | `http://localhost:8000` | FastAPI base URL |
| `fastbridger.fastapi.timeout` | `10s` | Global timeout |
| `fastbridger.fastapi.connect-timeout` | `5s` | TCP connection timeout |
| `fastbridger.fastapi.read-timeout` | `10s` | Response read timeout |
| `fastbridger.fastapi.max-retries` | `3` | Retry attempts on transient errors |
| `fastbridger.fastapi.retry-backoff` | `200ms` | Initial retry delay |
| `fastbridger.fastapi.max-in-memory-size` | `10485760` (10 MB) | Max response buffer |
| `fastbridger.fastapi.bearer-token` | _(none)_ | Bearer token for FastAPI |
| `fastbridger.fastapi.api-key` | _(none)_ | API key (X-API-Key header) |
| `fastbridger.fastapi.logging-enabled` | `false` | Log requests/responses |

---

## Disable Selectively

```yaml
fastbridger:
  cors:
    enabled: false    # Disable CORS (manage it yourself)
  fastapi:
    enabled: false    # Disable FastAPI client (don't need it)
```

---

## Building from Source

```bash
git clone https://github.com/your-org/fastbridger-spring-boot-starter
cd fastbridger-spring-boot-starter
mvn clean install
```

### Run tests

```bash
mvn test
```

---

## Requirements

- Java 17+
- Spring Boot 3.2+

---

## License

MIT © FastBridger Contributors
