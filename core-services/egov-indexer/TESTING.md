# egov-indexer tests

Unit tests live in `src/test/java` and use JUnit 5, Mockito, and AssertJ. They do not start a Spring context and do not open network connections. Repositories and the Elasticsearch bulk client are mocked. `ArgumentCaptor` checks the URL and document passed to `BulkIndexer`.

## Run

From `egov-indexer`:

```bash
mvn -q test
```

Surefire excludes classes named `*IT`. JaCoCo checks line coverage of `org.egov.infra.indexer.service` with a minimum of 0.10 when the check goal runs.

## IndexerServiceTest

| Test | Behaviour |
| --- | --- |
| `esIndexer_matchingTopic_postsBulkJson` | A versioned mapping matches the topic and the bulk document is posted. |
| `validateAndIndex_nullOrEmptyJson_skipsIndexing` | Null or empty JSON is not sent to Elasticsearch. |
| `esIndexer_bulkIndexerThrows_swallowsException` | A failure from the bulk client is caught inside `esIndexer`. |
| `esIndexer_unknownTopic_doesNotIndex` | A mapping for a different topic does not index. |
| `indexWithESId_nonLegacyHost_postsTypedIndexUrl` | A non-bulk document is posted to the typed `_index` URL. |
| `indexProccessor_legacyConfigKey_skipsIndexing` | `LEGACYINDEX` does not call the bulk client. |
| `indexProccessor_legacyElasticsearch_postsTypedBulkUrl` | Legacy Elasticsearch builds `/{index}/{type}/_bulk`. |

`BulkIndexer`, `IndexerApplicationRunnerImpl`, `DataTransformationService`, and `IndexerUtils` are mocked. `@Value` fields used by these tests are set directly on the service instance.
