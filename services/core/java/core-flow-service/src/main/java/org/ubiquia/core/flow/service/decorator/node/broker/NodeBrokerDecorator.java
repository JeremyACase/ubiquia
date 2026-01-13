package org.ubiquia.core.flow.service.decorator.node.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.node.SubscribeNode;

/**
 * A service that can be used to "decorate"--add on--functionality to adapters.
 */
@Service
public class NodeBrokerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(NodeBrokerDecorator.class);
    @Autowired(required = false)
    private KafkaSubscriptionNodeDecorator kafkaSubscriptionNodeDecorator;

    /**
     * Initialize an adapter so that it is subscribed to a broker topic.
     *
     * @param subscribeAdapter The adapter to initialize a subscription for.
     */
    public void initializeBrokerSubscriptionFor(SubscribeNode subscribeAdapter) {

        var type = subscribeAdapter.getNodeContext().getBrokerSettings().getType();
        switch (type) {

            case KAFKA: {
                this.kafkaSubscriptionNodeDecorator.initializeKafkaSubscriptionFor(
                    subscribeAdapter);
            }
            break;

            default: {
                throw new RuntimeException("ERROR: Unrecognized broker type: " + type);
            }
        }
    }
}
