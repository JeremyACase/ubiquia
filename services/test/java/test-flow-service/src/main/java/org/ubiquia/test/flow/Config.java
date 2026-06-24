package org.ubiquia.test.flow;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Spring configuration for the test flow service. */
@Configuration
public class Config {

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

    /** Sets the JVM default timezone to UTC on startup. */
    @PostConstruct
    public void init() {
        // Setting Spring Boot SetTimeZone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
