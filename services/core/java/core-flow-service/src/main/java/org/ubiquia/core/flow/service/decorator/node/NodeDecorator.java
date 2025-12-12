package org.ubiquia.core.flow.service.decorator.node;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.jimblackler.jsonschemafriend.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ubiquia.common.library.implementation.service.builder.NodeEndpointRecordBuilder;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.component.node.PollNode;
import org.ubiquia.core.flow.component.node.QueueNode;
import org.ubiquia.core.flow.component.node.SubscribeNode;
import org.ubiquia.core.flow.service.builder.StimulatedPayloadBuilder;
import org.ubiquia.core.flow.service.decorator.node.broker.NodeBrokerDecorator;
import org.ubiquia.core.flow.service.visitor.validator.PayloadModelValidator;


/**
 * This is a Decorator service dedicated to "decorating" adapters. Specifically, this service
 * can be used to dynamically append RESTful endpoints and polling functionality to adapters
 * depending on their needs. For example, only some adapters will need to define "back-pressure"
 * endpoints; this service provides a clean API to do so.
 *
 * @see <a href="https://sourcemaking.com/design_patterns/decorator">Decorator Design Pattern</a>
 */
@Service
public class NodeDecorator {

    private static final Logger logger = LoggerFactory.getLogger(NodeDecorator.class);
    @Autowired
    private NodeBrokerDecorator nodeBrokerDecorator;
    @Autowired
    private NodeEndpointRecordBuilder nodeEndpointRecordBuilder;
    @Autowired
    private PayloadModelValidator payloadModelValidator;
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired
    private StimulatedPayloadBuilder stimulatedPayloadBuilder;

    /**
     * Initialize the provided adapter so that it begins polling in order to calculate
     * back pressure.
     *
     * @param node The adapter to calculate back pressure for.
     */
    public void initializeBackPressurePollingFor(AbstractNode node) {
        var nodeContext = node.getNodeContext();

        logger.info("...Initializing polling of back pressure for adapter {} of graph {}...",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            node::pollToSampleBackPressure,
            nodeContext.getNodeSettings().getBackpressurePollFrequencyMilliseconds(),
            nodeContext.getNodeSettings().getBackpressurePollFrequencyMilliseconds(),
            TimeUnit.MILLISECONDS);
        nodeContext.getTasks().add(task);
        logger.info("...completed back pressure polling initialization...");
    }

