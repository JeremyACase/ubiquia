package org.ubiquia.common.library.implementation.service.clock;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A service that manages the internal Java clock, allowing the current time to be overridden
 * with a provided GMT time.
 */
@Service
public class ClockService {

    private static final Logger logger = LoggerFactory.getLogger(ClockService.class);

    private volatile Clock clock = Clock.systemUTC();

    /**
     * Returns the current clock instance.
     *
     * @return The active {@link Clock}.
     */
    public Clock getClock() {
        return this.clock;
    }

    /**
     * Returns the current time from the internal clock as a UTC {@link OffsetDateTime}.
     *
     * @return The current time.
     */
    public OffsetDateTime getCurrentTime() {
        logger.debug("Fetching current time from internal clock...");
        return OffsetDateTime.now(this.clock);
    }

    /**
     * Sets the internal clock to a fixed instant derived from the provided GMT time.
     * Subsequent calls to {@link #getClock()} or {@link #getCurrentTime()} will reflect
     * the new time.
     *
     * @param gmtTime The GMT time to set the clock to.
     */
    public void setTime(final OffsetDateTime gmtTime) {
        logger.info("Setting internal clock to GMT time: {}", gmtTime);
        var utcInstant = gmtTime.withOffsetSameInstant(ZoneOffset.UTC).toInstant();
        this.clock = Clock.fixed(utcInstant, ZoneOffset.UTC);
        logger.info("...internal clock set to: {}", this.clock.instant());
    }
}
