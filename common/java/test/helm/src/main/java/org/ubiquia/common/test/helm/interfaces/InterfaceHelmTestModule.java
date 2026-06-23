package org.ubiquia.common.test.helm.interfaces;

/** Interface for helm test modules. */
public interface InterfaceHelmTestModule {

    /** Performs test setup. */
    void doSetup();

    /** Executes the tests. */
    void doTests();

    /** Performs test cleanup. */
    void doCleanup();

}