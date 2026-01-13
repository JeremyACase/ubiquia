package org.ubiquia.common.model.ubiquia.node.backpressure;

/**
 * A class to maintain egress settings for adapters.
 */
public class Egress {

    private Integer maxOpenMessages = 1;

    private Integer currentOpenMessages = 0;


    public Integer getMaxOpenMessages() {
        return maxOpenMessages;
    }

    public void setMaxOpenMessages(Integer maxOpenMessages) {
        this.maxOpenMessages = maxOpenMessages;
    }

    public Integer getCurrentOpenMessages() {
        return currentOpenMessages;
    }

    public void setCurrentOpenMessages(Integer currentOpenConnections) {
        this.currentOpenMessages = currentOpenConnections;
    }
}
