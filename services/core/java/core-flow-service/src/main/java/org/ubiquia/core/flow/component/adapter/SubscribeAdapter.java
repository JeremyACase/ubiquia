package org.ubiquia.core.flow.component.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SubscribeAdapter
    extends AbstractAdapter
    implements MessageListener<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeAdapter.class);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        super.initializeBehavior();
        super.adapterDecorator.initializeSubscriptionFor(this);
        super.adapterDecorator.initializeOutputLogicFor(this);
        super.adapterDecorator.registerPushEndpointFor(this);
        super.adapterDecorator.tryInitializeInputStimulationFor(this);
        this.getLogger().info("...{} adapter initialization complete...",
            this.getAdapterContext().getAdapterType());
    }

    @Override
    public void onMessage(@NotNull ConsumerRecord<String, String> message) {
        this.getLogger().info("Received a message over broker; processing...");

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        try {
            var inputPayload = message.value();
            super.payloadModelValidator.tryValidateInputPayloadFor(inputPayload, this);
            var event = super.flowEventBuilder.makeEventFrom(inputPayload, this);
            super.adapterPayloadOrchestrator.forwardPayload(event, this, inputPayload);
        } catch (Exception e) {
            logger.error("Could not process incoming message: {} ", message);
            logger.error("Reason: {} ", e.getMessage());
        }

        if (Objects.nonNull(sample)) {
            super.microMeterHelper.endSample(
                sample,
                "onMessage",
                this.getAdapterContext().getTags());
        }
        this.getLogger().info("...finished processing incoming message.");
    }
}