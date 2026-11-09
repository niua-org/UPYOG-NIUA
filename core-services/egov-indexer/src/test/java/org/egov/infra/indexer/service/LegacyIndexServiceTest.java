package org.egov.infra.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.IndexerApplicationRunnerImpl;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.infra.indexer.custom.pt.PTCustomDecorator;
import org.egov.infra.indexer.custom.pt.Property;
import org.egov.infra.indexer.models.AuditDetails;
import org.egov.infra.indexer.models.IndexJob;
import org.egov.infra.indexer.models.IndexJobWrapper;
import org.egov.infra.indexer.producer.IndexerProducer;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.util.ResponseInfoFactory;
import org.egov.infra.indexer.web.contract.APIDetails;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.LegacyIndexRequest;
import org.egov.infra.indexer.web.contract.LegacyIndexResponse;
import org.egov.infra.indexer.web.contract.Mapping;
import org.egov.infra.indexer.web.contract.PaginationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyIndexServiceTest {

	private static final String LEGACY_TOPIC = "legacy-data-topic";
	private static final String PGR_LEGACY_TOPIC = "pgr-service-legacyindex";
	private static final String PT_LEGACY_TOPIC = "pt-property-legacyindex";
	private static final String PERSISTER_CREATE = "indexer.job.create";
	private static final String PERSISTER_UPDATE = "indexer.job.update";
	private static final String ES_HOST = "http://es-host/";

	@Mock
	private IndexerApplicationRunnerImpl runner;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private IndexerUtils indexerUtils;

	@Mock
	private ResponseInfoFactory factory;

	@Mock
	private IndexerProducer indexerProducer;

	@Mock
	private PTCustomDecorator ptCustomDecorator;

	private LegacyIndexService legacyIndexService;

	private RequestInfo requestInfo;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		legacyIndexService = new LegacyIndexService();
		setField(legacyIndexService, "runner", runner);
		setField(legacyIndexService, "restTemplate", restTemplate);
		setField(legacyIndexService, "indexerUtils", indexerUtils);
		setField(legacyIndexService, "factory", factory);
		setField(legacyIndexService, "indexerProducer", indexerProducer);
		setField(legacyIndexService, "ptCustomDecorator", ptCustomDecorator);
		setField(legacyIndexService, "legacyIndexTopic", LEGACY_TOPIC);
		setField(legacyIndexService, "persisterCreate", PERSISTER_CREATE);
		setField(legacyIndexService, "persisterUpdate", PERSISTER_UPDATE);
		setField(legacyIndexService, "defaultPageSizeForLegacyindex", 100);
		setField(legacyIndexService, "esHostUrl", ES_HOST);
		setField(legacyIndexService, "isLegacyVersionES", Boolean.FALSE);
		setField(legacyIndexService, "pgrLegacyTopic", PGR_LEGACY_TOPIC);
		setField(legacyIndexService, "ptLegacyTopic", PT_LEGACY_TOPIC);
		setField(legacyIndexService, "indexThreadPollInterval", 1L);
		setField(legacyIndexService, "serviceHost", "http://service-host/");

		requestInfo = RequestInfo.builder()
				.userInfo(User.builder().uuid("admin").roles(List.of()).build())
				.build();
		objectMapper = new IndexerUtils().getObjectMapper();
	}

	@Test
	void createLegacyindexJob_returnsJobMetadataAndPersistsJob() {
		LegacyIndexRequest request = baseLegacyRequest(LEGACY_TOPIC);
		stubMappingForTopic(LEGACY_TOPIC, "legacy-index", "legacy-type");
		when(factory.createResponseInfoFromRequestInfo(requestInfo, true))
				.thenReturn(ResponseInfo.builder().status("successful").build());
		when(indexerUtils.getAuditDetails("admin", true)).thenReturn(AuditDetails.builder().build());

		LegacyIndexResponse response = legacyIndexService.createLegacyindexJob(request);

		assertThat(response.getJobId()).isNotNull();
		assertThat(response.getUrl()).isEqualTo(ES_HOST + "legacy-index/_search");
		verify(indexerProducer).producer(eq(PERSISTER_CREATE), any(IndexJobWrapper.class));
	}

	@Test
	void createLegacyindexJob_legacyElasticsearch_buildsTypedSearchUrl() {
		setField(legacyIndexService, "isLegacyVersionES", Boolean.TRUE);
		LegacyIndexRequest request = baseLegacyRequest(LEGACY_TOPIC);
		stubMappingForTopic(LEGACY_TOPIC, "legacy-index", "legacy-type");
		when(factory.createResponseInfoFromRequestInfo(requestInfo, true))
				.thenReturn(ResponseInfo.builder().status("successful").build());
		when(indexerUtils.getAuditDetails("admin", true)).thenReturn(AuditDetails.builder().build());

		LegacyIndexResponse response = legacyIndexService.createLegacyindexJob(request);

		assertThat(response.getUrl()).isEqualTo(ES_HOST + "legacy-index/legacy-type/_search");
	}

	@Test
	void beginLegacyIndex_returnsTrue() {
		assertThat(legacyIndexService.beginLegacyIndex(baseLegacyRequest(LEGACY_TOPIC))).isTrue();
	}

	@Test
	void childThreadExecutor_defaultTopic_producesRawResponse() {
		LegacyIndexRequest request = baseLegacyRequest(LEGACY_TOPIC);
		Map<String, Object> response = Map.of("items", List.of(Map.of("id", "1")));

		legacyIndexService.childThreadExecutor(request, objectMapper, response);

		verify(indexerProducer).producer(LEGACY_TOPIC, response);
	}

	@Test
	void childThreadExecutor_pgrTopic_producesServiceResponse() throws Exception {
		LegacyIndexRequest request = baseLegacyRequest(PGR_LEGACY_TOPIC);
		Map<String, Object> response = Map.of("services", List.of(Map.of("serviceRequestId", "SR-1")));

		legacyIndexService.childThreadExecutor(request, objectMapper, response);

		verify(indexerProducer).producer(eq(PGR_LEGACY_TOPIC), any());
	}

	@Test
	void childThreadExecutor_ptTopic_transformsPropertiesBeforeProduce() throws Exception {
		LegacyIndexRequest request = baseLegacyRequest(PT_LEGACY_TOPIC);
		Property property = Property.builder().propertyId("PT-1").build();
		Map<String, Object> response = Map.of(
				"Properties", List.of(Map.of("propertyId", "PT-1")),
				"ResponseInfo", Map.of("status", "successful"));
		when(ptCustomDecorator.transformData(any())).thenReturn(Collections.singletonList(property));

		legacyIndexService.childThreadExecutor(request, objectMapper, response);

		verify(ptCustomDecorator).transformData(any());
		verify(indexerProducer).producer(eq(PT_LEGACY_TOPIC), any());
	}

	@Test
	void beginLegacyIndex_emptyPageCompletesJob() throws Exception {
		LegacyIndexRequest request = baseLegacyRequest(LEGACY_TOPIC);
		request.setJobId("job-legacy-1");
		request.setStartTime(System.currentTimeMillis());
		when(indexerUtils.getObjectMapper()).thenReturn(objectMapper);
		when(indexerUtils.buildPagedUriForLegacyIndex(any(), eq(0), eq(100)))
				.thenReturn("http://service/search?offset=0&size=100");
		when(indexerUtils.getAuditDetails(eq("admin"), eq(false))).thenReturn(AuditDetails.builder().build());
		when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
				.thenReturn(Map.of("items", List.of()));

		legacyIndexService.beginLegacyIndex(request);
		Thread.sleep(250);

		ArgumentCaptor<IndexJobWrapper> captor = ArgumentCaptor.forClass(IndexJobWrapper.class);
		verify(indexerProducer, atLeastOnce()).producer(eq(PERSISTER_UPDATE), captor.capture());
		assertThat(captor.getAllValues()).anyMatch(w -> w.getJob().getJobStatus() == IndexJob.StatusEnum.COMPLETED);
	}

	private LegacyIndexRequest baseLegacyRequest(String legacyIndexTopic) {
		PaginationDetails pagination = PaginationDetails.builder()
				.offsetKey("offset")
				.sizeKey("size")
				.maxPageSize(100)
				.maxRecords(0)
				.build();
		APIDetails apiDetails = APIDetails.builder()
				.uri("/legacy-search")
				.paginationDetails(pagination)
				.responseJsonPath("$.items")
				.build();
		return LegacyIndexRequest.builder()
				.requestInfo(requestInfo)
				.apiDetails(apiDetails)
				.legacyIndexTopic(legacyIndexTopic)
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
