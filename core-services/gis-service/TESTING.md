# gis-service testing

Unit tests under `src/test/java` use JUnit 5, Mockito, and AssertJ. They do not start Spring and they do not call HTTP.

Run them with:

```bash
mvn test
```

`maven-surefire-plugin` excludes `**/*IT.java`. JaCoCo checks line coverage of `org.egov.gis.service` at a minimum of 0.10 during the `test` phase.

## Deferred integration tests

`GisServiceApplication` is a Spring Boot entry point, but a bootable integration test is not practical in one pass:

- `org.egov.services:tracer:2.9.0-SNAPSHOT` comes from the UPYOG Nexus snapshot repository, and `MainConfiguration` imports `TracerConfiguration`.
- `GisConfiguration` requires `egov.property.host`, `egov.property.search.endpoint`, `egov.asset.host`, and `egov.asset.search.endpoint`.
- `ServiceRequestRepository` posts to property and asset services through `RestTemplate`. No WireMock, MockRestServiceServer, or Testcontainers setup exists in this module.

Add integration tests later with downstream HTTP stubbed and a test profile. Keep the current suite free of `@SpringBootTest`.
