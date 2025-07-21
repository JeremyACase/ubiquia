# Architecture Decision Record: Helm Chart Tests

## Decision
Ubiquia will leverage Helm's "Chart Test" feature, and this feature will be used in automated testing in CI/CD devops pipelines as well as with CI/CD tools towards upgrading production Ubiquia agents.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Allows for containerized, integration and end-to-end testing
- This testing can be run both "locally" and in "pipelines" because it is configured via Helm

### Cons
- Containerized tests require a lot of developer time

### Alternatives
- Make e2e testing as Scripts 

## Context
Helm has a [Chart Test](https://helm.sh/docs/topics/chart_tests/) feature that can be used towards testing. Importantly, these tests are considered pass/fail solely based on the exit code returned from the containers instantiated by these tests. For this reason, these tests can be anything from unit tests all the way up to full systems-regression testing. 

Many useful properties "fall out" of this feature. For one, these "chart tests" can also be used by CI/CD tools (like [Flux](https://fluxcd.io/)) to do rolling system updates - _but only when all chart tests pass_. This can also be used in an automated pipeline for use by devs to see if their changes have potentially broken any other components in the system. Last-but-not-least, this feature can be used by devs to test an entire Ubiquia agent in a local Kubernetes cluster.

## Consequences & Tradeoffs
Testing in software development is a foregone conclusion; its absence amounts to professional negligence if not malfeasance. It is not a matter of whether to test or not, but _how_ to test. Most languages provide standardized tools (e.g., [Junit](https://junit.org/junit5/) for Java), but testing an entire software system is a different ball game. 

Helm's chart tests, then, represent only the framework with which to test Ubiquia "at the system level." It is still up to the developers themselves to build containerized "test logic" that can return exit codes to Helm to determine pass/fail. And systems testing will only be as good as these tests. 

Testing is not a tradeoff per se, but making good tests is more art that science. Not unlike scientific theories, good software testing doesn't mean an absence of bugs, it simply means the tests found no bugs for the code in question. More test code means more certainty in an absence of bugs. But more test code means more code, and even test code must be maintained lest it rot. Therefore, Ubiquia's "Chart Test" approach will result in a fair amount of overhead required of devs to implement, maintain, and deliver containerized testing logic.

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com