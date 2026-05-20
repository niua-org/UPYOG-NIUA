# Changelog
All notable changes to this module will be documented in this file.

---

# 1.3.0 - 2026-05-20

## Added

* Added handler-based inbox architecture
* Added `ModuleInboxHandler`
* Added `ModuleHandlerRegistry`
* Added `InboxAssembler`
* Added `InboxContext`
* Added module-specific handler implementations
* Added module-specific workflow customization
* Added module-specific business object mapping
* Added configurable workflow total count handling
* Added configurable nearing SLA count handling
* Added ElasticSearch integration handling

---

## Refactored

### Previous Architecture

Previously all module-specific logic was tightly coupled inside:

```
InboxService
```

The service contained:

* Module-specific conditions
* Workflow customizations
* SLA customizations
* Status mapping logic
* Application id fetching logic
* Business object mapping logic

Example:

```
if(module.equals("PT")) {
   ...
}
else if(module.equals("FSM")) {
   ...
}
```

Problems:

* Large monolithic service
* Difficult maintenance
* Difficult onboarding of new modules
* High coupling
* Repeated conditional logic
* Difficult testing

---

### New Architecture

Module-specific logic has been moved into dedicated handlers.

New flow:

```
InboxService
    ->
ModuleHandlerRegistry
    ->
ModuleInboxHandler
    ->
InboxAssembler
```

Benefits:

* Modular architecture
* Easy module onboarding
* Cleaner code separation
* Better maintainability
* Better extensibility
* Reduced conditional logic
* Improved readability
* Improved testability

---

## Added Module Handlers

* ASSETModuleHandler
* BPAModuleHandler
* ChallanModuleHandler
* CHBModuleHandler
* CNDModuleHandler
* EWasteModuleHandler
* FSMModuleHandler
* MTModuleHandler
* NDCModuleHandler
* NOCModuleHandler
* PGRAiModuleHandler
* PTModuleHandler
* PTRModuleHandler
* SVModuleHandler
* TLModuleHandler
* TPModuleHandler
* WSModuleHandler
* WTModuleHandler

---

## Moved Logic From InboxService To Handlers

Moved:

* Application id fetching
* Module-specific counts
* Status enrichment
* Business object mapping
* Workflow customization
* SLA handling flags
* Search parameter cleanup
* ElasticSearch integration handling

---

## 1.2.2 - 2023-01-31

- Changed 1.2.2-beta version to 1.2.2

## 1.2.2-beta - 2022-09-10

- Added elastic search call for privacy audit report.

## 1.2.1 - 2022-08-29

- Added serviceSLA for W&S inbox objects

## 1.2.0 - 2022-08-18

- Support Inbox for  WnS service

## 1.1.0 - 2022-01-13

- Updated to log4j2 version 2.17.1

## 1.1.0

- Support new Inbox for for OBPS service, stakeholder registration service, and NOC service

## 1.0.0

- base version