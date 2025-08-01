package org.ubiquia.core.flow.interfaces;


/**
 * An interface defining bootstrappers - or classes dedicated to initializing a Ubiquia
 * agent with some initial data (e.g., ACLs, DAGs, belief states, etc.)
 */
public interface InterfaceBootstrapper {

    /**
     * Bootstrap!
     *
     * @throws Exception Exceptions from issuring occurring during bootstrapping.
     */
    void bootstrap() throws Exception;
}
