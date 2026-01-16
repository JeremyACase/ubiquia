package org.ubiquia.test.flow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.ubiquia.common.test.helm.service.AbstractTestManager;
import org.ubiquia.common.test.helm.service.TestRegistrar;
import org.ubiquia.test.flow.service.module.ComponentDeploymentTestModule;
import org.ubiquia.test.flow.service.module.ComponentTeardownTestModule;

@Service
public class TestManager extends AbstractTestManager {

    private static final Logger logger = LoggerFactory.getLogger(TestManager.class);

    @Autowired
    private ComponentDeploymentTestModule componentDeploymentTestModule;

    @Autowired
    private ComponentTeardownTestModule componentTeardownTestModule;

    @Autowired
    private TestRegistrar testRegistrar;

    @Override
    public void registerTests() {
        this.testRegistrar.registerModule(this.componentDeploymentTestModule);
        //this.testRegistrar.registerModule(this.componentTeardownTestModule);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAndRunTests() {
        this.registerTests();
        this.runTests();
    }
}