    public void initializeOutputLogicFor(AbstractNode node)
        throws GenerationException {

        var nodeContext = node.getNodeContext();
        logger.info("...Initializing output logic for node {} of graph {}...",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        this.payloadModelValidator.tryInitializeOutputSchema(nodeContext);
        logger.info("...completed egress logic initialization...");
    }

    /**
     * Initialize the adapter so that it begins polling for incoming messages.
     *
     * @param node The adapter to initialize.
     */
    public void initializeInboxPollingFor(AbstractNode node)
        throws GenerationException, JsonProcessingException {
        var nodeContext = node.getNodeContext();
        logger.info("...Initializing polling of inbox for node {} of graph {}...",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        this.payloadModelValidator.tryInitializeInputPayloadSchema(nodeContext);
        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            node::tryPollInbox,
            nodeContext.getNodeSettings().getInboxPollFrequencyMilliseconds(),
            nodeContext.getNodeSettings().getInboxPollFrequencyMilliseconds(),
            TimeUnit.MILLISECONDS);
        nodeContext.getTasks().add(task);
        logger.info("...completed inbox polling initialization...");
    }

    /**
     * Initialize a poll adapter so that it begins polling its target endpoint.
     *
     * @param node The adapter to initialize.
     */
    public void initializePollingFor(PollNode node)
        throws GenerationException,
        JsonProcessingException {
        var nodeContext = node.getNodeContext();
        logger.info("...Initializing polling for node {} of graph {}... ",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        this.payloadModelValidator.tryInitializeInputPayloadSchema(nodeContext);

        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            node::tryPollEndpoint,
            nodeContext.getPollSettings().getPollFrequencyInMilliseconds(),
            nodeContext.getPollSettings().getPollFrequencyInMilliseconds(),
            TimeUnit.MILLISECONDS);
        nodeContext.getTasks().add(task);
        logger.info("...completed polling initialization...");
    }

    public void tryInitializeInputStimulationFor(AbstractNode node)
        throws GenerationException {

        var nodeContext = node.getNodeContext();
        var settings = node.getNodeContext().getNodeSettings();

        if (settings.getStimulateInputPayload()) {
            logger.info("...Initializing periodic input stimulation for node {} of graph {}...",
                nodeContext.getNodeName(),
                nodeContext.getGraph().getName());

            this.stimulatedPayloadBuilder.initializeSchema(nodeContext.getNodeId());
            var executor = new ScheduledThreadPoolExecutor(1);
            var task = executor.scheduleAtFixedRate(
                node::stimulateComponent,
                nodeContext.getNodeSettings().getStimulateFrequencyMilliseconds(),
                nodeContext.getNodeSettings().getStimulateFrequencyMilliseconds(),
                TimeUnit.MILLISECONDS);
            nodeContext.getTasks().add(task);
            logger.info("...completed back pressure polling initialization...");
        } else {
            logger.debug("...node not configured for input stimulation; "
                + "not initializing stimulation.");
        }
    }

    /**
     * Initialize an adapter to have a subscription to a broker.
     *
     * @param node The adapter to initialize.
     */
    public void initializeSubscriptionFor(SubscribeNode node) {
        var nodeContext = node.getNodeContext();
        logger.info("...Initializing broker subscription for adapter {} of graph {}...",
            nodeContext.getNodeName(),
            nodeContext.getGraph().getName());

        this.nodeBrokerDecorator.initializeBrokerSubscriptionFor(node);
        logger.info("...completed broker subscription initialization...");
    }

    /**
     * Create an endpoint for an adapter so that clients can query for its back pressure.
     *
     * @param node The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerBackpressureEndpointFor(AbstractNode node) throws RuntimeException {

        var nodeContext = node.getNodeContext();

        var endpointRecord = this
            .nodeEndpointRecordBuilder
            .getBackpressureEndpointFor(
                nodeContext.getGraph().getName(),
                nodeContext.getNodeName());
        logger.info("...building endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(node, "tryGetBackPressure");
        nodeContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, node, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can query its peek endpoint.
     *
     * @param node The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPeekEndpointFor(QueueNode node) throws RuntimeException {

        var nodeContext = node.getNodeContext();

        var endpointRecord = this
            .nodeEndpointRecordBuilder
            .getPeekEndpointFor(
                nodeContext.getGraph().getName(),
                nodeContext.getNodeName());

        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(node, "peek");
        nodeContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, node, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can query its peek endpoint.
     *
     * @param node The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPopEndpointFor(QueueNode node) throws RuntimeException {

        var nodeContext = node.getNodeContext();

        var endpointRecord = this
            .nodeEndpointRecordBuilder
            .getPopEndpointFor(
                nodeContext.getGraph().getName(),
                nodeContext.getNodeName());
        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(node, "pop");
        nodeContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, node, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can push data to it.
     *
     * @param node The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPushEndpointFor(AbstractNode node) throws RuntimeException {

        var nodeContext = node.getNodeContext();

        var endpointRecord = this
            .nodeEndpointRecordBuilder
            .getPushEndpointFor(
                nodeContext.getGraph().getName(),
                nodeContext.getNodeName());

        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(node, "push");
        nodeContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, node, method);
    }

    /**
     * A simple helper method to ensure that we can properly return methods for adapters.
     *
     * @param node       The adapter to get a method for.
     * @param methodName The method name to retrieve.
     * @return The method.
     * @throws RuntimeException Exception from being unable to find the method.
     */
    private Method getMethodFrom(final AbstractNode node, final String methodName)
        throws RuntimeException {
        var methods = Arrays.stream(node.getClass().getMethods()).toList();
        var match = methods.stream().filter(x -> x.getName().equals(methodName)).findFirst();

        if (match.isEmpty()) {
            throw new RuntimeException("ERROR: Unable to initialize - could not find a '"
                + methodName
                + "' method!");
        }
        return match.get();
    }
}

