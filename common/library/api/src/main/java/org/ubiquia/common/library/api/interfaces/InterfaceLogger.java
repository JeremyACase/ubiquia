package org.ubiquia.common.library.api.interfaces;


import org.slf4j.Logger;

/**
 * An interface defining methods that can be used to egress data over brokers.
 */
public interface InterfaceLogger {

    /**
     * Get the logger from the implementing class.
     *
     * @return The logger used.
     */
    Logger getLogger();
}
