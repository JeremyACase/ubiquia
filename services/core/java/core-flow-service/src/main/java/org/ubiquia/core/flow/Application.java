package org.ubiquia.core.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * The top level application class for our Flow Service.
 *
 * @author <a href="jeremycase@odysseyconsult.com">Jeremy Case</a>
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "org.ubiquia.common.library.api.repository",
    "org.ubiquia.core.flow.repository"})
@ComponentScan(basePackages = {
    "org.ubiquia.core.flow",
    "org.ubiquia.common.library.advice",
    "org.ubiquia.common.library.api",
    "org.ubiquia.common.library.dao",
    "org.ubiquia.common.library.implementation"})
@EntityScan(basePackages = {"org.ubiquia.common.model.ubiquia"})
@EnableRetry
public class Application {

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

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

    /**
     * Something - some library - is overwriting our object mapping config;
     * force our configuration on a context refresh.
     * Without this method, the controllers will return nanosecond representation
     * of timestamps when queried; with the method, they will return properly-formatted
     * timestamps.
     *
     * @param event The guilty offender.
     */
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        handlerAdapter
            .getMessageConverters()
            .stream()
            .forEach(x -> {
                if (x instanceof MappingJackson2HttpMessageConverter jsonMessageConverter) {
                    jsonMessageConverter.setObjectMapper(this.objectMapper);
                }
            });
    }
}
