# egov-notification-sms findings

## Covered

`GenericSMSServiceImplTest` (JUnit 5, MockitoExtension, AssertJ) constructs `GenericSMSServiceImpl` directly and mocks the gateway `RestTemplate`. No Spring context and no network.

- `sendSMS_validMessage_submitsMessageToGateway` — valid SMS is posted to the gateway; the request carries the mobile number and message when the gateway body contains `API000`.
- `sendSMS_nullMessage_doesNotCallGateway` — a null message is not sent.
- `sendSMS_emptyMessage_doesNotCallGateway` — an empty message is not sent.
- `sendSMS_gatewayRejects_throwsRuntimeException` — a gateway body without `API000` (`info=rejected`) throws `RuntimeException` with message `rejected`.

## Deferred

Integration tests (Spring context and a live SMS gateway) are deferred. Other providers, blacklist/whitelist checks, GET submission, SSL setup, and transport exceptions are not covered by this unit test.

## Maven result

Homebrew Java 23 rejects `aspectjweaver-1.8.10.jar` (`Invalid CEN header`). Re-downloading the jar did not help.

`mvn -q test` was run with `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` (Java 17.0.10, build 17.0.10+11-LTS-240). Exit code 0.

Surefire: `GenericSMSServiceImplTest` — Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.303 s.
