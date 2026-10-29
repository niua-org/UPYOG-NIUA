# Bugs found while adding tests

## gateway — `ExceptionUtils.raiseErrorFilterException`

**Severity:** High (incorrect HTTP status to clients)

**Behavior:** Spring Framework 6 `HttpClientErrorException.create(...)` yields nested types whose simple class name is `Forbidden`, `BadRequest`, etc., not `HttpClientErrorException`. `ExceptionUtils` branches on `exceptionName.equalsIgnoreCase("HttpClientErrorException")` (see `ExceptionUtils.java` ~55–72), so legitimate 4xx responses from downstream services are handled as generic failures and the gateway responds with **500 Internal Server Error** instead of propagating the client error status.

**Reproduction:** `ExceptionUtilsTest.raiseErrorFilterException_httpClientError_usesExceptionStatus` (expects 500 today; should expect 403 once fixed).

**Suggested fix:** Use `instanceof HttpClientErrorException` / `RestClientResponseException` and map `getStatusCode()` without casting to deprecated `HttpStatus` only.
