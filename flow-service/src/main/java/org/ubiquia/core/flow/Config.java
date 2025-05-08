package org.ubiquia.core.flow;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * A top-level configuration class for AMIGOS.
 */
@Configuration
@EnableWebMvc
public class Config implements WebMvcConfigurer {

    protected static final Logger logger = LoggerFactory.getLogger(Config.class);

    /**
     * Rest template bean.
     *
     * @return our REST template.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Web client bean.
     *
     * @return Our web client.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    /**
     * Ensure that AMIGOS is always assuming UTC times.
     */
    @PostConstruct
    public void init() {
        // Setting Spring Boot SetTimeZone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Bean for micrometer.
     *
     * @return A meter registry.
     */
    @Bean
    public MeterRegistry getMeterRegistry() {
        return new CompositeMeterRegistry();
    }

    /**
     * Bean towards using time aspect for micrometer.
     *
     * @param registry The registry we're using time aspect for.
     * @return A timedAspect bean.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Bean to track hikari metrics.
     *
     * @param registry   The micrometer registry.
     * @param datasource The hikari data source.
     * @return Factory to track hikari metrics.
     */
    @Bean
    public MicrometerMetricsTrackerFactory hikariMetricFactory(
        MeterRegistry registry,
        HikariDataSource datasource) {
        var track = new MicrometerMetricsTrackerFactory(registry);
        datasource.setMetricsTrackerFactory(track);
        return track;
    }
}
