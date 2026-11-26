# egov-workflow-v2 tests

Unit tests use JUnit 5, Mockito (`MockitoExtension`), and AssertJ. They do not start Spring Boot and they do not call the network.

`mvn test` runs Surefire on `*Test` and skips `*IT`. JaCoCo writes a line-coverage report for that run. The enforced gate is 10% line coverage on `org.egov.wf.service`, and it runs only on `mvn verify` (with Failsafe `*IT`). The documented aim is 80% line and 70% branch coverage; those targets are not enforced.

Integration tests are deferred because this service is not bootable here without Postgres, Kafka, and MDMS.
