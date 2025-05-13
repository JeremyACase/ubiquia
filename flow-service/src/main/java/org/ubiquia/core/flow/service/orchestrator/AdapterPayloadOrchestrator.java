package org.ubiquia.core.flow.service.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import net.jimblackler.jsongenerator.JsonGeneratorException;
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
import org.ubiquia.core.flow.model.enums.AdapterType;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;
import org.ubiquia.core.flow.service.proxy.TemplateAgentProxy;
import org.ubiquia.core.flow.service.visitor.AdapterOpenMessageVisitor;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterPayloadOrchestrator implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterPayloadOrchestrator.class);

    @Autowired
    private AdapterOpenMessageVisitor adapterOpenMessageVisitor;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Outbox outbox;

    @Autowired
    private PayloadModelValidator payloadModelValidator;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TemplateAgentProxy templateAgentProxy;

    @Autowired
    private StamperVisitor stamperVisitor;

    @Autowired
    private WebClient webClient;

    public Logger getLogger() {
        return logger;
    }

    public void forwardPayload(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final String payload)
        throws JsonProcessingException,
        JsonGeneratorException,
        GenerationException {

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getIsPassthrough()) {
            this.outbox.tryQueueAgentResponse(
                flowEvent,
                payload);
        } else if (adapterContext.getTemplateAgent()) {
            this.templateAgentProxy.proxyAsAgentFor(flowEvent);
        } else {
            this.trySendInputPayloadToAgent(
                flowEvent,
                adapter,
                payload);
        }
    }

    public void trySendInputPayloadToAgent(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload) {

        if (adapter.getAdapterContext().getAdapterType().equals(AdapterType.PUSH)) {
            this.trySendInputToAgentSynchronously(flowEvent, adapter, inputPayload);
        } else {
            this.tryPostInputToAgentAsynchronously(flowEvent, adapter, inputPayload);
        }
    }

    @Transactional
    public ResponseEntity<Object> tryPostPayloadToAgentSynchronously(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload) {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var adapterContext = adapter.getAdapterContext();
        logger.info("POSTing payload to URI: {}...", adapterContext.getEndpointUri());

        var request = new HttpEntity<>(inputPayload, headers);
        ResponseEntity<Object> response = null;
        try {
            flowEvent
                .getFlowEventTimes()
                .setPayloadSentToTransformTime(OffsetDateTime.now());
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
        return response;
    }

    @Transactional
    public ResponseEntity<Object> tryPutPayloadToAgentSynchronously(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var adapterContext = adapter.getAdapterContext();
        logger.info("PUTing payload to URI: {}...", adapterContext.getEndpointUri());

        var request = new HttpEntity<>(inputPayload, headers);
        ResponseEntity<Object> response = null;
        try {
            flowEvent
                .getFlowEventTimes()
                .setPayloadSentToTransformTime(OffsetDateTime.now());
            response = this.restTemplate.exchange(
                adapterContext.getEndpointUri(),
                HttpMethod.PUT,
                request,
                Object.class);
        } catch (HttpStatusCodeException e) {
            response = new ResponseEntity<>(
                e.getResponseBodyAsString(),
                e.getResponseHeaders(),
                e.getStatusCode());
        }
        return response;
    }

    private void processAgentResponse(
        final FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final ResponseEntity<Object> response)
        throws JsonProcessingException, ValidationException, GenerationException {

        var stringifiedPayload = this.objectMapper.writeValueAsString(response.getBody());
        this.payloadModelValidator.tryValidateOutputPayloadFor(stringifiedPayload, adapter);
        this.stamperVisitor.tryStampOutputs(flowEvent, stringifiedPayload);

        if (adapter.getAdapterContext().getAdapterSettings().getIsPersistOutputPayload()) {
            flowEvent.setOutputPayload(stringifiedPayload);
        }

        this.outbox.tryQueueAgentResponse(flowEvent, stringifiedPayload);
    }

    @Transactional
    private void tryPostInputToAgentAsynchronously(
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
                    eventTimes.setDataTransformResponseTime(OffsetDateTime.now());
                    eventTimes.setEventCompleteTime(OffsetDateTime.now());
                    flowEvent.setHttpResponseCode(response.getStatusCode().value());
                    logger.info("...got response from data transform: {} for batch id {}...",
                        response.getStatusCode(),
                        flowEvent.getBatchId());
                    try {
                        this.processAgentResponse(flowEvent, adapter, response);
                    } catch (Exception e) {
                        logger.error("ERROR processing transform response: {}", e.getMessage());
                    }
                },
                error -> {
                    if (error instanceof HttpStatusCodeException errorCast) {
                        flowEvent.setHttpResponseCode(errorCast.getStatusCode().value());
                    } else {
                        logger.error("ERROR response: {} ", error.getMessage());
                    }
                    eventTimes.setDataTransformResponseTime(OffsetDateTime.now());
                    eventTimes.setEventCompleteTime(OffsetDateTime.now());
                    this.adapterOpenMessageVisitor.decrementOpenMessagesFor(adapter);
                    this.flowEventRepository.save(flowEvent);
                }
            );
    }

    @Transactional
    private void trySendInputToAgentSynchronously(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload) {

        var adapterContext = adapter.getAdapterContext();

        ResponseEntity<Object> response = null;
        switch (adapterContext.getEgressSettings().getEgressType()) {

            case POST: {
                response = this.tryPostPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            case PUT: {
                response = this.tryPutPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            default: {
                throw new RuntimeException("ERROR: Cannot forward a payload for egress type: "
                    + adapterContext.getEgressSettings().getEgressType());
            }
        }
        var eventTimes = flowEvent.getFlowEventTimes();
        eventTimes.setDataTransformResponseTime(OffsetDateTime.now());
        flowEvent.setHttpResponseCode(response.getStatusCode().value());
        eventTimes.setEventCompleteTime(OffsetDateTime.now());
    }

}
