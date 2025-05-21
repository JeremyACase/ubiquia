package org.ubiquia.core.flow.service.command.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.model.entity.FlowEvent;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.visitor.AdapterOpenMessageVisitor;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterPostToAgentCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterPostToAgentCommand.class);

    @Autowired
    private AdapterAgentResponseCommand adapterAgentResponseCommand;

    @Autowired
    private AdapterOpenMessageVisitor adapterOpenMessageVisitor;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private PayloadModelValidator payloadModelValidator;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void tryPostPayloadToAgentSynchronously(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var adapterContext = adapter.getAdapterContext();
        logger.info("POSTing synchronous payload to URI: {}...", adapterContext.getEndpointUri());

        var flowEventTimes = flowEvent.getFlowEventTimes();
        flowEventTimes.setPayloadSentToAgentTime(OffsetDateTime.now());

        var request = new HttpEntity<>(inputPayload, headers);
        ResponseEntity<Object> response = null;
        try {
            response = this.restTemplate.postForEntity(
                adapterContext.getEndpointUri(),
                request,
                Object.class);
        } catch (HttpStatusCodeException e) {
            response = new ResponseEntity<>(
                e.getResponseBodyAsString(),
                e.getResponseHeaders(),
                e.getStatusCode());
        }
        flowEventTimes.setAgentResponseTime(OffsetDateTime.now());
        flowEvent.setHttpResponseCode(response.getStatusCode().value());
        this.adapterAgentResponseCommand.processAgentResponse(flowEvent, adapter, response);
    }

    @Transactional
    public void tryPostInputToAgentAsynchronously(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload) {

        var adapterContext = adapter.getAdapterContext();
        logger.info("POSTing payload asynchronously to URI: {}...",
            adapterContext.getEndpointUri());

        this.adapterOpenMessageVisitor.incrementOpenMessagesFor(adapter);
        var eventTimes = flowEvent.getFlowEventTimes();
        this.webClient
            .post()
            .uri(adapterContext.getEndpointUri())
            .bodyValue(inputPayload)
            .retrieve()
            .toEntity(Object.class)
            .subscribe(
                response -> {
                    this.adapterOpenMessageVisitor.decrementOpenMessagesFor(adapter);
                    eventTimes.setAgentResponseTime(OffsetDateTime.now());
                    flowEvent.setHttpResponseCode(response.getStatusCode().value());
                    logger.info("...got response code {} from agent for batch id {}...",
                        response.getStatusCode(),
                        flowEvent.getBatchId());
                    try {
                        this.adapterAgentResponseCommand.processAgentResponse(
                            flowEvent,
                            adapter,
                            response);
                    } catch (Exception e) {
                        logger.error("ERROR processing agent response: {}", e.getMessage());
                    }
                },
                error -> {
                    if (error instanceof HttpStatusCodeException errorCast) {
                        flowEvent.setHttpResponseCode(errorCast.getStatusCode().value());
                    } else {
                        logger.error("ERROR response: {} ", error.getMessage());
                    }
                    eventTimes.setAgentResponseTime(OffsetDateTime.now());
                    eventTimes.setEventCompleteTime(OffsetDateTime.now());
                    this.adapterOpenMessageVisitor.decrementOpenMessagesFor(adapter);
                    this.flowEventRepository.save(flowEvent);
                }
            );
    }
}
