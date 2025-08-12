package org.ubiquia.core.flow.component.adapter;


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
public class PollAdapter extends AbstractAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PollAdapter.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException {
        super.initializeBehavior();
        super.adapterDecorator.initializePollingFor(this);
        super.adapterDecorator.initializeOutputLogicFor(this);
        super.adapterDecorator.registerPushEndpointFor(this);
        super.adapterDecorator.tryInitializeInputStimulationFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    public void tryPollEndpoint() {

        var adapterContext = this.getAdapterContext();

        this.getLogger().info("Adapter {} of graph {} is polling...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        Timer.Sample sample = null;
        if (Objects.nonNull(super.microMeterHelper)) {
            sample = super.microMeterHelper.startSample();
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(headers);

        var flowEvent = super.flowEventBuilder.makeEventFrom(this);

        ResponseEntity<Object> response = null;
        try {
            this.getLogger().info("...polling url: {}...",
                adapterContext.getPollSettings().getPollEndpoint());
            response = super.restTemplate.exchange(
                adapterContext.getPollSettings().getPollEndpoint(),
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
            var stringifiedPayload = super.objectMapper.writeValueAsString(response.getBody());
            super.payloadModelValidator.tryValidateInputPayloadFor(stringifiedPayload, this);
            super.stamper.tryStampInputs(flowEvent, stringifiedPayload);

            if (adapterContext.getAdapterSettings().getPersistInputPayload()) {
                flowEvent.setInputPayload(stringifiedPayload);
            }

            super.adapterPayloadOrchestrator.forwardPayload(
                flowEvent,
                this,
                stringifiedPayload);
        } catch (Exception e) {
            logger.error("Could not successfully forward polled response to agent: {}",
                e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            super.microMeterHelper.endSample(sample, "tryPollEndpoint", adapterContext.getTags());
        }
    }

}