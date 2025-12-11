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
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.service.command.node.NodePostToComponentCommand;
import org.ubiquia.core.flow.service.command.node.NodePutToComponentCommand;
import org.ubiquia.core.flow.service.io.Outbox;
import org.ubiquia.core.flow.service.logic.node.NodePassthroughLogic;
import org.ubiquia.core.flow.service.logic.node.NodeSimulatedOutputLogic;
import org.ubiquia.core.flow.service.proxy.TemplateComponentProxy;

/**
 * A service that exposes various methods common to all adapters.
 */
@Service
public class NodePayloadOrchestrator implements InterfaceLogger {

    private static final Logger logger = LoggerFactory.getLogger(NodePayloadOrchestrator.class);

    @Autowired
    private NodePostToComponentCommand nodePostToComponentCommand;

    @Autowired
    private NodePutToComponentCommand nodePutToComponentCommand;

    @Autowired
    private NodePassthroughLogic nodePassthroughLogic;
    @Autowired
    private NodeSimulatedOutputLogic nodeSimulatedOutputLogic;
    @Autowired
    private Outbox outbox;

    @Autowired
    private TemplateComponentProxy templateComponentProxy;

    public Logger getLogger() {
        return logger;
    }

    public void forwardPayload(
        FlowEventEntity flowEventEntity,
        final AbstractNode node,
        final String payload)
        throws JsonProcessingException,
        JsonGeneratorException,
        GenerationException,
        ValidationException {

        var nodeContext = node.getNodeContext();
        if (this.nodePassthroughLogic.isPassthrough(node)) {
            this.outbox.tryQueueMessage(
                flowEventEntity,
                payload);
        } else if (this.nodeSimulatedOutputLogic.isSimulatedResponsePayload(node)) {
            this.templateComponentProxy.proxyAsComponentWith(flowEventEntity);
        } else {
            this.trySendInputPayloadToComponent(
                flowEventEntity,
                node,
                payload);
        }
    }

    private void tryPostInputPayloadToComponent(
        FlowEventEntity flowEventEntity,
        final AbstractNode node,
        final Object inputPayload)
        throws JsonProcessingException {

        var egressSettings = node.getNodeContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.nodePostToComponentCommand.tryPostInputToComponentAsynchronously(
                    flowEventEntity,
                    node,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.nodePostToComponentCommand.tryPostPayloadToComponentSynchronously(
                    flowEventEntity,
                    node,
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
        final AbstractNode node,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = node.getNodeContext().getEgressSettings();

        switch (egressSettings.getEgressType()) {

            case ASYNCHRONOUS: {
                this.nodePutToComponentCommand.tryPutInputToComponentAsynchronously(
                    flowEventEntity,
                    node,
                    inputPayload);
            }
            break;

            case SYNCHRONOUS: {
                this.nodePutToComponentCommand.tryPutPayloadToComponentSynchronously(
                    flowEventEntity,
                    node,
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
        final AbstractNode adapter,
        final Object inputPayload)
        throws ValidationException,
        GenerationException,
        JsonProcessingException {

        var egressSettings = adapter.getNodeContext().getEgressSettings();
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
