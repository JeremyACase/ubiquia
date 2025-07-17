package org.ubiquia.test.belief.state.generator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.ubiquia.common.test.helm.service.AbstractTestManager;
import org.ubiquia.common.test.helm.service.TestRegistrar;

@Service
public class TestManager extends AbstractTestManager {

    private static final Logger logger = LoggerFactory.getLogger(TestManager.class);

    @Autowired
    private AclRegistrationTestModule aclRegistrationTestModule;

    @Autowired
    private BeliefStateDeploymentTestModule beliefStateDeploymentTestModule;

    @Autowired
    private PersonPostTestModule personPostTestModule;

    @Autowired
    private PersonQueryTestModule personQueryTestModule;

    @Autowired
    private AnimalPostTestModule animalPostTestModule;

    @Autowired
    private AnimalQueryTestModule animalQueryTestModule;

    @Autowired
    private BeliefStateTeardownTestModule beliefStateTeardownTestModule;

    @Autowired
    private TestRegistrar testRegistrar;

    @Override
    public void registerTests() {
        this.testRegistrar.registerModule(this.aclRegistrationTestModule);
        this.testRegistrar.registerModule(this.beliefStateDeploymentTestModule);
        this.testRegistrar.registerModule(this.personPostTestModule);
        this.testRegistrar.registerModule(this.personQueryTestModule);
        this.testRegistrar.registerModule(this.animalPostTestModule);
        this.testRegistrar.registerModule(this.animalQueryTestModule);
        this.testRegistrar.registerModule(this.beliefStateTeardownTestModule);

    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAndRunTests() {
        this.registerTests();
        this.runTests();
    }
}

