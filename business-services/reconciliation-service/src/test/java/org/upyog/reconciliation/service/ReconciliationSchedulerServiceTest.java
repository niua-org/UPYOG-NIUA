package org.upyog.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.upyog.reconciliation.extractor.DataExtractorFactory;
import org.upyog.reconciliation.extractor.MetricDataExtractor;
import org.upyog.reconciliation.model.ReconConfiguration;
import org.upyog.reconciliation.model.ReconExtractionDetail;
import org.upyog.reconciliation.producer.ReconciliationProducer;
import org.upyog.reconciliation.repository.ReconConfigurationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationSchedulerServiceTest {

    @Mock
    private ReconConfigurationRepository configurationRepository;
    @Mock
    private DataExtractorFactory extractorFactory;
    @Mock
    private MetricDataExtractor metricDataExtractor;
    @Mock
    private ReconciliationProducer producer;

    @InjectMocks
    private ReconciliationSchedulerService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "dataSourceType", "ELASTICSEARCH");
        ReflectionTestUtils.setField(service, "saveTopic", "save-reconciliation-details");
    }

    @Test
    void executeExtractionForDate_success_pushesKafkaPayload() throws Exception {
        ReconConfiguration config = new ReconConfiguration();
        config.setId(1L);
        config.setTenantId("pg.smoke");
        config.setModuleName("finance");
        config.setClientName("client");
        config.setIsActive(true);

        when(configurationRepository.findByIsActiveTrue()).thenReturn(List.of(config));
        when(extractorFactory.getExtractor("ELASTICSEARCH")).thenReturn(metricDataExtractor);
        when(metricDataExtractor.extractData("pg.smoke", "finance", LocalDate.of(2024, 1, 2)))
                .thenReturn(List.of(Map.of("metric", 1)));

        service.executeExtractionForDate(LocalDate.of(2024, 1, 2));

        ArgumentCaptor<ReconExtractionDetail> captor = ArgumentCaptor.forClass(ReconExtractionDetail.class);
        verify(producer).push(eq("save-reconciliation-details"), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        verify(configurationRepository).save(config);
    }

    @Test
    void executeExtractionForDate_extractorFailure_setsFailureStatus() {
        ReconConfiguration config = new ReconConfiguration();
        config.setId(2L);
        config.setTenantId("pg.smoke");
        config.setModuleName("finance");
        config.setClientName("client");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(List.of(config));
        when(extractorFactory.getExtractor("ELASTICSEARCH")).thenThrow(new IllegalArgumentException("bad type"));

        service.executeExtractionForDate(LocalDate.of(2024, 1, 2));

        ArgumentCaptor<ReconExtractionDetail> captor = ArgumentCaptor.forClass(ReconExtractionDetail.class);
        verify(producer).push(eq("save-reconciliation-details"), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILURE");
        assertThat(captor.getValue().getErrorMessage()).contains("bad type");
    }
}
