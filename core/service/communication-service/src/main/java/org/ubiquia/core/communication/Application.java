package org.ubiquia.core.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

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
     * The main command-line interface for our program.
     *
     * @param args Any command-line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
