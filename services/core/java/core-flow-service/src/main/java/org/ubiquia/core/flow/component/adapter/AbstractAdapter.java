package org.ubiquia.core.flow.component.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Objects;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.library.implementation.service.mapper.FlowEventDtoMapper;
import org.ubiquia.common.model.ubiquia.adapter.backpressure.BackPressure;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.dto.FlowMessage;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.builder.FlowEventBuilder;
import org.ubiquia.core.flow.service.builder.StimulatedPayloadBuilder;
import org.ubiquia.core.flow.service.calculator.BackPressureCalculator;
import org.ubiquia.core.flow.service.command.adapter.AdapterInboxMessageCommand;
import org.ubiquia.core.flow.service.command.adapter.AdapterInboxPollCommand;
import org.ubiquia.core.flow.service.decorator.adapter.AdapterDecorator;
import org.ubiquia.core.flow.service.logic.adapter.AdapterInboxPollingLogic;
import org.ubiquia.core.flow.service.orchestrator.AdapterPayloadOrchestrator;
import org.ubiquia.core.flow.service.telemetry.MicroMeterHelper;
import org.ubiquia.core.flow.service.visitor.StamperVisitor;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;

/**
 * An abstract class that can be used to adapt to Components in Ubiquia. There are different
 * types of adapters; each inherits from this abstract base class.
 * Adapters are built at runtime using data from the database. They can be torn down or deployed
 * dynamically when a graph is similarly torn down or deployed.
 * All adapters do very-different things (i.e., poll versus POST.) As such, some adapters
 * will constantly monitor their "backpressure", while others will not.
 */
public abstract class AbstractAdapter implements InterfaceLogger {

    @Autowired
    protected AdapterDecorator adapterDecorator;
    @Autowired
    protected AdapterPayloadOrchestrator adapterPayloadOrchestrator;
    @Autowired
    protected AdapterInboxMessageCommand adapterInboxMessageCommand;
    @Autowired
    protected AdapterInboxPollCommand adapterInboxPollCommand;
    @Autowired
    protected AdapterInboxPollingLogic adapterInboxPollingLogic;
    @Autowired
    protected BackPressureCalculator backPressureCalculator;
    @Autowired
    protected FlowEventBuilder flowEventBuilder;
    @Autowired
    protected FlowEventDtoMapper flowEventDtoMapper;
    @Autowired
    protected FlowMessageRepository flowMessageRepository;
    @Autowired(required = false)
    protected MicroMeterHelper microMeterHelper;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PayloadModelValidator payloadModelValidator;
    @Autowired
    protected RestTemplate restTemplate;
    @Autowired
    protected StamperVisitor stamper;
    @Autowired
    private StimulatedPayloadBuilder stimulatedPayloadBuilder;
    private AdapterContext adapterContext;

    public AdapterContext getAdapterContext() {
        return adapterContext;
    }

    public void setAdapterContext(AdapterContext adapterContext) {
        this.adapterContext = adapterContext;
    }

    public void initializeBehavior() throws GenerationException, JsonProcessingException {
        var adapterContext = this.getAdapterContext();
        this.getLogger().info("...Initializing {} adapter named {} for graph {}...",
            adapterContext.getAdapterType(),
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());
    }

    public void pollToSampleBackPressure() {

        var adapterContext = this.getAdapterContext();

        this.getLogger().debug("Adapter {} of graph {} is polling "
                + "to calculate backpressure...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        var count = this
            .flowMessageRepository
            .countByTargetAdapterId(adapterContext.getAdapterId());

        // We're only using 2 data points for now; we can eventually use more data points
        // if need be for whatever fancy telemetry we might eventually want.
        if (adapterContext.getBackPressureSamplings().size() > 1) {
            adapterContext
                .getBackPressureSamplings()
                .set(1, adapterContext.getBackPressureSamplings().get(0));
            adapterContext.getBackPressureSamplings().set(0, count);
        } else {
            adapterContext.getBackPressureSamplings().add(count);
        }
    }

    public ResponseEntity<FlowEvent> push(@RequestBody final String inputPayload)
        throws Exception {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        this.getLogger().info("Received a payload to push to component...");
        this.payloadModelValidator.tryValidateInputPayloadFor(inputPayload, this);
        var event = this.flowEventBuilder.makeEventFrom(inputPayload, this);

        this.adapterPayloadOrchestrator.forwardPayload(event, this, inputPayload);
        var egress = this.flowEventDtoMapper.map(event);
        this.getLogger().info("...finished processing input payload.");
        var response = ResponseEntity.accepted().body(egress);

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(sample, "push", this.adapterContext.getTags());
        }

        return response;
    }

    public void stimulateComponent() {

        Timer.Sample sample = null;
        if (Objects.nonNull(this.microMeterHelper)) {
            sample = this.microMeterHelper.startSample();
        }

        this.getLogger().info("Stimulating component with dummy input payload...");

        try {
            var stimulatePayload = this.stimulatedPayloadBuilder.buildStimulatedPayloadFor(this);
            var event = this.flowEventBuilder.makeEventFrom(stimulatePayload, this);
            this.adapterPayloadOrchestrator.forwardPayload(event, this, stimulatePayload);
        } catch (Exception e) {
            this.getLogger().error("ERROR: Error while stimulating component: ", e);
        }

        if (Objects.nonNull(sample)) {
            this.microMeterHelper.endSample(
                sample,
                "stimulateAgent",
                this.adapterContext.getTags());
        }
    }

    public void tryPollInbox() {

        if (this.adapterInboxPollingLogic.isValidToPollInbox(this)) {
            var messages = this.adapterInboxPollCommand.tryPollInboxFor(this);
            this.tryProcessInboxMessages(messages);
        }
    }

    public ResponseEntity<BackPressure> tryGetBackPressure() {
        this.getLogger().debug("Received a request to get current back pressure...");
        var backPressure = this.backPressureCalculator.calculateBackPressureFor(this);
        var response = ResponseEntity.accepted().body(backPressure);
        return response;
    }

    protected void tryProcessInboxMessages(final List<FlowMessage> messages) {
        for (var message : messages) {
            this.adapterInboxMessageCommand.tryProcessInboxMessageFor(message, this);
        }
    }
}
