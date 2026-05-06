package io.fastbridger.annotation;

import io.fastbridger.autoconfigure.FastBridgeAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Activates FastBridger manually (for use when auto-configuration is disabled).
 *
 * <p>Add to your Spring Boot main class:
 * <pre>{@code
 * @SpringBootApplication
 * @EnableFastBridger
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * }</pre>
 *
 * <p>This is optional when using the starter — auto-configuration activates automatically.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FastBridgeAutoConfiguration.class)
public @interface EnableFastBridger {
}
