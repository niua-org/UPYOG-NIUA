# gis-service findings

## Maven result

`mvn -q test` in `gis-service` on 2026-10-21 exited 0. Quiet mode did not print the reactor banner. Surefire reports from that run:

| Test class | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.gis.adapters.PropertyServiceAdapterTest` | 17 | 0 | 0 | 0 |
| `org.egov.gis.web.controller.GisControllerTest` | 12 | 0 | 0 | 0 |
| `org.egov.gis.service.ServiceRegistryTest` | 7 | 0 | 0 | 0 |
| `org.egov.gis.service.GisServiceTest` | 5 | 0 | 0 | 0 |
| **Total** | **41** | **0** | **0** | **0** |

BUILD SUCCESS. No test class failed to compile.

JaCoCo 0.8.11 wrote this to the log and did not fail the build:

`java.lang.instrument.IllegalClassFormatException: Error while instrumenting sun/util/resources/cldr/provider/CLDRLocaleDataMetaInfo with JaCoCo 0.8.11.202310140853/f33756c` caused by `java.lang.IllegalArgumentException: Unsupported class file major version 67`.

## Covered classes

JaCoCo line coverage from this run (covered/total):

| Class | Lines |
| --- | ---: |
| `org.egov.gis.web.controller.GisController` | 25/25 |
| `org.egov.gis.service.ServiceRegistry` | 14/14 |
| `org.egov.gis.service.GisService` | 32/37 |
| `org.egov.gis.adapters.PropertyServiceAdapter` | 105/113 |
| `org.egov.gis.models.GisRequest` | 10/12 |
| `org.egov.gis.models.GisResponse` | 6/9 |
| `org.egov.gis.models.Entity` | 7/16 |
| `org.egov.gis.repository.ServiceRequestRepository` | 1/15 |

`org.egov.gis.service` is above the 0.10 line minimum (`GisService` 32/37 and `ServiceRegistry` 14/14).

No line coverage: `AssetServiceAdapter`, `GisServiceApplication`, `MainConfiguration`, `SwaggerConfiguration`, `GisConfiguration`, `GeometryType`.

## Deferred integration tests

From `gis-service/TESTING.md`: a bootable integration test is not practical in one pass.

- `org.egov.services:tracer:2.9.0-SNAPSHOT` comes from the UPYOG Nexus snapshot repository, and `MainConfiguration` imports `TracerConfiguration`.
- `GisConfiguration` requires `egov.property.host`, `egov.property.search.endpoint`, `egov.asset.host`, and `egov.asset.search.endpoint`.
- `ServiceRequestRepository` posts to property and asset services through `RestTemplate`. No WireMock, MockRestServiceServer, or Testcontainers setup exists in this module.

Integration tests should be added later with downstream HTTP stubbed and a test profile. The current suite stays free of `@SpringBootTest`.
