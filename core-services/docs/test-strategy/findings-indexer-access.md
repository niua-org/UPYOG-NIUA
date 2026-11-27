# Indexer and access-control test findings

Production code was not changed. Tests characterize current behaviour.

## egov-indexer

Primary class: `org.egov.infra.indexer.service.IndexerService`.

### Defects observed

1. **Indexing failures are swallowed.** `esIndexer` catches `Exception` around `indexProccessor` and only logs it. Callers cannot tell that Elasticsearch rejected the document. `esIndexer_bulkIndexerThrows_swallowsException` locks this in.

2. **Missing version mappings throw.** `getApplicableMapping` iterates `versionMap.get(semVer.getNormalVersion())` with no null check. An unknown version throws `NullPointerException` outside the `try` in `esIndexer`, so it propagates instead of taking the "no mappings" log branch.

3. **Null config key.** `indexProccessor` calls `configkey.equals(Mapping.ConfigKeyEnum.LEGACYINDEX)`. A null config key throws `NullPointerException`, which `esIndexer` then swallows (see defect 1).

4. **Null legacy flag.** `if (this.isLegacyVersionES)` unboxes the `Boolean` from `egov.infra.indexer.legacy.version`. A missing property throws `NullPointerException`.

5. **Inconsistent Elasticsearch URLs.** Non-legacy bulk URLs are `{host}{index}/_bulk` (no type). Non-legacy single-document URLs are `{host}{index}/{type}/_index`. Legacy bulk URLs include the type (`{host}{index}/{type}/_bulk`) but legacy `indexWithESId` omits it (`{host}{index}/_index`). The type segment is on opposite branches.

6. **Host is concatenated as-is.** `esHostUrl` and the index name are appended with no slash inserted between them. `egov.infra.indexer.host` must already end with `/`.

7. **Names containing `collection` or `payment` skip indexing.** `indexProccessor` does not call `validateAndIndex` when the index name contains either substring, and it does not log that the document was skipped.

8. **Empty payloads are silent.** `validateAndIndex` logs and returns when the JSON is null or empty. `StringUtils.isEmpty` does not treat whitespace-only JSON as empty, so a blank payload still goes to Elasticsearch.

9. **Method name typo.** The processor method is spelled `indexProccessor`.

### Maven

Command (cwd `egov-indexer`): `mvn -q test`

Result: exit code 1. Tests were not executed (`IndexerServiceTest` was not compiled; no Surefire report). `mvn -q` did not print a `BUILD SUCCESS` or `BUILD FAILURE` line.

```
[ERROR] COMPILATION ERROR :
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project egov-indexer: Compilation failure: Compilation failure:
[ERROR] -> [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
```

100 main-source diagnostics, all before the test compile: 52 in `LegacyIndexService.java`, 48 in `ReindexService.java`. 99 are `cannot find symbol` (Lombok getters, `builder()`, and `log`). One is `LegacyIndexService.java:[216,71] non-static method <T>read(java.lang.Object,com.jayway.jsonpath.Configuration) cannot be referenced from a static context`. First diagnostic: `ReindexService.java:[99,61] cannot find symbol` `getReindexTopic()` on `ReindexRequest`. The added test was not changed; this is a main compile failure.

## individual

Not started.

## egov-accesscontrol

Not started.
