package org.ubiquia.core.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Ubiquia Communication Service.
 *
 * <p>
 * This Spring Boot application:
 * </p>
 * <ul>
 *   <li>Excludes {@link DataSourceAutoConfiguration} since this service does not use a local datasource.</li>
 *   <li>Enables scheduled tasks via {@link EnableScheduling} (e.g., background pollers).</li>
 *   <li>Scans multiple base packages for components, APIs, and shared implementations.</li>
 * </ul>
 *
 * <p>
 * The {@link ObjectMapper} is autowired for JSON (de)serialization needs across the service.
 * </p>
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {
    "org.ubiquia.core.communication",
    "org.ubiquia.common.library.advice",
    "org.ubiquia.common.library.api",
    "org.ubiquia.common.library.implementation"})
@EnableScheduling
public class Application {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
