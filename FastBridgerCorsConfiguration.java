package io.fastbridger.cors;

import io.fastbridger.autoconfigure.FastBridgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * FastBridger CORS Auto-Configuration.
 *
 * <p>Registers both a {@link CorsFilter} (catches requests before security filters)
 * and a {@link WebMvcConfigurer} (covers Spring MVC handler mappings).
 * This dual-layer approach prevents the "CORS crash" where either the filter
 * or the MVC config alone silently misses certain request paths.
 *
 * <p>Handles:
 * <ul>
 *   <li>OPTIONS preflight requests — returns 200 immediately</li>
 *   <li>Wildcard vs. credential-aware origin handling</li>
 *   <li>All modern JS framework dev-server ports by default</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FastBridgeCorsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FastBridgeCorsConfiguration.class);

    private final FastBridgeProperties.Cors corsProps;

    public FastBridgeCorsConfiguration(FastBridgeProperties properties) {
        this.corsProps = properties.getCors();
        log.info("[FastBridger] CORS configured for origins: {}", corsProps.getAllowedOrigins());
    }

    /**
     * Builds the shared {@link CorsConfiguration} from properties.
     * Automatically handles the credentials + wildcard origin restriction.
     */
    private CorsConfiguration buildCorsConfig() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = corsProps.getAllowedOrigins();

        // If credentials are enabled, we cannot use "*" as an origin —
        // Spring will throw an IllegalArgumentException at runtime.
        // Instead we set allowedOriginPatterns which supports "*" safely.
        if (corsProps.isAllowCredentials()) {
            origins.forEach(config::addAllowedOriginPattern);
        } else {
            origins.forEach(config::addAllowedOrigin);
        }

        corsProps.getAllowedMethods().forEach(config::addAllowedMethod);

        if (corsProps.getAllowedHeaders().contains("*")) {
            config.addAllowedHeader("*");
        } else {
            corsProps.getAllowedHeaders().forEach(config::addAllowedHeader);
        }

        corsProps.getExposedHeaders().forEach(config::addExposedHeader);
        config.setAllowCredentials(corsProps.isAllowCredentials());
        config.setMaxAge(corsProps.getMaxAge());

        return config;
    }

    /**
     * {@link CorsFilter} bean — intercepts CORS at the servlet filter level,
     * which fires before Spring Security and MVC dispatcher.
     * This is the primary guard that prevents preflight crashes.
     */
    @Bean
    public CorsFilter fastBridgeCorsFilter() {
        CorsConfigurationSource source = corsConfigurationSource();
        return new CorsFilter(source);
    }

    /**
     * {@link CorsConfigurationSource} used by both the filter and
     * (optionally) Spring Security's {@code httpSecurity.cors()} setup.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = buildCorsConfig();

        for (String pattern : corsProps.getPathPatterns()) {
            source.registerCorsConfiguration(pattern, config);
        }

        return source;
    }

    /**
     * {@link WebMvcConfigurer} fallback — ensures CORS headers are also added
     * by the MVC layer for any requests that bypass the servlet filter.
     */
    @Bean
    public WebMvcConfigurer fastBridgeCorsWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                for (String pattern : corsProps.getPathPatterns()) {
                    var registration = registry.addMapping(pattern);

                    if (corsProps.isAllowCredentials()) {
                        // Use patterns for credentials mode
                        registration.allowedOriginPatterns(
                                corsProps.getAllowedOrigins().toArray(String[]::new)
                        );
                    } else {
                        registration.allowedOrigins(
                                corsProps.getAllowedOrigins().toArray(String[]::new)
                        );
                    }

                    registration
                            .allowedMethods(corsProps.getAllowedMethods().toArray(String[]::new))
                            .allowedHeaders(corsProps.getAllowedHeaders().toArray(String[]::new))
                            .exposedHeaders(corsProps.getExposedHeaders().toArray(String[]::new))
                            .allowCredentials(corsProps.isAllowCredentials())
                            .maxAge(corsProps.getMaxAge());
                }
            }
        };
    }
}
