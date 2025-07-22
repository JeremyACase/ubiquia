package org.ubiquia.test.belief.state.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

@Service
public class AnimalQueryTestModule extends AbstractHelmTestModule {

    private static final Logger logger = LoggerFactory.getLogger(AnimalQueryTestModule.class);

    @Autowired
    private Cache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doTests() {



    }
}

