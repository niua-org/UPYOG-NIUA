package org.upyog.dashboard.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.dashboard.config.DashboardProperties;
import org.upyog.dashboard.model.DashboardData;
import org.upyog.dashboard.model.DashboardPayload;
import org.upyog.dashboard.producer.DashboardProducer;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class KafkaIngestionRecordPersistenceServiceImplTest {

    @InjectMocks
    private KafkaIngestionRecordPersistenceServiceImpl service;

    @Mock
    private DashboardProducer producer;

    @Mock
    private DashboardProperties dashboardProperties;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.lenient().when(dashboardProperties.getSaveIngestionDetailTopic())
                .thenReturn("save-dashboard-ingestion-detail");
        org.mockito.Mockito.lenient().when(dashboardProperties.getSaveAdapterErrorLogTopic())
                .thenReturn("save-dashboard-data-error-log");
        java.lang.reflect.Field mapperField = KafkaIngestionRecordPersistenceServiceImpl.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, new ObjectMapper());
    }

    @Test
    @DisplayName("pushIngestionRecord publishes daily ingestion record to Kafka on success")
    void pushIngestionRecord_success_pushesIngestionTopic() {
        DashboardPayload payload = dashboardPayload();

        service.pushIngestionRecord(payload, "{\"request\":true}", "{\"status\":\"ok\"}", "SUCCESS");

        verify(producer).push(eq("save-dashboard-ingestion-detail"), any(Map.class));
        verify(producer, times(0)).push(eq("save-dashboard-data-error-log"), any(Map.class));
    }

    @Test
    @DisplayName("pushIngestionRecord publishes error log topic when status is FAILURE")
    void pushIngestionRecord_failure_pushesErrorLogTopic() {
        DashboardPayload payload = dashboardPayload();

        service.pushIngestionRecord(payload, "{\"request\":true}", "timeout", "FAILURE");

        verify(producer).push(eq("save-dashboard-ingestion-detail"), any(Map.class));
        verify(producer).push(eq("save-dashboard-data-error-log"), any(Map.class));
    }

    private static DashboardPayload dashboardPayload() {
        DashboardData data = DashboardData.builder()
                .module("PT")
                .ulb("pg.citya")
                .date("08-09-2026")
                .build();
        return DashboardPayload.builder()
                .data(List.of(data))
                .moduleDetailId("mod-detail-1")
                .schedulerId("sched-1")
                .build();
    }
}
