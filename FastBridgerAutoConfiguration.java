package io.fastbridger.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fastbridger.client.FastApiClient;
import io.fastbridger.cors.FastBridgeCorsConfiguration;
import io.fastbridger.health.FastApiHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * FastBridger Auto-Configuration.
 *
 * <p>This is the root configuration that Spring Boot's auto-configuration
 * mechanism discovers via {@code META-INF/spring/...AutoConfiguration.imports}.
 *
 * <p>It conditionally wires:
 * <ol>
 *   <li>CORS configuration (when {@code fastbridger.cors.enabled=true})</li>
 *   <li>FastAPI WebClient (when {@code fastbridger.fastapi.enabled=true})</li>
 *   <li>Actuator health indicator (when Actuator is on the classpath)</li>
 * </ol>
 */
@AutoConfiguration
@EnableConfigurationProperties(FastBridgeProperties.class)
@Import(FastBridgeCorsConfiguration.class)
public class FastBridgeAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FastBridgeAutoConfiguration.class);

    public FastBridgeAutoConfiguration() {
        log.info("════════════════════════════════════════");
        log.info("  FastBridger Spring Boot Starter v1.0.0");
        log.info("  CORS + FastAPI client initializing...");
        log.info("════════════════════════════════════════");
    }

    /**
     * Registers a Jackson {@link ObjectMapper} with:
     * <ul>
     *   <li>Java 8 date/time support (JSR-310)</li>
     *   <li>Timestamps written as ISO strings, not epoch numbers</li>
     *   <li>Unknown properties ignored (FastAPI response evolution)</li>
     * </ul>
     *
     * Only registered if no {@code ObjectMapper} bean already exists.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper fastBridgeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );
        return mapper;
    }

    /**
     * Registers the {@link FastApiClient} bean when:
     * <ul>
     *   <li>{@code fastbridger.fastapi.enabled=true} (default)</li>
     *   <li>No existing {@code FastApiClient} bean is present</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(FastApiClient.class)
    @ConditionalOnProperty(prefix = "fastbridger.fastapi", name = "enabled", matchIfMissing = true)
    public FastApiClient fastApiClient(FastBridgeProperties properties,
                                       ObjectMapper objectMapper) {
        return new FastApiClient(properties.getFastapi(), objectMapper);
    }

    /**
     * Actuator health indicator for FastAPI connectivity.
     * Only registered when Spring Boot Actuator is on the classpath.
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnProperty(prefix = "fastbridger.fastapi", name = "enabled", matchIfMissing = true)
    public FastApiHealthIndicator fastApiHealthIndicator(FastApiClient client,
                                                          FastBridgeProperties properties) {
        return new FastApiHealthIndicator(client, properties.getFastapi());
    }
}
