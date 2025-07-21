# Architecture Decision Record: Helm

## Decision
Ubiquia will use Helm as its package manager and configuration management tool. 

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Industry-standard versioning and configuration of a Kubernetes application
- Artifacts can be made available in artifact repositories

### Cons
- Helm has a learning curve

### Alternatives
- Using Kustomize paired with a custom versioning method 

## Context

Helm is primarily two things: it is a package manager (of Kubernetes tools and applications), and it is a Kubernetes application configuration management tool.

As a package manager, Helm is conceptually like other package managers: it can be used to install applications, define dependencies, and manage versions. In these use-cases, it's identical to tools like Pip, Gradle, or Node Package Manager (NPM.) Where it differs with these tools--primarily--is that it is concerned not with Python, Java, or JavaScript (respectively), but with Kubernetes, and therefore it is often concerned with entire software systems (instead of components.)

As a package manager of software systems, Helm also doubles as a configuration manager. It can be used to upgrade running Kubernetes applications, or change their configuration...even at runtime. 

In either case, Helm works by adding a layer of abstraction over Kubernetes's native "manifest files." Whereas Kubernetes knows how to interpret these files directly, Helm allows developers to define "template" files which allow devs to "inject" variables into. During installation (or upgrade), Helm will effectively transpile these template files to generate "native" manifest files, which Kubernetes can in turn interpret. 

Helm also allows developers to define "Helm Tests" which can be run in automated pipelines. These tests represent a sort of testing framework, and so devs are free to use them to define any conceivable level of test: from a "unit" test to a full-up systems-level regression test. Importantly, this feature of Helm readily plugs into CI/CD tools like Flux, thus enabling automated deployments of updates.

Helm has quickly become a Kubernetes standard tool. There are tools that overlap with Helm (e.g., Kustomize as a configuration management tool), but there is no other tool that makes a satisfactory compromise between ubiquity, readability, power, flexibility, package management, and configuration management. 


## Consequences & Tradeoffs
It is the author's opinion that there is little risk of adopting Helm and much to be gained. Helm implies a learning curve--as well as some developer time to maintain--but so, too, do "raw" Kubernetes manifest files, as does any other roughly-analogous tools (again, like Kustomize.)

Using Helm, it will be possible to run automated pipelines of Ubiquia and publish versioned Helm charts of Ubiquia. These charts, in turn, will allow for repeatable and configurable installations of specific Ubiquia versions that have been published only because they've passed automated testing. It will then, also, allow for rollbacks of Ubiquia should the need arise. 


## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com