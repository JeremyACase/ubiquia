package org.ubiquia.core.flow.service.decorator.adapter;


import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.PollAdapter;
import org.ubiquia.core.flow.component.adapter.QueueAdapter;
import org.ubiquia.core.flow.component.adapter.SubscribeAdapter;
import org.ubiquia.core.flow.service.decorator.adapter.broker.AdapterBrokerDecorator;


/**
 * This is a Decorator service dedicated to "decorating" adapters. Specifically, this service
 * can be used to dynamically append RESTful endpoints and polling functionality to adapters
 * depending on their needs. For example, only some adapters will need to define "back-pressure"
 * endpoints; this service provides a clean API to do so.
 *
 * @see <a href="https://sourcemaking.com/design_patterns/decorator">Decorator Design Pattern</a>
 */
@Service
public class AdapterDecorator {

    private static final Logger logger = LoggerFactory.getLogger(AdapterDecorator.class);
    @Autowired
    private AdapterBrokerDecorator adapterBrokerDecorator;
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Initialize the provided adapter so that it begins polling in order to calculate
     * back pressure.
     *
     * @param adapter The adapter to calculate back pressure for.
     */
    public void initializeBackPressurePollingFor(AbstractAdapter adapter) {
        var adapterContext = adapter.getAdapterContext();

        logger.info("...Initializing polling of back pressure for adapter {} of graph {}...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            adapter::pollToSampleBackPressure,
            adapterContext.getAdapterSettings().getBackpressurePollFrequencyMilliseconds(),
            adapterContext.getAdapterSettings().getBackpressurePollFrequencyMilliseconds(),
            TimeUnit.MILLISECONDS);
        adapterContext.getTasks().add(task);
        logger.info("...completed back pressure polling initialization...");
    }

    /**
     * Initialize the adapter so that it begins polling for incoming messages.
     *
     * @param adapter The adapter to initialize.
     */
    public void initializeInboxPollingFor(AbstractAdapter adapter) {
        var adapterContext = adapter.getAdapterContext();
        logger.info("...Initializing polling of inbox for adapter {} of graph {}...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            adapter::tryPollInbox,
            adapterContext.getAdapterSettings().getInboxPollFrequencyMilliseconds(),
            adapterContext.getAdapterSettings().getInboxPollFrequencyMilliseconds(),
            TimeUnit.MILLISECONDS);
        adapterContext.getTasks().add(task);
        logger.info("...completed inbox polling initialization...");
    }

