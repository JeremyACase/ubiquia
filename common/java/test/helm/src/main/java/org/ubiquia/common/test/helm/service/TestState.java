package org.ubiquia.common.test.helm.service;


import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Tracks the current state of helm test execution. */
@Service
public class TestState {

    private static final Logger logger = LoggerFactory.getLogger(TestState.class);
    private Boolean passed;
    private List<String> failures;

    /** Initializes test state with an empty failure list and passed set to true. */
    public TestState() {
        this.failures = new ArrayList<>();
        this.passed = true;
    }

    public Boolean getPassed() {
        return passed;
    }

    /** Records a test failure and marks the overall test run as failed. */
    public void addFailure(final String failureReason) {
        if (this.getPassed()) {
            logger.info("Setting state of testing passed to FALSE...");
            this.passed = false;
        }
        logger.error("ERROR: {}", failureReason);
        this.failures.add(failureReason);
    }

    public List<String> getFailures() {
        return failures;
    }

    public void setFailures(final List<String> failures) {
        this.failures = failures;
    }
}