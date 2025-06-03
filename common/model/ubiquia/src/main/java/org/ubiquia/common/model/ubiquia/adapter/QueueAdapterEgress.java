package org.ubiquia.common.model.ubiquia.adapter;

import org.ubiquia.common.model.ubiquia.dto.FlowEventDto;

/**
 * A special egress model for queue adapters.
 */
public class QueueAdapterEgress {

    private Long queuedRecords;

    private FlowEventDto flowEvent;

    public Long getQueuedRecords() {
        return queuedRecords;
    }

    public void setQueuedRecords(Long queuedRecords) {
        this.queuedRecords = queuedRecords;
    }

    public FlowEventDto getFlowEvent() {
        return flowEvent;
    }

    public void setFlowEvent(FlowEventDto flowEvent) {
        this.flowEvent = flowEvent;
    }
}
