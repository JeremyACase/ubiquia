package org.ubiquia.common.library.belief.state.libraries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"org.ubiquia"})
@EntityScan(basePackages = {"org.ubiquia"})
@EnableJpaRepositories(basePackages = {"org.ubiquia.common.library"})
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
