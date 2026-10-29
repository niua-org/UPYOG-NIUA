package org.egov.infra.indexer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.infra.indexer.service.IndexerService;
import org.egov.infra.indexer.service.ReindexService;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.ReindexRequest;
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
class ReindexMessageListenerTest {

	private static final String REINDEX_TOPIC = "egov.core.reindex";

	@Mock
	private ReindexService reindexService;

	@Mock
	private IndexerService indexerService;

	private ReindexMessageListener listener;

	@BeforeEach
	void setUp() throws Exception {
		listener = new ReindexMessageListener();
		IndexerUtils indexerUtils = new IndexerUtils();
		setField(listener, "indexerUtils", indexerUtils);
		setField(listener, "reindexService", reindexService);
		setField(listener, "indexerService", indexerService);
		setField(listener, "reindexTopic", REINDEX_TOPIC);
	}

	@Test
	void onMessage_reindexTopic_delegatesToReindexService() throws Exception {
		ReindexRequest request = sampleReindexRequest();
		String payload = new ObjectMapper().writeValueAsString(request);
		ConsumerRecord<String, String> record = new ConsumerRecord<>(REINDEX_TOPIC, 0, 0L, "key", payload);

		listener.onMessage(record);

		verify(reindexService).beginReindex(any(ReindexRequest.class));
		verify(indexerService, never()).esIndexer(any(), any());
	}

	@Test
	void onMessage_otherTopic_delegatesToIndexerService() throws Exception {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("save-service", 0, 0L, "key", "{\"id\":1}");

		listener.onMessage(record);

		verify(indexerService).esIndexer("save-service", "{\"id\":1}");
		verify(reindexService, never()).beginReindex(any());
	}

	@Test
	void onMessage_invalidReindexPayload_doesNotPropagate() {
		ConsumerRecord<String, String> record = new ConsumerRecord<>(REINDEX_TOPIC, 0, 0L, "key", "{bad");

		listener.onMessage(record);

		verify(reindexService, never()).beginReindex(any());
	}

	@Test
	void onMessage_indexerThrows_doesNotPropagate() throws Exception {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("other", 0, 0L, "key", "{}");
		doThrow(new RuntimeException("index failed")).when(indexerService).esIndexer("other", "{}");

		listener.onMessage(record);

		verify(indexerService).esIndexer("other", "{}");
	}

	private static ReindexRequest sampleReindexRequest() {
		RequestInfo requestInfo = RequestInfo.builder()
				.userInfo(User.builder().uuid("admin").roles(List.of()).build())
				.build();
		return ReindexRequest.builder()
				.requestInfo(requestInfo)
				.index("citizen-index")
				.type("citizen")
				.reindexTopic("reindex-topic")
				.tenantId("pg.citya")
				.build();
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
