package org.egov.infra.indexer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.infra.indexer.service.IndexerService;
import org.egov.infra.indexer.service.LegacyIndexService;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.APIDetails;
import org.egov.infra.indexer.web.contract.LegacyIndexRequest;
import org.egov.infra.indexer.web.contract.PaginationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegacyIndexMessageListenerTest {

	private static final String LEGACY_TOPIC = "egov.core.legacyindex";

	@Mock
	private LegacyIndexService legacyIndexService;

	@Mock
	private IndexerService indexerService;

	private LegacyIndexMessageListener listener;

	@BeforeEach
	void setUp() throws Exception {
		listener = new LegacyIndexMessageListener();
		IndexerUtils indexerUtils = new IndexerUtils();
		setField(listener, "indexerUtils", indexerUtils);
		setField(listener, "legacyIndexService", legacyIndexService);
		setField(listener, "indexerService", indexerService);
		setField(listener, "legacyIndexTopic", LEGACY_TOPIC);
	}

	@Test
	void onMessage_legacyTopic_delegatesToLegacyIndexService() throws Exception {
		LegacyIndexRequest request = sampleLegacyRequest();
		String payload = new ObjectMapper().writeValueAsString(request);
		ConsumerRecord<String, String> record = new ConsumerRecord<>(LEGACY_TOPIC, 0, 0L, "key", payload);

		listener.onMessage(record);

		verify(legacyIndexService).beginLegacyIndex(any(LegacyIndexRequest.class));
		verify(indexerService, never()).esIndexer(any(), any());
	}

	@Test
	void onMessage_otherTopic_delegatesToIndexerService() throws Exception {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("save-service", 0, 0L, "key", "{\"id\":1}");

		listener.onMessage(record);

		verify(indexerService).esIndexer("save-service", "{\"id\":1}");
		verify(legacyIndexService, never()).beginLegacyIndex(any());
	}

	@Test
	void onMessage_invalidLegacyPayload_doesNotPropagate() {
		ConsumerRecord<String, String> record = new ConsumerRecord<>(LEGACY_TOPIC, 0, 0L, "key", "not-json");

		listener.onMessage(record);

		verify(legacyIndexService, never()).beginLegacyIndex(any());
	}

	@Test
	void onMessage_indexerThrows_doesNotPropagate() throws Exception {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("other", 0, 0L, "key", "{}");
		doThrow(new RuntimeException("index failed")).when(indexerService).esIndexer("other", "{}");

		listener.onMessage(record);

		verify(indexerService).esIndexer("other", "{}");
	}

	private static LegacyIndexRequest sampleLegacyRequest() {
		RequestInfo requestInfo = RequestInfo.builder()
				.userInfo(User.builder().uuid("admin").roles(List.of()).build())
				.build();
		PaginationDetails pagination = PaginationDetails.builder()
				.offsetKey("offset")
				.sizeKey("size")
				.maxPageSize(10)
				.build();
		APIDetails apiDetails = APIDetails.builder()
				.uri("/search")
				.paginationDetails(pagination)
				.responseJsonPath("$.items")
				.build();
		return LegacyIndexRequest.builder()
				.requestInfo(requestInfo)
				.apiDetails(apiDetails)
				.legacyIndexTopic("legacy-topic")
				.tenantId("pg.citya")
				.build();
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