    /**
     * Initialize a poll adapter so that it begins polling its target endpoint.
     *
     * @param adapter The adapter to initialize.
     */
    public void initializePollingFor(PollAdapter adapter) {
        var adapterContext = adapter.getAdapterContext();
        logger.info("...Initializing polling for adapter {} of graph {}... ",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        var executor = new ScheduledThreadPoolExecutor(1);
        var task = executor.scheduleAtFixedRate(
            adapter::tryPollEndpoint,
            adapterContext.getPollSettings().getPollFrequencyInMilliseconds(),
            adapterContext.getPollSettings().getPollFrequencyInMilliseconds(),
            TimeUnit.MILLISECONDS);
        adapterContext.getTasks().add(task);
        logger.info("...completed polling initialization...");
    }

    /**
     * Initialize an adapter to have a subscription to a broker.
     *
     * @param adapter The adapter to initialize.
     */
    public void initializeSubscriptionFor(SubscribeAdapter adapter) {
        var adapterContext = adapter.getAdapterContext();
        logger.info("...Initializing broker subscription for adapter {} of graph {}...",
            adapterContext.getAdapterName(),
            adapterContext.getGraphName());

        this.adapterBrokerDecorator.initializeBrokerSubscriptionFor(adapter);
        logger.info("...completed broker subscription initialization...");
    }

    /**
     * Create an endpoint for an adapter so that clients can query for its back pressure.
     *
     * @param adapter The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerBackpressureEndpointFor(AbstractAdapter adapter) throws RuntimeException {

        var adapterContext = adapter.getAdapterContext();

        // Build an endpoint using the agent's name as part of the path.
        var path = this.getBackpressurePathFor(adapter);
        logger.info("...building endpoint: {}", path);
        var mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(adapter, "tryGetBackPressure");
        adapterContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, adapter, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can query its peek endpoint.
     *
     * @param adapter The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPeekEndpointFor(QueueAdapter adapter) throws RuntimeException {

        var adapterContext = adapter.getAdapterContext();

        var path = this.getPeekPathFor(adapter);
        logger.info("...generating endpoint: {}", path);
        var mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(adapter, "peek");
        adapterContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, adapter, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can query its peek endpoint.
     *
     * @param adapter The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPopEndpointFor(QueueAdapter adapter) throws RuntimeException {

        var adapterContext = adapter.getAdapterContext();

        var path = this.getPopPathFor(adapter);
        logger.info("...generating endpoint: {}", path);
        var mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(adapter, "pop");
        adapterContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, adapter, method);
    }

    /**
     * Create an endpoint for an adapter so that clients can push data to it.
     *
     * @param adapter The adapter to create an endpoint for.
     * @throws RuntimeException Exceptions from creating the endpoints.
     */
    public void registerPushEndpointFor(AbstractAdapter adapter) throws RuntimeException {

        var adapterContext = adapter.getAdapterContext();

        var path = this.getPushPathFor(adapter);
        logger.info("...generating endpoint: {}", path);
        var mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build();
        var method = this.getMethodFrom(adapter, "push");
        adapterContext.getRegisteredMappingInfos().add(mappingInfo);
        this.requestMappingHandlerMapping.registerMapping(mappingInfo, adapter, method);
    }

    /**
     * A simple helper method to ensure that we can properly return methods for adapters.
     *
     * @param adapter    The adapter to get a method for.
     * @param methodName The method name to retrieve.
     * @return The method.
     * @throws RuntimeException Exception from being unable to find the method.
     */
    private Method getMethodFrom(final AbstractAdapter adapter, final String methodName)
        throws RuntimeException {
        var methods = Arrays.stream(adapter.getClass().getMethods()).toList();
        var match = methods.stream().filter(x -> x.getName().equals(methodName)).findFirst();

        if (match.isEmpty()) {
            throw new RuntimeException("ERROR: Unable to initialize - could not find a '"
                + methodName
                + "' method!");
        }
        return match.get();
    }

    /**
     * Helper method to build back pressure paths.
     *
     * @param adapter The adapter to build an path for.
     * @return The path.
     */
    private String getBackpressurePathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/back-pressure";
        return path;
    }

    /**
     * Helper method to build back pressure paths.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getPushPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/push";
        return path;
    }

    /**
     * Helper method to build back pressure paths.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getPushListPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/push/list";
        return path;
    }

    /**
     * Helper method to build peek paths.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getPeekPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/queue/peek";
        return path;
    }

    /**
     * Helper method to build pop paths.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getPopPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/queue/pop";
        return path;
    }

    /**
     * Helper method to build paths.
     *
     * @param adapter The adapter to build n path for.
     * @return The path.
     */
    private String getPathHelper(final AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();

        var path = "ubiquia/graph/"
            + adapterContext.getGraphName().toLowerCase()
            + "/adapter/";

        path += adapterContext.getAdapterName().toLowerCase();

        return path;
    }

    /**
     * Helper method to build an upload path.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getUploadPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/upload/file";
        return path;
    }

    /**
     * Helper method to build a multiple file upload path.
     *
     * @param adapter The adapter to build a path for.
     * @return The path.
     */
    private String getUploadsPathFor(final AbstractAdapter adapter) {
        var path = this.getPathHelper(adapter) + "/upload/files";
        return path;
    }
}
