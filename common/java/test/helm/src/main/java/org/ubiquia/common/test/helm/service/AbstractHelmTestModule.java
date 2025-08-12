package org.ubiquia.common.test.helm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceLogger;
import org.ubiquia.common.test.helm.interfaces.InterfaceHelmTestModule;

@Service
public abstract class AbstractHelmTestModule implements InterfaceHelmTestModule, InterfaceLogger {

    @Autowired
    protected TestState testState;

    @Override
    public void doSetup() {
        this.getLogger().info("No test setup defined...");
    }

    @Override
    public void doCleanup() {
        this.getLogger().info("No test cleanup defined...");
    }
}
