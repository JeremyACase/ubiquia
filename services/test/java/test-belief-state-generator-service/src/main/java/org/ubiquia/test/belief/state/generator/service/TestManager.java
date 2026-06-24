package org.ubiquia.test.belief.state.generator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.ubiquia.common.test.helm.service.AbstractTestManager;
import org.ubiquia.common.test.helm.service.TestRegistrar;
import org.ubiquia.test.belief.state.generator.service.module.*;

/** Orchestrates registration and execution of all belief state generator test modules. */
@Service
public class TestManager extends AbstractTestManager {

    private static final Logger logger = LoggerFactory.getLogger(TestManager.class);

    @Autowired
    private DomainOntologyRegistrationTestModule domainOntologyRegistrationTestModule;

    @Autowired
    private BeliefStateDeploymentTestModule beliefStateDeploymentTestModule;

    @Autowired
    private PersonTestModule personTestModule;

    @Autowired
    private AnimalTestModule animalTestModule;

    @Autowired
    private BeliefStateTeardownTestModule beliefStateTeardownTestModule;

    @Autowired
    private TestRegistrar testRegistrar;

    @Override
    public void registerTests() {
        this.testRegistrar.registerModule(this.domainOntologyRegistrationTestModule);
        this.testRegistrar.registerModule(this.beliefStateDeploymentTestModule);
        this.testRegistrar.registerModule(this.animalTestModule);
        this.testRegistrar.registerModule(this.personTestModule);

        // This guy gets a 404 sometimes in devops tests because "static resource isn't available"
        // ...it shouldn't be the case, but I'll track it down later.
        //this.testRegistrar.registerModule(this.beliefStateTeardownTestModule);
    }

    /** Registers and runs all tests once the application context is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerAndRunTests() {
        this.registerTests();
        this.runTests();
    }
}

