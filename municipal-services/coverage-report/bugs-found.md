# Bugs observed during test authoring (production unchanged)

| Module | Area | Observation | Test reference |
|--------|------|-------------|----------------|
| pgr-services | `ServiceRequestValidator.validateCreate` | Null/incomplete `service` can NPE before aggregated `CustomException` | Existing `ServiceRequestValidatorTest` |
| pgr-services | `RequestsApiController` | NPE if `pgrService.create` returns null | Prior strategy report |
| street-vending / tp-services | `GlobalExceptionHandler` | Standalone MockMvc returns **500** for some validation paths where API docs imply **400** | Legacy controller tests |
| property-services | `PropertyValidator.validateCreateRequest` | `owners.remove(null)` on immutable lists throws `UnsupportedOperationException` instead of validation error | Removed duplicate-owner test; documented in `untestable.md` |
| property-services | `PropertyValidator.validateMobileNumber` | NPE when `ownershipCategory` is null (`ownershipCategory.contains(...)`) | `PropertyValidatorTest` sets ownershipCategory to reach creation-reason validation |
| sw-services | `SewerageServiceImpl.validateDisconnectionRequest` / `validateReconnectionRequest` | Assumes `getAllSewerageApplications` returns a non-empty list before `previousConnectionsList.get(0)` | `SewerageServiceImplTest` covers status guards only; empty history not exercised |
