package org.ubiquia.common.test.helm.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.ubiquia.IngressResponse;

@Service
public class GenericAclPostAndRetriever<T> implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(GenericAclPostAndRetriever.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
    }

    public T postAndRetrieve(
        final String postUrl,
        final String getUrl,
        final T object,
        final Class<T> clazz)
        throws JsonProcessingException {

        logger.debug("Posting object: {}",
            this.objectMapper.writeValueAsString(object));

        logger.info("POSTing to URL: {}", postUrl);

        var postResponse = this.restTemplate.postForEntity(
            postUrl,
            object,
            IngressResponse.class);

        var queryUrl = getUrl + postResponse.getBody().getId();
        logger.info("GETting from URL: {}", queryUrl);
        var response = this.restTemplate.getForEntity(queryUrl, clazz);

        return response.getBody();
    }
}