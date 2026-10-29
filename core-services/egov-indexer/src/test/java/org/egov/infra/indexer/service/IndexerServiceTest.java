package org.egov.infra.indexer.service;

import com.github.zafarkhaja.semver.Version;
import org.egov.IndexerApplicationRunnerImpl;
import org.egov.infra.indexer.bulkindexer.BulkIndexer;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.CustomJsonMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.Mapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexerServiceTest {

	private static final String TOPIC = "save-service";
	private static final String KAFKA_JSON = "{\"RequestInfo\":{\"ver\":\"1.0.0\"},\"id\":\"1\"}";
	private static final String BULK_JSON = "{ \"index\" : { \"_id\" : \"1\" } }\n{\"id\":\"1\"}\n";
	private static final String ES_HOST = "http://es-host/";

	@Mock
	private BulkIndexer bulkIndexer;

	@Mock
	private IndexerApplicationRunnerImpl runner;

	@Mock
	private DataTransformationService dataTransformationService;

	@Mock
	private IndexerUtils utils;

	private IndexerService indexerService;

	@BeforeEach
	void setUp() {
		indexerService = new IndexerService();
		setField(indexerService, "bulkIndexer", bulkIndexer);
		setField(indexerService, "runner", runner);
		setField(indexerService, "dataTransformationService", dataTransformationService);
		setField(indexerService, "utils", utils);
		setField(indexerService, "esHostUrl", ES_HOST);
		setField(indexerService, "isLegacyVersionES", Boolean.FALSE);
	}

	@Test
	void esIndexer_matchingTopic_postsBulkJson() throws Exception {
		Index index = stubApplicableIndex(TOPIC, KAFKA_JSON, BULK_JSON);

		indexerService.esIndexer(TOPIC, KAFKA_JSON);

		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(bulkIndexer).indexJsonOntoES(urlCaptor.capture(), bodyCaptor.capture(), eq(index));
		assertThat(urlCaptor.getValue()).isEqualTo(ES_HOST + "citizen-index/_bulk");
		assertThat(bodyCaptor.getValue()).isEqualTo(BULK_JSON);
	}

	@Test
	void validateAndIndex_nullOrEmptyJson_skipsIndexing() throws Exception {
		Index index = mock(Index.class);

		indexerService.validateAndIndex(null, ES_HOST + "citizen-index/_bulk", index);
		indexerService.validateAndIndex("", ES_HOST + "citizen-index/_bulk", index);

		verify(bulkIndexer, never()).indexJsonOntoES(anyString(), anyString(), any());
	}

	@Test
	void esIndexer_bulkIndexerThrows_swallowsException() throws Exception {
		Index index = stubApplicableIndex(TOPIC, KAFKA_JSON, BULK_JSON);
		doThrow(new RuntimeException("elasticsearch down"))
				.when(bulkIndexer)
				.indexJsonOntoES(anyString(), anyString(), eq(index));

		assertThatCode(() -> indexerService.esIndexer(TOPIC, KAFKA_JSON)).doesNotThrowAnyException();
		verify(bulkIndexer).indexJsonOntoES(anyString(), eq(BULK_JSON), eq(index));
	}

	@Test
	void esIndexer_unknownTopic_doesNotIndex() throws Exception {
		Version semVer = Version.valueOf("1.0.0");
		when(utils.getSemVer(anyString())).thenReturn(semVer);
		Mapping mapping = mock(Mapping.class);
		when(mapping.getTopic()).thenReturn("other-topic");
		ConcurrentHashMap<String, List<Mapping>> versionMap = new ConcurrentHashMap<>();
		versionMap.put(semVer.getNormalVersion(), Collections.singletonList(mapping));
		when(runner.getVersionMap()).thenReturn(versionMap);

		indexerService.esIndexer(TOPIC, KAFKA_JSON);

		verify(bulkIndexer, never()).indexJsonOntoES(anyString(), anyString(), any());
		verify(dataTransformationService, never()).buildJsonForIndex(any(), anyString(), anyBoolean(), anyBoolean());
	}

	@Test
	void indexWithESId_nonLegacyHost_postsTypedIndexUrl() throws Exception {
		Index index = mock(Index.class);
		when(index.getName()).thenReturn("citizen-index");
		when(index.getType()).thenReturn("citizen");

		indexerService.indexWithESId(index, "{\"id\":1}");

		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
		verify(bulkIndexer).indexJsonOntoES(urlCaptor.capture(), eq("{\"id\":1}"), eq(index));
		assertThat(urlCaptor.getValue()).isEqualTo(ES_HOST + "citizen-index/citizen/_index");
	}

	@Test
	void indexProccessor_legacyConfigKey_skipsIndexing() throws Exception {
		Index index = mock(Index.class);
		when(index.getName()).thenReturn("citizen-index");
		when(index.getCustomJsonMapping()).thenReturn(null);
		when(dataTransformationService.buildJsonForIndex(index, KAFKA_JSON, false, false)).thenReturn(BULK_JSON);

		indexerService.indexProccessor(index, Mapping.ConfigKeyEnum.LEGACYINDEX, KAFKA_JSON, false);

		verify(bulkIndexer, never()).indexJsonOntoES(anyString(), anyString(), any());
	}

	@Test
	void indexProccessor_legacyElasticsearch_postsTypedBulkUrl() throws Exception {
		setField(indexerService, "isLegacyVersionES", Boolean.TRUE);
		Index index = mock(Index.class);
		when(index.getName()).thenReturn("citizen-index");
		when(index.getType()).thenReturn("citizen");
		when(index.getCustomJsonMapping()).thenReturn(CustomJsonMapping.builder().build());
		when(dataTransformationService.buildJsonForIndex(index, KAFKA_JSON, false, true)).thenReturn(BULK_JSON);

		indexerService.indexProccessor(index, nonLegacyConfigKey(), KAFKA_JSON, false);

		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
		verify(bulkIndexer).indexJsonOntoES(urlCaptor.capture(), eq(BULK_JSON), eq(index));
		assertThat(urlCaptor.getValue()).isEqualTo(ES_HOST + "citizen-index/citizen/_bulk");
	}

	private Index stubApplicableIndex(String topic, String kafkaJson, String bulkJson) throws Exception {
		Version semVer = Version.valueOf("1.0.0");
		when(utils.getSemVer(anyString())).thenReturn(semVer);

		Index index = mock(Index.class);
		when(index.getIsBulk()).thenReturn(Boolean.TRUE);
		when(index.getName()).thenReturn("citizen-index");
		when(index.getCustomJsonMapping()).thenReturn(null);

		Mapping mapping = mock(Mapping.class);
		when(mapping.getTopic()).thenReturn(topic);
		when(mapping.getIndexes()).thenReturn(Collections.singletonList(index));
		when(mapping.getConfigKey()).thenReturn(nonLegacyConfigKey());

		ConcurrentHashMap<String, List<Mapping>> versionMap = new ConcurrentHashMap<>();
		versionMap.put(semVer.getNormalVersion(), Collections.singletonList(mapping));
		when(runner.getVersionMap()).thenReturn(versionMap);
		when(dataTransformationService.buildJsonForIndex(index, kafkaJson, true, false)).thenReturn(bulkJson);
		return index;
	}

	private static Mapping.ConfigKeyEnum nonLegacyConfigKey() {
		for (Mapping.ConfigKeyEnum value : Mapping.ConfigKeyEnum.values()) {
			if (value != Mapping.ConfigKeyEnum.LEGACYINDEX) {
				return value;
			}
		}
		throw new IllegalStateException("Mapping.ConfigKeyEnum has no value other than LEGACYINDEX");
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
