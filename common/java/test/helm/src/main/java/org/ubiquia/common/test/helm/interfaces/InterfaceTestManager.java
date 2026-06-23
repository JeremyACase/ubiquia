package org.ubiquia.common.test.helm.interfaces;

/** Interface for test managers. */
public interface InterfaceTestManager {

    /** Registers all tests. */
    void registerTests();

    /** Runs all registered tests. */
    void runTests();

}