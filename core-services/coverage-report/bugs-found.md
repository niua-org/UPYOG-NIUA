# Bugs found while adding tests

## gateway — `ExceptionUtils.raiseErrorFilterException`

**Severity:** High (incorrect HTTP status to clients)

**Behavior:** Spring Framework 6 `HttpClientErrorException.create(...)` yields nested types whose simple class name is `Forbidden`, `BadRequest`, etc., not `HttpClientErrorException`. `ExceptionUtils` branches on `exceptionName.equalsIgnoreCase("HttpClientErrorException")` (see `ExceptionUtils.java` ~55–72), so legitimate 4xx responses from downstream services are handled as generic failures and the gateway responds with **500 Internal Server Error** instead of propagating the client error status.

**Reproduction:** `ExceptionUtilsTest.raiseErrorFilterException_httpClientError_usesExceptionStatus` (expects 500 today; should expect 403 once fixed).

**Suggested fix:** Use `instanceof HttpClientErrorException` / `RestClientResponseException` and map `getStatusCode()` without casting to deprecated `HttpStatus` only.

## egov-user — `UserRequest.toDomain` vs `UserRequestV2.toDomain` (identity verification)

**Severity:** Medium (inconsistent API contract / possible privilege escalation via API payload)

**Behavior:** `UserRequest.toDomain` copies `isIdentityVerified`, `identityVerifiedDate`, and `identityVerifiedSource` from the HTTP payload when set (create and update). `UserRequestV2.toDomain` forces `isIdentityVerified=false` on create and ignores payload flags on update. A client calling legacy V1 contracts can set verified identity without going through Aadhaar/profile verification flows.

**Reproduction:** `IdentityVerificationProtectionTest.testContractToDomain_DoesNotPassUntrustedIdentityVerification` (updated to document current V1 behavior).

**Suggested fix:** Align V1 with V2 by stripping untrusted identity fields in `UserRequest.toDomain`, or enforce verification only in `ProfileController` / service layer for all entry points.
