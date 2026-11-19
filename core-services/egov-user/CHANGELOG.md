# Changelog
All notable changes to this module will be documented in this file.

## 2.1.0 - 2026-11-12
- Standardized `ug_user_document` linkage via `user_uuid` (VARCHAR 64), replacing deprecated numeric `user_id`.
- Replaced ambiguous property aliases with canonical `@JsonProperty("userUuid")` across `UserDocument`.
- Added 3NF member asset storage tables and repositories for properties (`ug_user_property`, `ug_user_urban_property`, `ug_user_rural_property`, `ug_user_property_owner`), income declarations (`ug_user_income`), and vehicles (`ug_user_vehicle`).
- Architectural separation of concerns: Extracted all SQL query strings and dynamic builders into dedicated `@Component` QueryBuilder classes (`UserDocumentQueryBuilder`, `UserPropertyQueryBuilder`, `UserIncomeQueryBuilder`, `UserVehicleQueryBuilder`), strictly separating queries from JDBC repositories.
- Completely eliminated all inline imports / fully qualified class names across production and test codebases, moving all declarations to top-level imports.
- Added `@PostMapping("/_sync-member-assets")` in `ProfileController` to synchronize family member asset state directly into citizen profile registry.
- Implemented 520 realistic Indian mock citizen personas in `MockAadhaarRecordService` with non-repeating atomic round-robin sequential dispensing for mock Aadhaar OTP authentications.
- Complete Javadoc coverage added across all classes, interfaces, enums, constructors, and methods (both public and private) across the profile, asset, multi-auth, and document modules.
- Refactored hardcoded strings, error messages, and payload keys into centralized constant classes (`UserConstants` and `FamilyConstants`).
- Centralized all property bindings (including `user.address.*`, `egov.user.host`, `account.unlock.*`, `max.invalid.login.*`, `create.user.validate.name`, `egov.user.pwd.*`, and password expiry) exclusively into the centralized configuration class (`UserConfiguration`), eliminating 100% of `@Value` annotations from `UserService` and `UserRepository`.
- Removed all hardcoded inline fallback values (`:...`) in `@Value` annotations across `FamilyConfiguration` and `CustomTokenEnhancer`, relying strictly on properties defined in `application.properties`.
- Eliminated all static fallback sets in `FamilyMasterDataServiceImpl` (categories, occupations, property types, property usages, and relationships), making MDMS the strict single source of truth for all master data validation.
- Introduced `AuthUserAccount` interface adhering to Interface Segregation Principle (ISP) to decouple authenticated identity from concrete models, resolving class name collisions and eliminating inline imports in `UserService`.
- Standardized tenant identifier to `pg.citya` (state `pg`) across all unit and security test suites.
- Audited and verified full adherence to SOLID principles (SRP, OCP, LSP, ISP, DIP) across service, validator, adapter, and persistence layers with clean interface segregations, strategy extensions, and constructor-based dependency injection.

## 2.0.0 - 2025-04-20
- Added 3 Api endpoints V2 for new address fields to create, update and search users
- Added new address fields in address model and table - address2, houseNumber,houseName, streetName, landmark, locality
- Added new Address api endpoints to create, update and search address
- Added new status field in address table to handle update Address functionality
- During update old address will be made inactive and new address will be created 
- Unique address constraint was modified to include created date
- Duplicate 'Other' addresses can be made except 'permanent' and 'correspondence' address

## 1.3.0 - 2023-02-06

- Transition from 1.3.0-beta version to 1.3.0 version

## 1.3.0-beta - 2022-06-22

- Enhanced user service for the implementation of privacy feature.
- Added logback-classic version 1.2.0 and updated tracer to remove a critical vulnerabilities

## 1.2.7 - 2022-02-02
- Added security fixes for user enumerration issue.
- Added size validation on user models
- Added email and sms notification feature whenever user changes email.

## 1.2.6 - 2022-01-13
- Updated to log4j2 version 2.17.1

## 1.2.5 - 2021-07-26
- Added OTHERS as one of the gender option values
- Allowed names with apostrophe symbol

## 1.2.4 - 2021-05-11
- added permanentCity in oAuth response
- added html validations on input fields
- replaced OTHER with TRANSGENDER in gender enum
- corrections to error handling
- updated LOCALSETUP.md



## 1.2.3 - 2021-02-26
- Updated domain name in application.properties
- Added size validations

## 1.2.2 - 2020-01-12
- Added field relationShip with guardian and refactoration of code.

## 1.2.1 - 2020-07-14

- Upgraded to kafka 1.3.11.RELEASE

## 1.2.0 - 2020-07-02

- Added support for encrypting user PII data

## 1.1.0 - 2020-06-19

- Added password policy for additional security

## 1.0.0

- Base version
