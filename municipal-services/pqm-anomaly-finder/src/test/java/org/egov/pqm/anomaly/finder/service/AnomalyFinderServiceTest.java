package org.egov.pqm.anomaly.finder.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pqm.anomaly.finder.config.PqmAnomalyConfiguration;
import org.egov.pqm.anomaly.finder.repository.AnomalyRepository;
import org.egov.pqm.anomaly.finder.service.notification.NotificationService;
import org.egov.pqm.anomaly.finder.validator.TestAnomalyFinderValidator;
import org.egov.pqm.anomaly.finder.web.model.PqmAnomalySearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyFinderServiceTest {

    @Mock private AnomalyRepository anomalyRepository;
    @Mock private TestAnomalyFinderValidator testAnomalyFinderValidator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private NotificationService notificationService;
    @Mock private PqmAnomalyConfiguration config;

    @InjectMocks
    private AnomalyFinderService anomalyFinderService;

    @Test
    void anomalySearch_delegatesToRepository() {
        PqmAnomalySearchCriteria criteria = PqmAnomalySearchCriteria.builder().tenantId("pb.amritsar").build();
        when(anomalyRepository.getAnomalyDataForCriteria(criteria)).thenReturn(Collections.emptyList());

        assertThat(anomalyFinderService.anomalySearch(criteria)).isEmpty();
    }

    @Test
    void pqmAnomalyPlainSearch_whenNoIds_returnsEmpty() {
        PqmAnomalySearchCriteria criteria = PqmAnomalySearchCriteria.builder().tenantId("pb.amritsar").build();
        when(anomalyRepository.fetchPqmAnomalyIds(criteria)).thenReturn(Collections.emptyList());

        assertThat(anomalyFinderService.pqmAnomalyPlainSearch(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
