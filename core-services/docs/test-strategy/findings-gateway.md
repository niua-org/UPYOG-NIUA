# Gateway test findings

Recorded 2026-10-21. No production code, POM, or test classes were changed in this pass. No git commit.

## Maven result

Command (once), working directory `gateway/`:

```bash
mvn -q test
```

- Exit code: **1**
- Elapsed: about 37 seconds
- Phase: `maven-compiler-plugin:3.11.0:compile` (`default-compile`) failed. The surefire test phase did not start. There is no `Tests run` line and no JaCoCo report from this run.
- The quiet log prints **71** `cannot find symbol` errors, then repeats the same list under `Failed to execute goal` (142 occurrences of that phrase).

The failures are generated-symbol gaps, not assertion failures:

- `log` is missing on many `@Slf4j` classes (annotation processing did not run).
- `builder()` is missing on model types such as `RequestCaptureCriteria`, `PreHookFilterRequest`, `AuthorizationRequest`, `EventLogRequest`, and `CustomAsyncRequest`.
- Getters are missing on `ApplicationProperties` (`getOpenEndpointsWhitelist`, `getMixedModeEndpointsWhitelist`, `getAuthServiceHost`, `getAuthUri`, `getUserSearchURI`, `getAuthorizationUrl`, `getDefaultReplenishRate`, `getDefaultBurstCapacity`) and on request models (`getRoles`, `getUri`, `isCaptureInputBody`, `isCaptureOutputBody`, `isCaptureOutputBodyOnlyForError`).
- `AuthorizationRequestWrapper` is constructed with two arguments; the compiler only sees a no-arg constructor.

`AuthCheckFilterHelper` itself fails to compile on the missing `log` field, so `AuthCheckFilterHelperTest` never executed.

## Test classes on disk

Nine `*Test.java` classes. No `*IT.java` classes.

| Class | Package |
| --- | --- |
| `CommonUtilsTest` | `com.example.gateway.utils` |
| `UserUtilsTest` | `com.example.gateway.utils` |
| `ExceptionUtilsTest` | `com.example.gateway.utils` |
| `EventLoggerUtilTest` | `com.example.gateway.utils` |
| `URLProviderTest` | `com.example.gateway.utils` |
| `UpyogMcpGatewaySupportTest` | `com.example.gateway.utils` |
| `AuthCheckFilterHelperTest` | `com.example.gateway.filters.pre.helpers` |
| `AuthPreCheckFilterHelperTest` | `com.example.gateway.filters.pre.helpers` |
| `ProducerTest` | `com.example.gateway.producer` |

## Not covered

This pass did not open those test bodies and did not execute them, so branch coverage inside the nine classes is unverified.

From `CommonUtils` and `AuthCheckFilterHelper` (read before this run), behaviour that this Maven invocation did not exercise:

- `CommonUtils`: null or empty content type; POST, PUT, and PATCH versus other methods; JSON and form bodies; empty tenant set; deepest tenant and ties; `RequestInfo` removed in Pascal and camel case; `tenantId` as array or string; the literal `"null"` skipped; query-param fallback; comma-separated tenant ids; default tenant `pg` when the id is absent; parse failure thrown as `CustomException` with `401 UNAUTHORIZED`.
- `AuthCheckFilterHelper`: missing token (null body and no `Auth-Token` header, or a body whose `RequestInfo` has no token); invalid token when `UserUtils.getUser` throws; valid token copying `User` onto `RequestInfo`. There is no wrong-role rejection branch. Empty roles are only logged. Role mismatch is not enforced in this helper.

No unit-test class was found for the types that failed to compile around those two helpers, including `RbacFilterHelper`, `RbacPreCheckFilter`, `RbacPreCheckFilterHelper`, `RbacPreCheckFormDataFilterHelper`, `PreHookFilterHelper`, `CorrelationIdFilterHelper`, `CorrIdFormDataFilterHelper`, `UpyogMcpGatewayAuthHelper`, `CustomAsyncFilter`, `AuthPreCheckFilter`, `GatewayApplication`, `ApplicationProperties`, and the model types whose builders and accessors the compiler could not see.

`gateway/pom.xml` still has JaCoCo `prepare-agent` and `report` only. It does not set a 0.10 line-coverage minimum on `com.example.gateway.utils` or `com.example.gateway.filters`, and Surefire does not exclude `*IT`. Those POM changes were not applied. Integration tests remain deferred.
