package org.ubiquia.core.flow.interfaces;

/**
 * An interface defining bootstrappers - or classes dedicated to initializing a Ubiquia
 * agent with some initial data (e.g., ACLs, DAGs, belief states, etc.).
 */
public interface InterfaceBootstrapper {

    /**
     * Bootstraps the agent with initial data.
     *
     * @throws Exception Exceptions from issuing occurring during bootstrapping.
     */
    void bootstrap() throws Exception;
}
