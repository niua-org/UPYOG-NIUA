# MDMS v2 test findings

## Status

Tests were written. Maven was not run yet.

## Covered

- `MdmsDataValidator.validateCreateRequest` — new row, null and empty repository hits, duplicate row, schema rule failures (minLength, type, combined), null request, null mdms, null data, null schema, empty schema, empty reference list, missing reference, object reference, string / number / list reference found.
- `MdmsDataValidator.validateUpdateRequest` — null request, null mdms, null and empty id, whitespace id, missing row (null and empty repository), unique-field change, null stored unique identifier, case-insensitive unique match.
- `MDMSService.create` — happy path and call order, null schema, null request, null mdms, validator failure, enricher failure.
- `MDMSService.search` — one master, two masters, empty module list, null request, null criteria, null tenant, null module details, null repository result, blank filter, matching filter, invalid filter.
- `MDMSController.search` — data, empty map, null map, null body, service exception. Plain Mockito, not a Spring slice. `@Valid` is not enforced without MVC.

## Not covered

- `SchemaDefinitionService`, `WorkflowService`, `ThemeConfigService`, `MdmsDataEnricher` internals, repository SQL, `SchemaUtil`.
- `FallbackUtil` and `CompositeUniqueIdentifierGenerationUtil` as their own units (search and update call the real static methods).
- Any controller other than `MDMSController`.
- Flyway migrations and a booted Spring context.
- ArchUnit.

## Findings

- Create validation catches unexpected failures, including a null request, and throws `CustomException` (`MASTER_DATA_VALIDATION_ERR`). Schema violations are stored and thrown only after duplicate and reference checks still run.
- Update requires an id before schema checks. A null request NPEs. Null or empty id throws `MASTER_DATA_ID_ABSENT_ERR`. A whitespace id is not treated as empty; it is looked up and then reported as not found. Unique comparison uses `equalsIgnoreCase`. A null stored unique identifier NPEs.
- `MDMSService.create` returns `Arrays.asList(mdms)` when mdms itself is null (a one-element list containing null). Validator or enricher failures skip persistence.
- `MDMSService.search` copies the incoming tenant, replaces the criteria tenant with `MultiStateInstanceUtil.getStateLevelTenant`, filters with JsonPath, then calls `FallbackUtil` with the original tenant. Schema codes are split on `DOT_REGEX` into module and master. A null repository map NPEs.
- `MDMSController.search` always wraps the service map in `MdmsResponse` with HTTP 200. A null map still returns 200. Exceptions propagate. The controller does not call a repository.
- ArchUnit was not added. Only `MDMSController` was inspected; a module-wide rule could fail on an unseen controller.
- No `*IT`. Flyway is a dependency; a bootable context was not confirmed.
- Production code was not changed. Compiler target was not changed.

## Maven

Tests were written and Maven was not run yet.
