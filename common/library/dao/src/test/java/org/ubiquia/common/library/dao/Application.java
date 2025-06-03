package org.ubiquia.common.library.dao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * The main entry point to our application.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.ubiquia"})
@EntityScan(basePackages = {"org.ubiquia"})
public class Application {

    /**
     * The main command-line interface for our program.
     *
     * @param args Any command-line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
