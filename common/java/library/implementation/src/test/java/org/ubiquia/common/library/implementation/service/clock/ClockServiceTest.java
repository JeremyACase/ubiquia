package org.ubiquia.common.library.implementation.service.clock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClockServiceTest {

    private ClockService clockService;

    @BeforeEach
    public void setup() {
        this.clockService = new ClockService();
    }

    @Test
    public void assertDefaultClockIsUtc_isValid() {
        var clock = this.clockService.getClock();
        Assertions.assertEquals(ZoneOffset.UTC, clock.getZone());
    }

    @Test
    public void assertGetCurrentTimeReturnsUtcOffset_isValid() {
        var currentTime = this.clockService.getCurrentTime();
        Assertions.assertEquals(ZoneOffset.UTC, currentTime.getOffset());
    }

    @Test
    public void assertSetTimeUpdatesCurrentTime_isValid() {
        var targetTime = OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        this.clockService.setTime(targetTime);
        Assertions.assertEquals(targetTime, this.clockService.getCurrentTime());
    }

    @Test
    public void assertSetTimeNormalizesToUtc_isValid() {
        var targetTimeEst = OffsetDateTime.of(2025, 6, 15, 8, 0, 0, 0, ZoneOffset.ofHours(-4));
        var expectedUtc = OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        this.clockService.setTime(targetTimeEst);
        Assertions.assertEquals(expectedUtc, this.clockService.getCurrentTime());
    }

    @Test
    public void assertSetTimeCanBeCalledMultipleTimes_isValid() {
        var firstTime = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var secondTime = OffsetDateTime.of(2026, 6, 15, 12, 30, 0, 0, ZoneOffset.UTC);
        this.clockService.setTime(firstTime);
        Assertions.assertEquals(firstTime, this.clockService.getCurrentTime());
        this.clockService.setTime(secondTime);
        Assertions.assertEquals(secondTime, this.clockService.getCurrentTime());
    }

    @Test
    public void assertClockIsFixedAfterSetTime_isValid() throws InterruptedException {
        var targetTime = OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        this.clockService.setTime(targetTime);
        Thread.sleep(10);
        Assertions.assertEquals(targetTime, this.clockService.getCurrentTime());
    }
}
