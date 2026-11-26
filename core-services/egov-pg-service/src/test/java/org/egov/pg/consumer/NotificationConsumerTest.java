package org.egov.pg.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pg.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        notificationConsumer = new NotificationConsumer();
        try {
            var field = NotificationConsumer.class.getDeclaredField("notificationService");
            field.setAccessible(true);
            field.set(notificationConsumer, notificationService);
            var mapperField = NotificationConsumer.class.getDeclaredField("mapper");
            mapperField.setAccessible(true);
            mapperField.set(notificationConsumer, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void listenDelegatesToNotificationService() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("RequestInfo", new HashMap<>());
        record.put("Transaction", new HashMap<>());

        notificationConsumer.listen(record, "pg-txn-topic");

        verify(notificationService).smsNotification(any(), eq("pg-txn-topic"));
    }

    @Test
    public void listenSwallowsExceptions() {
        doThrow(new RuntimeException("fail")).when(notificationService).smsNotification(any(), any());
        notificationConsumer.listen(new HashMap<>(), "topic");
    }
}
