package org.ubiquia.core.flow.service.decorator.adapter;


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
import org.ubiquia.common.library.logic.service.builder.AdapterEndpointRecordBuilder;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.component.adapter.PollAdapter;
import org.ubiquia.core.flow.component.adapter.QueueAdapter;
import org.ubiquia.core.flow.component.adapter.SubscribeAdapter;
import org.ubiquia.core.flow.service.builder.StimulatedPayloadBuilder;
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
    private AdapterEndpointRecordBuilder adapterEndpointRecordBuilder;
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired
    private StimulatedPayloadBuilder stimulatedPayloadBuilder;

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

    public void tryInitializeInputStimulationFor(AbstractAdapter adapter) {

        var adapterContext = adapter.getAdapterContext();
        var settings = adapter.getAdapterContext().getAdapterSettings();

        if (settings.getStimulateInputPayload()) {
            logger.info("...Initializing periodic input stimulation for adapter {} of graph {}...",
                adapterContext.getAdapterName(),
                adapterContext.getGraphName());

            try {
                this.stimulatedPayloadBuilder.initializeSchema(adapterContext.getAdapterId());
            } catch (GenerationException e) {
                logger.error("ERROR: Could not initialize schema for adapter {}",
                    adapterContext.getAdapterName());
            }

            var executor = new ScheduledThreadPoolExecutor(1);
            var task = executor.scheduleAtFixedRate(
                adapter::stimulateAgent,
                adapterContext.getAdapterSettings().getStimulateFrequencyMilliseconds(),
                adapterContext.getAdapterSettings().getStimulateFrequencyMilliseconds(),
                TimeUnit.MILLISECONDS);
            adapterContext.getTasks().add(task);
            logger.info("...completed back pressure polling initialization...");
        } else {
            logger.debug("...Adapter not configured for input stimulation; "
                + "not initializing stimulation.");
        }
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

        var endpointRecord = this.adapterEndpointRecordBuilder.getBackpressureEndpointFor(
            adapterContext.getGraphName(),
            adapterContext.getAdapterName());
        logger.info("...building endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
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

        var endpointRecord = this.adapterEndpointRecordBuilder.getPeekEndpointFor(
            adapterContext.getGraphName(),
            adapterContext.getAdapterName());
        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
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
    public void registerPopEndpointFor(final QueueAdapter adapter) throws RuntimeException {

        var adapterContext = adapter.getAdapterContext();

        var endpointRecord = this.adapterEndpointRecordBuilder.getPopEndpointFor(
            adapterContext.getGraphName(),
            adapterContext.getAdapterName());
        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
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

        var endpointRecord = this.adapterEndpointRecordBuilder.getPushEndpointFor(
            adapterContext.getGraphName(),
            adapterContext.getAdapterName());
        logger.info("...generating endpoint: {}", endpointRecord.path());
        var mappingInfo = RequestMappingInfo
            .paths(endpointRecord.path())
            .methods(endpointRecord.method())
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
}

