# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.10] - 2026-06-23
### Fixed
- Resolved all checkstyle linting warnings in test sources: added missing Javadoc on
  `BeliefStateGeneratorTest`, wrapped a long `.as(...)` assertion in
  `BeliefStateGenerationSupportProcessorTest`, renamed `dList`/`sList` to `depList`/`svcList` to
  satisfy the local variable naming pattern in `BeliefStateOperatorTest`, shortened an over-length
  test method name, moved `spy` and `deployment` variable declarations adjacent to their first use
  to satisfy `VariableDeclarationUsageDistance`.

## [0.37.1] - 2026-06-15
### Fixed
- `KeyValuePairEntity.key`: field mapped to SQL reserved word `key` as a bare column name; `UbiquiaDomainEntityGenerator.attachColumnAnnotationsToEmbeddables()` now detects reserved-word field names on embeddable models and emits `@Column(name = "pair_<field>")`, resolving H2 syntax errors at runtime
- `BeliefStateGenerationSupportProcessor.SupportTemplate.APPLICATION`: destination path typo (`org/ubiquiadomainl/generated/`) placed `Application.java` in a directory that did not match its package declaration, causing every generated belief state compilation to fail; corrected to `org/ubiquia/domain/generated/`
- Stale `Application.java` left at the typo'd path from a prior run was compiled alongside the correctly placed file, causing a duplicate-class error; artifact removed

### Changed
- `BeliefStateCompiler`: collects `DiagnosticCollector` output and logs each error at `ERROR` level before throwing, surfacing compiler errors in logs without requiring `--stacktrace`
- Resolved all Google Java Style checkstyle warnings across the service: missing Javadoc on types and methods, `NeedBraces`, `LineLength`, `OperatorWrap`, `VariableDeclarationUsageDistance`, `CustomImportOrder`, `EmptyLineSeparator`, `SummaryJavadoc`, and `AbbreviationAsWordInName`; `BLACKLISTED_FILENAMES` renamed to `blacklistedFilenames`

## [0.37.0] - 2026-06-12
### Added
- `SchemaTransformer` interface: single-method contract (`transform(String) throws IOException`) implemented by all schema preprocessing steps
- `SchemaTransformationPipeline`: Spring-managed Chain of Responsibility that applies `@Order`-sorted `SchemaTransformer` beans in sequence; replaces ad-hoc inline transforms in `BeliefStateGenerator`
- `AbstractOpenApiGenerator`: Template Method base class for OpenAPI code generators; subclasses supply `getGeneratorName()`, `getTemplatePath()`, and optional hooks `configureAdditionalProperties()` / `buildGlobalProperties()`; `generate(String)` handles temp-file creation, `CodegenConfigurator` setup, and `DefaultGenerator` invocation
- `K8sLabelBuilder`: fluent builder for Kubernetes label maps; `from(map)` copies a base map, `put()` / `remove()` mutate entries, `withBeliefState(name)` stamps the standard `belief-state` and `app` labels
- `K8sResourceClient<T, L>`: generic wrapper around `GenericKubernetesApi<T, L>` that binds a namespace at construction; exposes `get`, `create`, `delete`, `list`, `listBySelector`, `deleteBySelector`, and `getApi()`
- `BeliefStateGenerationSupportProcessor.SupportTemplate`: package-private enum replacing the previous inline resource-path constants; each constant carries `resourcePath`, `destinationPath`, and `requiresReplacement` fields
- Unit tests: `K8sLabelBuilderTest`, `K8sResourceClientTest`, `BeliefStateOperatorTest`, `SchemaTransformationPipelineTest`, `InheritancePreprocessorTest`, `BeliefStateGenerationSupportProcessorTest`; `BeliefStateDeploymentBuilderTest` extended with label-map independence assertions

### Changed
- `OpenApiDtoGenerator` and `OpenApiEntityGenerator` now extend `AbstractOpenApiGenerator` instead of duplicating `CodegenConfigurator` setup; generator-specific properties are supplied via `configureAdditionalProperties()` overrides
- `BeliefStateOperator`: `deploymentApi` and `serviceApi` fields replaced by typed `K8sResourceClient<V1Deployment, V1DeploymentList>` and `K8sResourceClient<V1Service, V1ServiceList>` instances; namespace is bound at construction rather than passed to every call
- `BeliefStateDeploymentBuilder`: label maps now constructed via `K8sLabelBuilder`; deployment and service label maps are independent copies

