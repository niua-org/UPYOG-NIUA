package org.egov.infra.indexer.service;

import org.egov.IndexerApplicationRunnerImpl;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.infra.indexer.bulkindexer.BulkIndexer;
import org.egov.infra.indexer.models.AuditDetails;
import org.egov.infra.indexer.models.IndexJobWrapper;
import org.egov.infra.indexer.producer.IndexerProducer;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.util.ResponseInfoFactory;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.Mapping;
import org.egov.infra.indexer.web.contract.ReindexRequest;
import org.egov.infra.indexer.web.contract.ReindexResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexServiceTest {

	private static final String REINDEX_TOPIC = "egov.core.reindex";
	private static final String PERSISTER_CREATE = "indexer.job.create";
	private static final String PERSISTER_UPDATE = "indexer.job.update";
	private static final String ES_HOST = "http://es-host/";

	@Mock
	private BulkIndexer bulkIndexer;

	@Mock
	private IndexerApplicationRunnerImpl runner;

	@Mock
	private IndexerUtils indexerUtils;

	@Mock
	private ResponseInfoFactory factory;

	@Mock
	private IndexerProducer indexerProducer;

	private ReindexService reindexService;

	private RequestInfo requestInfo;

	@BeforeEach
	void setUp() {
		reindexService = new ReindexService();
		setField(reindexService, "bulkIndexer", bulkIndexer);
		setField(reindexService, "runner", runner);
		setField(reindexService, "indexerUtils", indexerUtils);
		setField(reindexService, "factory", factory);
		setField(reindexService, "indexerProducer", indexerProducer);
		setField(reindexService, "reindexTopic", REINDEX_TOPIC);
		setField(reindexService, "persisterCreate", PERSISTER_CREATE);
		setField(reindexService, "persisterUpdate", PERSISTER_UPDATE);
		setField(reindexService, "defaultPageSizeForReindex", 100);
		setField(reindexService, "esHostUrl", ES_HOST);
		setField(reindexService, "isLegacyVersionES", Boolean.FALSE);
		setField(reindexService, "indexThreadPollInterval", 1L);

		requestInfo = RequestInfo.builder()
				.userInfo(User.builder().uuid("admin").roles(List.of()).build())
				.build();
	}

	@Test
	void createReindexJob_returnsJobMetadataAndPublishesMessages() {
		ReindexRequest request = baseReindexRequest();
		stubMappingForTopic(request.getReindexTopic(), "new-index", "new-type");
		when(indexerUtils.getESSearchURL(request)).thenReturn(ES_HOST + "citizen-index/citizen/_search");
		when(bulkIndexer.getESResponse(anyString(), isNull(), isNull()))
				.thenReturn(Map.of("hits", Map.of("total", 42)));
		when(indexerUtils.fetchEstimatedTime(42)).thenReturn("1.05secs");
		when(factory.createResponseInfoFromRequestInfo(requestInfo, true))
				.thenReturn(ResponseInfo.builder().status("successful").build());
		when(indexerUtils.getAuditDetails("admin", true)).thenReturn(AuditDetails.builder().build());

		ReindexResponse response = reindexService.createReindexJob(request);

		assertThat(response.getJobId()).isNotNull();
		assertThat(response.getTotalRecordsToBeIndexed()).isEqualTo(42);
		assertThat(response.getUrl()).isEqualTo(ES_HOST + "new-index/_search");
		verify(indexerProducer).producer(REINDEX_TOPIC, request);
		verify(indexerProducer).producer(eq(PERSISTER_CREATE), any(IndexJobWrapper.class));
	}

	@Test
	void createReindexJob_legacyElasticsearch_buildsTypedSearchUrl() {
		setField(reindexService, "isLegacyVersionES", Boolean.TRUE);
		ReindexRequest request = baseReindexRequest();
		stubMappingForTopic(request.getReindexTopic(), "new-index", "new-type");
		when(indexerUtils.getESSearchURL(request)).thenReturn(ES_HOST + "citizen-index/citizen/_search");
		when(bulkIndexer.getESResponse(anyString(), isNull(), isNull()))
				.thenReturn(Map.of("hits", Map.of("total", 1)));
		when(indexerUtils.fetchEstimatedTime(1)).thenReturn("less than a second");
		when(factory.createResponseInfoFromRequestInfo(requestInfo, true))
				.thenReturn(ResponseInfo.builder().status("successful").build());
		when(indexerUtils.getAuditDetails("admin", true)).thenReturn(AuditDetails.builder().build());

		ReindexResponse response = reindexService.createReindexJob(request);

		assertThat(response.getUrl()).isEqualTo(ES_HOST + "new-index/new-type/_search");
	}

	@Test
	void beginReindex_returnsTrue() {
		assertThat(reindexService.beginReindex(baseReindexRequest())).isTrue();
	}

	@Test
	void isHitAnInvalidRecord_detectsNullAndPaginationOnlyRecords() {
		when(indexerUtils.getObjectMapper()).thenReturn(new IndexerUtils().getObjectMapper());

		assertThat(reindexService.isHitAnInvalidRecord(null)).isTrue();
		assertThat(reindexService.isHitAnInvalidRecord("null")).isTrue();
		assertThat(reindexService.isHitAnInvalidRecord(Map.of("from", 0, "size", 100))).isTrue();
		assertThat(reindexService.isHitAnInvalidRecord(Map.of("id", "1", "tenantId", "t1"))).isFalse();
	}

	@Test
	void increaseMaxResultWindow_settingsOk_logsSuccess() {
		ReindexRequest request = baseReindexRequest();
		Object body = Map.of("index", Map.of("max_result_window", 50100L));
		when(indexerUtils.getESSettingsURL(request)).thenReturn(ES_HOST + "citizen-index/_settings");
		when(indexerUtils.getESSettingsBody(100)).thenReturn(body);
		when(bulkIndexer.getESResponse(anyString(), eq(body), eq("PUT"))).thenReturn("OK");

		reindexService.increaseMaxResultWindow(request, 100);

		verify(bulkIndexer).getESResponse(ES_HOST + "citizen-index/_settings", body, "PUT");
	}

	@Test
	void childThreadExecutor_publishesHitsToReindexTopic() throws Exception {
		ReindexRequest request = baseReindexRequest();
		request.setReindexTopic("reindex-data-topic");
		Map<String, Object> payload = Map.of("hits", List.of(Map.of("id", "1")));

		reindexService.childThreadExecutor(request, new IndexerUtils().getObjectMapper(), payload, 1);

		Thread.sleep(150);
		verify(indexerProducer).producer(eq("reindex-data-topic"), eq(payload));
	}

	@Test
	void beginReindex_indexThreadCompletesWhenSearchReturnsNoHits() throws Exception {
		ReindexRequest request = baseReindexRequest();
		request.setJobId("job-1");
		request.setStartTime(System.currentTimeMillis());
		request.setTotalRecords(1);
		String searchUri = ES_HOST + "citizen-index/citizen/_search";
		when(indexerUtils.getESSearchURL(request)).thenReturn(searchUri);
		when(indexerUtils.getESSettingsURL(request)).thenReturn(ES_HOST + "citizen-index/_settings");
		when(indexerUtils.getESSettingsBody(anyInt())).thenReturn(Map.of());
		when(indexerUtils.getESSearchBody(anyInt(), anyInt())).thenReturn(Map.of("from", 0, "size", 100));
		when(indexerUtils.getObjectMapper()).thenReturn(new IndexerUtils().getObjectMapper());
		when(indexerUtils.getAuditDetails(eq("admin"), eq(false))).thenReturn(AuditDetails.builder().build());
		when(bulkIndexer.getESResponse(eq(ES_HOST + "citizen-index/_settings"), any(), eq("PUT"))).thenReturn("OK");
		when(bulkIndexer.getESResponse(eq(searchUri), any(), eq("POST"))).thenReturn(Map.of("hits", Map.of("hits", List.of())));

		reindexService.beginReindex(request);
		Thread.sleep(250);

		ArgumentCaptor<IndexJobWrapper> captor = ArgumentCaptor.forClass(IndexJobWrapper.class);
		verify(indexerProducer, atLeastOnce()).producer(eq(PERSISTER_UPDATE), captor.capture());
		assertThat(captor.getAllValues()).anyMatch(w -> w.getJob().getJobStatus().name().equals("COMPLETED"));
	}

	private ReindexRequest baseReindexRequest() {
		return ReindexRequest.builder()
				.requestInfo(requestInfo)
				.index("citizen-index")
				.type("citizen")
				.reindexTopic(REINDEX_TOPIC)
				.tenantId("pg.citya")
				.build();
	}

	private void stubMappingForTopic(String topic, String indexName, String indexType) {
		Index index = new Index();
		index.setName(indexName);
		index.setType(indexType);
		Mapping mapping = new Mapping();
		mapping.setIndexes(List.of(index));
		ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
		maps.put(topic, mapping);
		when(runner.getMappingMaps()).thenReturn(maps);
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
