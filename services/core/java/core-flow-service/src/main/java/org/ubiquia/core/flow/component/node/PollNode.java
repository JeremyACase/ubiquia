package org.ubiquia.core.flow.component.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component
@Scope("prototype")
public class PollNode extends AbstractNode {

    private static final Logger logger = LoggerFactory.getLogger(PollNode.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.nodeDecorator.initializePollingFor(this);
        super.nodeDecorator.initializeOutputLogicFor(this);
        super.nodeDecorator.registerPushEndpointFor(this);
        super.nodeDecorator.tryInitializeInputStimulationFor(this);
        this.getLogger().info("...{} node initialization complete...",
            this.getNodeContext().getNodeType());
    }

    public void tryPollEndpoint() {

        var nodeContext = this.getNodeContext();

        this.getLogger().info("Adapter {} of graph {} is polling...",
            nodeContext.getNodeName(),
            nodeContext.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(super.microMeterHelper)) {
            sample = super.microMeterHelper.startSample();
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(headers);

        var flowEvent = super.flowEventBuilder.makeFlowAndEventFrom(this);

        ResponseEntity<Object> response = null;
        try {
            this.getLogger().info("...polling url: {}...",
                nodeContext.getPollSettings().getPollEndpoint());
            response = super.restTemplate.exchange(
                nodeContext.getPollSettings().getPollEndpoint(),
                HttpMethod.GET,
                request,
                Object.class);
        } catch (HttpStatusCodeException e) {
            response = new ResponseEntity<>(
                e.getResponseBodyAsString(),
                e.getResponseHeaders(),
                e.getStatusCode());
        }
        logger.info("...got response from poll endpoint: {}.",
            response.getStatusCode());

        try {
            var stringifiedPayload = super
                .objectMapper
                .writeValueAsString(response.getBody());
            super.payloadModelValidator.tryValidateInputPayloadFor(stringifiedPayload, this);
            super.stamper.tryStampInputs(flowEvent, stringifiedPayload);

            if (nodeContext.getNodeSettings().getPersistInputPayload()) {
                flowEvent.setInputPayload(stringifiedPayload);
            }

            super.nodePayloadOrchestrator.forwardPayload(
                flowEvent,
                this,
                stringifiedPayload);
        } catch (Exception e) {
            logger.error("Could not successfully forward polled response to agent: {}",
                e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            super.microMeterHelper.endSample(sample, "tryPollEndpoint", nodeContext.getTags());
        }
    }

}