# egov-user test findings

`mvn -q test` in `egov-user` with `JAVA_HOME` set to the machine JDK 17.0.10 (`/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`). Compiler source/target was left at Java 17. JUnit comes from Spring Boot 3.2.2 `spring-boot-starter-test` (not pinned to 5.9, which is the Java 8 line).

## Maven result

**BUILD SUCCESS** (exit code 0).

Surefire: 133 tests, 0 failures, 0 errors, 25 skipped (repository tests that need a database). JaCoCo `org.egov.user.domain.service` line coverage 174/636 (27.4%), above the 0.10 minimum. `UserService` itself is 173/399 lines.

## Covered

`UserService` unit tests (Mockito, no network): unique lookup (happy path, blank input, not found, duplicate, auth context skips decrypt, decrypt failure returns the stored user), search (empty, citizen state-tenant plus alternate mobile, missing tenant, decrypt failure), create (persist with state tenant, duplicate username, name required, OTP rejected), citizen username rejected when OTP login is on, password length/pattern/blank, null password encode, null decrypt context, account unlock cooldown, reset of failed logins, failed login below and at the lock threshold, logged-in citizen password update rejected when OTP login is on, address create for an unknown uuid and missing mandatory fields, empty address list, token removal does not touch the repository.

`UserController` Mockito tests: citizen create without login, citizen create with login, create-without-validation (gateway keeps mobile validation; inter-service workaround turns it off; duplicate user propagates), `/_search` sets active when null and applies the default page size when page size is 0, `/v1/_search` leaves active unset, search exception propagates.

## Not covered

`updateWithoutOtpValidation`, `partialUpdate`, non-logged-in password update, `registerWithLogin` / token HTTP call, DigiLocker update, user/address v2 create-update-search, file-store URLs, email-change notification. `TokenService` and the Redis authorization services are in the same package and are almost untested. Other controllers, filters, and persistence are out of this pass. No `@WebMvcTest` or `@SpringBootTest`. Integration tests are deferred in `egov-user/TESTING.md` because the service is large and the context is not booted.

## Bugs not fixed

- `SecurityConfig` permits `/_search`, `/citizen/_create`, `/users/_createnovalidate`, profile and password routes, and the other user endpoints. The order-2 chain has no security matcher, so it matches every request and the order-5 `/api` and `/secure` opaque-token chain does not run. That chain also hardcodes introspection client id `egov-user-client` and secret `egov-user-secret`.
- `removeTokensByUser` only logs. Locking an account after failed logins does not revoke tokens.
- `updateAddress` calls `get(0)` on the address list before the null check, so a missing id throws index-out-of-bounds instead of the intended illegal-argument error.
- `decryptUserWithContext` tries the `"User"` key first, then null, while the log says the opposite.
- `searchUsers` swallows decryption errors and returns stored ciphertext. `searchUsersV2` does not.
- `isAccountUnlockAble` unboxes `accountLocked` and throws if it is null.
