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
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.service.command.adapter.AdapterPostToComponentCommand;
import org.ubiquia.core.flow.service.command.adapter.AdapterPutToComponentCommand;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.proxy.TemplateComponentProxy;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class AdapterPayloadOrchestrator implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterPayloadOrchestrator.class);

    @Autowired
    private AdapterPostToComponentCommand adapterPostToComponentCommand;

    @Autowired
    private AdapterPutToComponentCommand adapterPutToComponentCommand;

    @Autowired
    private Outbox outbox;

    @Autowired
    private TemplateComponentProxy templateComponentProxy;

    public Logger getLogger() {
        return logger;
    }

    public void forwardPayload(
        FlowEventEntity flowEventEntity,
        final AbstractAdapter adapter,
        final String payload)
        throws JsonProcessingException,
        JsonGeneratorException,
        GenerationException,
        ValidationException {

        var adapterContext = adapter.getAdapterContext();
        if (adapterContext.getAdapterSettings().getIsPassthrough()) {
            this.outbox.tryQueueComponentResponse(
                flowEventEntity,
                payload);
        } else if (adapterContext.getTemplateComponent()) {
            this.templateComponentProxy.proxyAsComponentFor(flowEventEntity);
        } else {
            this.trySendInputPayloadToComponent(
                flowEventEntity,
                adapter,
                payload);
        }
    }

    private void tryPostInputPayloadToComponent(
        FlowEventEntity flowEventEntity,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws
        ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.adapterPostToComponentCommand.tryPostInputToComponentAsynchronously(
                    flowEventEntity,
                    adapter,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.adapterPostToComponentCommand.tryPostPayloadToComponentSynchronously(
                    flowEventEntity,
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

    private void tryPutInputPayloadToComponent(
        FlowEventEntity flowEventEntity,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.adapterPutToComponentCommand.tryPutInputToComponentAsynchronously(
                    flowEventEntity,
                    adapter,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.adapterPutToComponentCommand.tryPutPayloadToComponentSynchronously(
                    flowEventEntity,
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

    private void trySendInputPayloadToComponent(
        FlowEventEntity flowEventEntity,
        final AbstractAdapter adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getAdapterContext().getEgressSettings();
        switch (egressSettings.getHttpOutputType()) {

            case PUT: {
                this.tryPutInputPayloadToComponent(flowEventEntity, adapter, inputPayload);
            }
            break;

            case POST: {
                this.tryPostInputPayloadToComponent(flowEventEntity, adapter, inputPayload);
            }
            break;

            default: {
                throw new NotImplementedException("ERROR: Unrecognized HTTP output type: "
                    + egressSettings.getHttpOutputType());
            }
        }
    }
}
