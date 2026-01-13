package org.ubiquia.common.model.ubiquia.node;

import org.ubiquia.common.model.ubiquia.dto.FlowEvent;

/**
 * A special egress model for queue adapters.
 */
public class QueueNodeEgress {

    private Long queuedRecords;

    private FlowEvent flowEvent;

    public Long getQueuedRecords() {
        return queuedRecords;
    }

    public void setQueuedRecords(Long queuedRecords) {
        this.queuedRecords = queuedRecords;
    }

    public FlowEvent getFlowEvent() {
        return flowEvent;
    }

    public void setFlowEvent(FlowEvent flowEvent) {
        this.flowEvent = flowEvent;
    }
}
