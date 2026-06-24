package org.ubiquia.core.communication.interfaces;

/** Interface for DAO controller proxies that provide a downstream URL helper. */
public interface InterfaceUbiquiaDaoControllerProxy {

    /** Returns the base URL of the downstream service this proxy targets. */
    String getUrlHelper();

}
