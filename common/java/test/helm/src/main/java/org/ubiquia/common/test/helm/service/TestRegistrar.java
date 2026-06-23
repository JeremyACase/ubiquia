package org.ubiquia.common.test.helm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.test.helm.interfaces.InterfaceHelmTestModule;

/** Manages registration of helm test modules. */
@Service
public class TestRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(TestRegistrar.class);

    private List<InterfaceHelmTestModule> registeredTestModules;

    /** Registers a test module. */
    public void registerModule(final InterfaceHelmTestModule testModule) {

        logger.info("Registering test module {}...",
            testModule.getClass().getSimpleName());

        if (Objects.isNull(this.registeredTestModules)) {
            this.registeredTestModules = new ArrayList<>();
        }

        this.registeredTestModules.add(testModule);

        logger.info("...registered.");
    }

    public List<InterfaceHelmTestModule> getRegisteredTestModules() {
        return this.registeredTestModules;
    }
}