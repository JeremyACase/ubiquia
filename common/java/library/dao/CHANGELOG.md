# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.5] 2026-06-22
### Changed
- Fixed checkstyle warnings in test source: added missing Javadoc on all public test classes, interfaces, and helper methods; corrected import ordering in `AnimalRepository`, `PersonRepository`, `TestHelper`, and `Person`; renamed `assertDerivesSubclass_IsValid` to `assertDerivesSubclass_isValid` in `ClassDeriverTest` to satisfy `MethodName` suppression; restructured `assertQueriesForNestedEmbeddedData_isValid` in `ParameterDaoTest` to resolve `VariableDeclarationUsageDistance`; split long lines in `GenericUbiquiaDaoControllerTest`

## [0.38.4] 2026-06-22
### Changed
- Fixed checkstyle warnings in main source: added missing Javadoc on classes, interfaces, constructors, and public methods; corrected import ordering in `NestedPredicateBuilder`; split lines exceeding 100 characters in `NestedPredicateBuilder`, `ClassDeriver`, and `GenericUbiquiaDaoController`; replaced `default: { }` blocks with brace-free form in `NonNestedPredicateBuilder` to satisfy `LeftCurlyNl`; renamed field `persistedDTOClass` to `persistedDtoClass` to resolve `AbbreviationAsWordInName`

## [0.18.0] 2026-03-18
### Added
- Controller and model tags to telemetry metrics

## [0.1.0] 2025-05-21
### Added
- Initial commit
