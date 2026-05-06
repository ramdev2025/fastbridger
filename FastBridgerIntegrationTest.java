package io.fastbridger;

import io.fastbridger.client.FastApiClient;
import io.fastbridger.autoconfigure.FastBridgeAutoConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FastBridger CORS + FastApiClient.
 */
@SpringBootTest(classes = FastBridgeIntegrationTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "fastbridger.cors.allowed-origins=http://localhost:3000",
        "fastbridger.cors.allow-credentials=true",
        "fastbridger.fastapi.base-url=http://localhost:9999",
        "fastbridger.fastapi.max-retries=1"
})
class FastBridgeIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired FastApiClient fastApiClient;

    static MockWebServer mockFastApi;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockFastApi = new MockWebServer();
        mockFastApi.start(9999);
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockFastApi.shutdown();
    }

    // ─── CORS Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Preflight OPTIONS returns 200 with CORS headers")
    void preflight_shouldReturn200_withCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    }

    @Test
    @DisplayName("Regular GET from allowed origin includes CORS headers")
    void get_fromAllowedOrigin_shouldIncludeCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"));
    }

    @Test
    @DisplayName("Request from unlisted origin is blocked")
    void get_fromUnknownOrigin_shouldNotIncludeCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/test")
                        .header(HttpHeaders.ORIGIN, "http://evil.com"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // ─── FastApiClient Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("FastApiClient is registered as a Spring bean")
    void fastApiClient_shouldBeRegisteredAsBean() {
        assertThat(fastApiClient).isNotNull();
    }

    @Test
    @DisplayName("GET returns deserialized response body")
    void get_shouldReturnDeserializedBody() {
        mockFastApi.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":1,\"name\":\"Alice\"}"));

        TestUser user = fastApiClient.get("/users/1", TestUser.class, 1);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1);
        assertThat(user.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("POST sends body and returns response")
    void post_shouldSendBodyAndReturnResponse() {
        mockFastApi.enqueue(new MockResponse()
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":42,\"name\":\"Bob\"}"));

        TestUser created = fastApiClient.post("/users",
                new TestUser(0, "Bob"), TestUser.class);

        assertThat(created.id()).isEqualTo(42);
        assertThat(created.name()).isEqualTo("Bob");
    }

    // ─── Test Fixtures ────────────────────────────────────────────────────────

    record TestUser(int id, String name) {}

    @RestController
    static class TestController {
        @GetMapping("/api/test")
        String test() { return "ok"; }
    }

    @SpringBootApplication
    static class TestApp {}
}
