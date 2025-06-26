package org.ubiquia.acl.generated;


import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"org.ubiquia.acl.generated"})
@EntityScan(basePackages = {"org.ubiquia.acl.generated"})
public class Application {

    /**
    * The main command-line interface for our program.
    *
    * @param args Any command-line arguments.
    */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Ensure that our application always runs in UTC (Zulu) time.
     */
    @PostConstruct
    public void init() {
        // Setting Spring Boot SetTimeZone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