### Fixed
- `InheritancePreprocessor.transform()`: was returning the original schema string (`jsonSchema`) instead of the modified serialization (`modified`); all schema transformations were silently discarded
- `InheritancePreprocessor.processContainer()`: added name-based exclusions for `AbstractDomainModel` (prevented self-referencing `allOf` that caused a duplicate `getModelType()` compile error in every generated class) and `KeyValuePair` (prevented a circular `allOf` reference that broke its EMBEDDABLE classification and caused codegen to skip generating the class)
- `AbstractOpenApiGenerator.generate()`: added `openApiNullable=false`; `JavaClientCodegen` defaults to `true`, wrapping nullable fields in `JsonNullable<>` and requiring `jackson-databind-nullable` on the generated project's classpath
- `UbiquiaModelInjector`: removed `modelType` from the injected `AbstractDomainModel` property set; the DTO and entity Mustache templates already emit `@Override public String getModelType()`, so including it as a schema property produced a duplicate method in every generated non-embeddable class
- `BeliefStateGenerationCleanupProcessor`: removed `KeyValuePair.java` and `KeyValuePairEntity.java` from the deletion blacklist; generated domain models import these from `org.ubiquia.domain.generated` and require them to be present for compilation to succeed; the embeddable support files (`Controller`, `Repository`, `RelationshipBuilder`, `IngressDtoMapper`, `EgressDtoMapper`) are already removed by the generators' own `postProcessFile` logic

## [0.34.0] - 2026-05-26
### Fixed
- `BeliefStateOperator.tryDeployBeliefState()`: deployment existence check used `getName().toLowerCase() + getVersion()` (e.g. `"pets1.2.3"`) as the lookup key instead of the canonical Kubernetes name produced by `BeliefStateNameBuilder.getKubernetesBeliefStateNameFrom()` (e.g. `"pets-belief-state-1-2-3"`); injected `BeliefStateNameBuilder` and corrected the check, eliminating intermittent `AlreadyExists` 400 errors when the same domain ontology was deployed more than once

## [0.30.1] - 2026-05-19
### Changed
- `ObjectController.java.template`: updated to extend `AbstractDomainModelController<ObjectMetadataEntity, ObjectMetadataDto>` with belief-state-owned entity and DTO types; removed dependency on core `GenericUbiquiaDaoController`, `ObjectMetadata`, and `ObjectMetadataEntity`
- `Application.java.template`: `@EntityScan` and `@EnableJpaRepositories` now include `org.ubiquia.common.library.belief.state.libraries.entity` and `org.ubiquia.common.library.belief.state.libraries.repository` so the `ObjectMetadataEntityRepository` bean is registered in generated services

### Fixed
- `build.gradle`: `clean` task now deletes `belief-state-libs/` directory, preventing stale JARs from being used when compiling generated code after a rebuild

## [0.30.0] - 2026-05-18
### Changed
- `BeliefStateGenerationSupportProcessor`: `resolveDbTokens()` (formerly `resolveDbTokensFromBooleans()`) now unconditionally returns H2 embedded database config; `h2.enabled` and `yugabyte.enabled` conditional logic removed

### Removed
- `jdbc-yugabytedb` runtime dependency
- `@Value("${ubiquia.agent.database.h2.enabled:false}")` and `@Value("${ubiquia.agent.database.yugabyte.enabled:false}")` fields from `BeliefStateGenerationSupportProcessor`

## [0.22.0] 2026-04-01
### Added
- `SimulationController`: REST endpoint `POST /ubiquia/core/belief-state-generator-service/simulation/clock/set` to update the service clock via `ClockService`; conditional on `ubiquia.mode != PROD`
- `ubiquia.mode` property in `application.yaml` (default `PROD`), propagated from Helm configmap

## [0.18.0] 2026-03-18
### Changed
- Micrometer version now managed by Spring Boot BOM

## [0.12.1] 2025-10-28
### Fixed
- Fixed generation of embedded models

## [0.12.0] 2025-10-24
### Added
- Ability to infer "embedded" models and handle them accordingly

## [0.8.12] 2025-10-01
### Fixed
- Fixed an endpoint

## [0.4.0] 2025-07-03
### Added
- Minio services and minio injection for generated belief states 

## [0.2.0] 2025-07-03
### Added
- Teardown for belief states
### Fixed
- Teardown now properly works for graph K8s resources

## [0.1.0] 2025-05-21
### Added
- Initial commit
