package org.ubiquia.common.model.ubiquia.node.backpressure;

/**
 * A class to maintain ingress settings for adapters.
 */
public class Ingress {

    private Long queuedRecords;

    private Float queueRatePerMinute;

    public Long getQueuedRecords() {
        return queuedRecords;
    }

    public void setQueuedRecords(Long queuedRecords) {
        this.queuedRecords = queuedRecords;
    }

    public Float getQueueRatePerMinute() {
        return queueRatePerMinute;
    }

    public void setQueueRatePerMinute(Float queueRatePerMinute) {
        this.queueRatePerMinute = queueRatePerMinute;
    }
}
