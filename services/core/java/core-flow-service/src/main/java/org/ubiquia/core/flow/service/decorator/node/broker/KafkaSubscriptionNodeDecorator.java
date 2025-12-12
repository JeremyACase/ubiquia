package org.ubiquia.core.flow.service.decorator.node.broker;


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.SubscribeNode;


/**
 * This is a decorator service that is used to specifically add Kafka subscriptions to
 * adapters.
 */
@ConditionalOnProperty(
    value = "ubiquia.broker.kafka.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class KafkaSubscriptionNodeDecorator {

    private static final AtomicLong endpointIdIndex = new AtomicLong(1);
    private static final Logger logger = LoggerFactory.getLogger(KafkaSubscriptionNodeDecorator.class);
    @Autowired
    private KafkaListenerContainerFactory kafkaListenerContainerFactory;
    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * Initialize an adapter to have a subscription to a Kafka topic.
     *
     * @param node The adapter to initialize.
     */
    public void initializeKafkaSubscriptionFor(final SubscribeNode node) {
        var listener = this.getKafkaListenerEndpointFor(node);
        this.kafkaListenerEndpointRegistry.registerListenerContainer(
            listener,
            this.kafkaListenerContainerFactory,
            true);
    }

    /**
     * Provided an adapter, build the appropriate Kafka endpoint for it.
     *
     * @param node The adapter to use to build a Kafka endpoint.
     * @return The kafka endpoint.
     */
    private KafkaListenerEndpoint getKafkaListenerEndpointFor(final SubscribeNode node) {

        var methods = Arrays
            .stream(node.getClass().getMethods())
            .toList();

        var match = methods
            .stream()
            .filter(x -> x.getName().contains("onMessage"))
            .findFirst();

        if (match.isEmpty()) {
            throw new RuntimeException("ERROR: Unable to initialize - could not find an "
                + "'onMessage' method!");
        }

        var kafkaListenerEndpoint = this.getMethodKafkaListenerFor(node);
        kafkaListenerEndpoint.setBean(node);
        kafkaListenerEndpoint.setMethod(match.get());

        return kafkaListenerEndpoint;
    }

    /**
     * A helper method to build a MethodKafkaListenerEndpoint.
     *
     * @param node The adapter to use.
     * @return A MethodKafkaListenerEndpoint.
     */
    private MethodKafkaListenerEndpoint<String, String> getMethodKafkaListenerFor(
        final SubscribeNode node) {
        var kafkaListenerEndpoint = new MethodKafkaListenerEndpoint<String, String>();
        kafkaListenerEndpoint.setId(this.generateListenerIdFor(node));
        kafkaListenerEndpoint.setGroupId("ubiquia-nodes");
        kafkaListenerEndpoint.setAutoStartup(true);
        kafkaListenerEndpoint.setTopics(node
            .getNodeContext()
            .getBrokerSettings()
            .getTopic());
        kafkaListenerEndpoint.setMessageHandlerMethodFactory(
            new DefaultMessageHandlerMethodFactory());
        return kafkaListenerEndpoint;
    }

    /**
     * Generate a Kafka listener ID for the provided adapter.
     *
     * @param adapter The adapter to generate an ID for.
     * @return An id.
     */
    private String generateListenerIdFor(final SubscribeNode adapter) {
        var context = adapter.getNodeContext();
        var id = context
            .getGraph().getName()
            + "-"
            + context.getComponent().getName()
            + "-"
            + endpointIdIndex.getAndIncrement();
        return id;
    }
}
