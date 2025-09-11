package org.ubiquia.core.communication;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Central Spring configuration for the Ubiquia Communication Service.
 *
 * <p>Defines application-wide beans, including:</p>
 * <ul>
 *   <li>A {@link WebClient} preconfigured with JSON content-type.</li>
 *   <li>A classic blocking {@link RestTemplate} for simple HTTP calls.</li>
 *   <li>OpenAPI metadata ({@link OpenAPI}) for Swagger / API docs.</li>
 *   <li>Global timezone initialization (UTC).</li>
 *   <li>Micrometer {@link MeterRegistry} and {@link TimedAspect} for metrics and method timing.</li>
 * </ul>
 */
@Configuration
public class Config {

    protected static final Logger logger = LoggerFactory.getLogger(Config.class);

    /**
     * Builds a reactive {@link WebClient} with a default {@code Content-Type: application/json}.
     *
     * @param builder injected {@link WebClient.Builder}
     * @return a configured {@link WebClient} instance
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Creates a shared {@link RestTemplate} bean for simple blocking HTTP interactions.
     *
     * @return a new {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Supplies base OpenAPI metadata for the service, used by Swagger UI / generators.
     *
     * @return an {@link OpenAPI} instance populated with title, version, and description
     */
    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI().info(new Info()
            .title("Ubiquia Communication Service")
            .version("0.1.0")
            .description("Generated OpenAPI docs for UBIQUIA"));
    }

    /**
     * Initializes global application state after bean construction.
     *
     * <p>Sets the default JVM timezone to UTC to ensure consistent timestamp handling.</p>
     */
    @PostConstruct
    public void init() {
        // Setting Spring Boot SetTimeZone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Provides a {@link MeterRegistry} for Micrometer metrics.
     *
     * <p>Uses a {@link CompositeMeterRegistry}, allowing additional registries
     * (e.g., Prometheus, StatsD) to be added at runtime if desired.</p>
     *
     * @return a composite meter registry
     */
    @Bean
    public MeterRegistry getMeterRegistry() {
        return new CompositeMeterRegistry();
    }

    /**
     * Enables the {@link TimedAspect} so methods annotated with {@code @Timed} are recorded by Micrometer.
     *
     * @param registry the {@link MeterRegistry} used to publish timing metrics
     * @return the {@link TimedAspect} AOP advice bean
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
