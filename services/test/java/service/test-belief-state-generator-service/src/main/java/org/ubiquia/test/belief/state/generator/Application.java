package org.ubiquia.test.belief.state.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {
    "org.ubiquia.core",
    "org.ubiquia.common",
    "org.ubiquia.test"
})
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

