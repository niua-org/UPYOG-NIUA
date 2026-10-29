# Classes difficult to unit-test without refactors

| Module | Class | Issue | Minimal refactor suggestion |
| --- | --- | --- | --- |
| mdms-v2 | `MdmsDataEnricher.enrichCreateRequest` (partial) | Calls static `UUIDEnrichmentUtil`, `AuditDetailsEnrichmentUtil`, `CompositeUniqueIdentifierGenerationUtil` | Inject enrichment helpers or wrap statics in a small bean |
| gateway | `ExceptionUtils` | Static methods, string-based exception dispatch | Extract instance-based mapper; use `instanceof` for HTTP client errors |
| egov-indexer | `IndexerApplicationRunnerImpl` | Loads YAML mappings at startup, heavy Spring lifecycle | Keep integration tests; unit-test extracted mapping parser |
| Various | Kafka `@KafkaListener` consumers | Thread/async lifecycle | Test listener methods directly with mocked dependencies (pattern used in `national-dashboard-ingest`) |

| egov-notification-sms | Entire module | `mvn verify` fails: unresolved `org.egov.services:tracer:2.9.0-SNAPSHOT` (and related Nexus SNAPSHOT deps) in this environment | Publish tracer to Nexus or pin a release version in `pom.xml` (production change — not done here) |

No silent production refactors were made for these items in this pass.
