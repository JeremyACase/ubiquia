package org.ubiquia.common.test.helm.service;


import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TestState {

    private static final Logger logger = LoggerFactory.getLogger(TestState.class);
    private Boolean passed;
    private List<String> failures;

    public TestState() {
        this.failures = new ArrayList<>();
        this.passed = true;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void addFailure(final String failureReason) {
        if (this.getPassed()) {
            logger.info("Setting state of testing passed to FALSE...");
            this.passed = false;
        }
        this.failures.add(failureReason);
    }

    public List<String> getFailures() {
        return failures;
    }

    public void setFailures(final List<String> failures) {
        this.failures = failures;
    }
}