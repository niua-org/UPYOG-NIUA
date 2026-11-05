package org.egov.infra.indexer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.infra.indexer.service.IndexerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoreIndexMessageListenerTest {

    @Mock
    private IndexerService indexerService;

    private CoreIndexMessageListener listener;

    @BeforeEach
    void setUp() throws Exception {
        listener = new CoreIndexMessageListener();
        Field field = CoreIndexMessageListener.class.getDeclaredField("indexerService");
        field.setAccessible(true);
        field.set(listener, indexerService);
    }

    @Test
    void onMessage_validRecord_delegatesToIndexerService() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("save-service", 0, 0L, "key", "{\"id\":1}");

        listener.onMessage(record);

        verify(indexerService).esIndexer("save-service", "{\"id\":1}");
    }

    @Test
    void onMessage_indexerThrows_doesNotPropagate() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("save-service", 0, 0L, "key", "{}");
        doThrow(new RuntimeException("index failed")).when(indexerService).esIndexer("save-service", "{}");

        listener.onMessage(record);

        verify(indexerService).esIndexer("save-service", "{}");
    }
}
