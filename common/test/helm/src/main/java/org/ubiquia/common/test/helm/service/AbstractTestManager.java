package org.ubiquia.common.test.helm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.test.helm.interfaces.InterfaceTestManager;

@Service
public abstract class AbstractTestManager implements InterfaceTestManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTestManager.class);

    @Autowired
    private TestRegistrar testRegistrar;

    @Autowired
    private TestState testState;

    public void runTests() {

        logger.info("Proceeding to run tests...");

        for (var testModule : this.testRegistrar.getRegisteredTestModules()) {
            testModule.doSetup();
            testModule.doTests();
            testModule.doCleanup();
        }

        logger.info("...all tests completed.");

        var exitCode = 0;
        if (!this.testState.getPassed()) {
            exitCode = 1;

            for (var failure : this.testState.getFailures()) {
                logger.info("Failure: {}", failure);
            }
        }

        if (exitCode == 0) {
            logger.info("Tests SUCCESSFUL; exiting...");
        } else {
            logger.info("Tests FAILED; exiting...");
        }

        System.exit(exitCode);
    }
}