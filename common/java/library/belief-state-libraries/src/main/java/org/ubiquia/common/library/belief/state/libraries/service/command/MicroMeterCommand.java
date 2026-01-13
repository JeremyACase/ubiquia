package org.ubiquia.common.library.belief.state.libraries.service.command;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.domain.embeddable.KeyValuePair;

/**
 * A command object to use micrometer to feed Prometheus metrics.
 */
@ConditionalOnProperty(
    value = "management.endpoint.info.prometheus.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class MicroMeterCommand {

    private static final Logger logger = LoggerFactory.getLogger(MicroMeterCommand.class);
    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Start a micrometer sampling.
     *
     * @return A new sample.
     */
    public Timer.Sample startSample() {
        logger.debug("Received request to start a sample...");
        var sample = Timer.start(this.meterRegistry);
        return sample;
    }

    /**
     * Provided a sample, terminate it after adding the appropriate telemetry.
     *
     * @param sample     The sample to end.
     * @param methodName The name of the method stopping the sampling.
     * @param kvps       A list of key-value-pairs to add to our telemetry.
     */
    public void endSample(
        Timer.Sample sample,
        final String methodName,
        final List<KeyValuePair> kvps) {

        logger.debug("Received request to end a sample for name {}...", methodName);

        var tags = new ArrayList<Tag>();
        for (var kvp : kvps) {
            var tag = Tag.of(kvp.getKey(), kvp.getValue());
            tags.add(tag);
        }

        var timer = Timer.builder(methodName)
            .publishPercentileHistogram(true)
            .tags(tags)
            .register(this.meterRegistry);

        sample.stop(timer);

        logger.debug("...stopped sample for name {}...", methodName);
    }
}