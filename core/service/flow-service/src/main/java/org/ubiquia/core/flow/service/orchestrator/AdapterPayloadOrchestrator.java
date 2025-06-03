package org.ubiquia.core.flow.service.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.jimblackler.jsongenerator.JsonGeneratorException;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.model.ubiquia.entity.FlowEvent;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.service.command.adapter.AdapterPostToAgentCommand;
import org.ubiquia.core.flow.service.command.adapter.AdapterPutToAgentCommand;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.proxy.TemplateAgentProxy;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterPayloadOrchestrator implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterPayloadOrchestrator.class);

    @Autowired
    private AdapterPostToAgentCommand adapterPostToAgentCommand;

    @Autowired
    private AdapterPutToAgentCommand adapterPutToAgentCommand;

    @Autowired
    private Outbox outbox;

    @Autowired
    private TemplateAgentProxy templateAgentProxy;

    public Logger getLogger() {
        return logger;
    }

    public void forwardPayload(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final String payload)
        throws JsonProcessingException,
        JsonGeneratorException,
        GenerationException,
        ValidationException {

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

    private void tryPostInputPayloadToAgent(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws
        ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.adapterPostToAgentCommand.tryPostInputToAgentAsynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.adapterPostToAgentCommand.tryPostPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized egress type: "
                    + egressSettings.getEgressType());
            }
        }
    }

    private void tryPutInputPayloadToAgent(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.adapterPutToAgentCommand.tryPutInputToAgentAsynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.adapterPutToAgentCommand.tryPutPayloadToAgentSynchronously(
                    flowEvent,
                    adapter,
                    inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized egress type: "
                    + egressSettings.getEgressType());
            }
        }
    }

    private void trySendInputPayloadToAgent(
        FlowEvent flowEvent,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();
        switch (egressSettings.getHttpOutputType()) {

            case PUT: {
                this.tryPutInputPayloadToAgent(flowEvent, adapter, inputPayload);
            }
            break;

            case POST: {
                this.tryPostInputPayloadToAgent(flowEvent, adapter, inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized HTTP output type: "
                    + egressSettings.getHttpOutputType());
            }
        }
    }
}
