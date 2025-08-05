package org.ubiquia.core.flow.service.decorator.adapter.broker;

import static org.ubiquia.common.model.ubiquia.enums.BrokerType.KAFKA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.SubscribeAdapter;

/**
 * A service that can be used to "decorate"--add on--functionality to adapters.
 */
@Service
public class AdapterBrokerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(AdapterBrokerDecorator.class);
    @Autowired(required = false)
    private KafkaSubscriptionAdapterDecorator kafkaSubscriptionAdapterDecorator;

    /**
     * Initialize an adapter so that it is subscribed to a broker topic.
     *
     * @param subscribeAdapter The adapter to initialize a subscription for.
     */
    public void initializeBrokerSubscriptionFor(SubscribeAdapter subscribeAdapter) {

        var type = subscribeAdapter.getAdapterContext().getBrokerSettings().getType();
        switch (type) {

            case KAFKA: {
                this.kafkaSubscriptionAdapterDecorator.initializeKafkaSubscriptionFor(
                    subscribeAdapter);
            }
            break;

            default: {
                throw new RuntimeException("ERROR: Unrecognized broker type: " + type);
            }
        }
    }
}
