# Classes difficult to unit-test without refactor

| Module | Class | Reason | Suggested minimal refactor |
|--------|--------|--------|----------------------------|
| finance-collections-voucher-consumer | `EgfKafkaListener` (remainder) | Very large `process` method, static time, deep MDMS/ERP integration | Extract voucher create/cancel handlers; inject clock and strategy beans |
| egf-master | `*Repository` JDBC / ES | Direct SQL/transport clients constructed inline | Already partially tested via service mocks; repository IT needs Testcontainers (skipped in CI here) |
| dashboard-ingest | `JSONUtil` (instance methods) | `@Autowired` ES repository and Spring `@Service` static helpers mixed | Split static helpers from Spring bean; inject `ElasticSearchRepository` in tests |
| dashboard-data-extractor | Row mappers with JDBC | Require live PG result sets for full branch coverage | `@DataJpaTest` or Testcontainers (module IT dependency on `dashboard-data-engine` artifact) |
| egov-hrms | Kafka consumers / producers | `@KafkaListener` end-to-end paths | Extract message handlers; unit-test handlers with mocked templates |
| billing-service | `BulkBillGenerationConsumer` | Kafka + async bill pipeline | Extract `processMessage` core into testable service |
| dashboard-analytics | `AmazonS3ClientServiceImpl` | AWS SDK static builders | Wrap S3 client factory interface |

Pure POJOs, DTOs, and config property holders are excluded from ranking in `jacoco-analyze.py` (`SKIP_PKG_FRAGMENTS`).
