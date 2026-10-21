# egov-notification-sms

Unit tests for `GenericSMSServiceImpl` use JUnit 5, `MockitoExtension`, and AssertJ. They construct the service directly, mock the SMS gateway `RestTemplate`, and do not start Spring or open a network connection.

```bash
mvn -q test
```

Integration tests (Spring context and a live SMS gateway) are deferred.
