package org.ubiquia.core.flow.service.command.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.repository.FlowEventRepository;
import org.ubiquia.core.flow.service.visitor.NodeOpenMessageVisitor;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class NodePostToComponentCommand implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(NodePostToComponentCommand.class);

    @Autowired
    private NodeComponentResponseCommand nodeComponentResponseCommand;

    @Autowired
    private NodeOpenMessageVisitor nodeOpenMessageVisitor;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;

    public Logger getLogger() {
        return logger;
    }

    @Transactional
    public void tryPostPayloadToComponentSynchronously(
        FlowEventEntity flowEventEntity,
        final AbstractNode node,
        final Object inputPayload)
        throws JsonProcessingException {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var nodeContext = node.getNodeContext();
        logger.info("POSTing synchronous payload to URI for node {}: {}...",
            nodeContext.getEndpointUri(),
            nodeContext.getNodeName());

        var flowEventTimes = flowEventEntity.getFlowEventTimes();
        flowEventTimes.setPayloadSentToComponentTime(OffsetDateTime.now());

        var request = new HttpEntity<>(inputPayload, headers);
        ResponseEntity<Object> response = null;
        try {
            response = this
                .restTemplate
                .postForEntity(nodeContext.getEndpointUri(), request, Object.class);

            flowEventEntity.setHttpResponseCode(response.getStatusCode().value());
        } catch (Exception e) {
            logger.error("ERROR: {}", e.getMessage());

            if (e instanceof RestClientResponseException cast) {
                var responseHeaders = cast.getResponseHeaders();
                response = ResponseEntity
                    .status(cast.getStatusCode())
                    .headers(responseHeaders)
                    .body(cast.getResponseBodyAsString());

                flowEventEntity.setHttpResponseCode(cast.getStatusCode().value());
            } else {
                response = new ResponseEntity<>(e.getMessage(), null, null);
            }
        }
        flowEventTimes.setComponentResponseTime(OffsetDateTime.now());
        this.nodeComponentResponseCommand.processComponentResponse(flowEventEntity, node, response);
    }

    @Transactional
    public void tryPostInputToComponentAsynchronously(
        FlowEventEntity flowEventEntity,
        final AbstractNode node,
        final Object inputPayload) {

        var nodeContext = node.getNodeContext();
        logger.info("POSTing payload asynchronously to URI: {}...",
            nodeContext.getEndpointUri());

        this.nodeOpenMessageVisitor.incrementOpenMessagesFor(node);
        var eventTimes = flowEventEntity.getFlowEventTimes();
        this.webClient
            .post()
            .uri(nodeContext.getEndpointUri())
            .bodyValue(inputPayload)
            .retrieve()
            .toEntity(Object.class)
            .doOnNext(response -> {
                this.nodeOpenMessageVisitor.decrementOpenMessagesFor(node);
                eventTimes.setComponentResponseTime(OffsetDateTime.now());
                flowEventEntity.setHttpResponseCode(response.getStatusCode().value());
                logger.info("...got response code {} from component for flow with id {}...",
                    response.getStatusCode(),
                    flowEventEntity.getFlow().getId());
                try {
                    this
                        .nodeComponentResponseCommand
                        .processComponentResponse(flowEventEntity, node, response);
                } catch (Exception e) {
                    logger.error("ERROR processing component response: {}", e.getMessage());
                }
            })
            .doOnError(error -> {
                if (error instanceof HttpStatusCodeException errorCast) {
                    flowEventEntity.setHttpResponseCode(errorCast.getStatusCode().value());
                }
                logger.error("ERROR response when POSTing to component: {} ",
                    error.getMessage());
                eventTimes.setComponentResponseTime(OffsetDateTime.now());
                eventTimes.setEventCompleteTime(OffsetDateTime.now());
                this.nodeOpenMessageVisitor.decrementOpenMessagesFor(node);
                this.flowEventRepository.save(flowEventEntity);
            })
            .subscribe();
    }
}
