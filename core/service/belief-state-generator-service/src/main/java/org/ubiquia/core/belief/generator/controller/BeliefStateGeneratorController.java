package org.ubiquia.core.belief.generator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.model.ubiquia.embeddable.DomainGeneration;

@RestController
@RequestMapping("/ubiquia/belief-state-generator")
public class BeliefStateGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateGeneratorController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @PutMapping("/generate/domain")
    @Transactional
    public DomainGeneration register(@RequestBody @Valid final DomainGeneration domainGeneration)
        throws JsonProcessingException {

        logger.info("Received a domain generation request: {}",
            this.objectMapper.writeValueAsString(domainGeneration));

        return domainGeneration;
    }
}



