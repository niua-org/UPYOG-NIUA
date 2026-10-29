package org.egov.ewst.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.models.EwasteApplicationSearchCriteria;
import org.egov.ewst.producer.Producer;
import org.egov.ewst.repository.EwasteApplicationRepository;
import org.egov.ewst.validator.EwasteRequestValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EwasteServiceTest {

    @Mock private Producer producer;
    @Mock private EwasteConfiguration config;
    @Mock private EnrichmentService enrichmentService;
    @Mock private EwasteRequestValidator validator;
    @Mock private WorkflowService wfService;
    @Mock private EwasteApplicationRepository ewasteApplicationRepository;

    @InjectMocks
    private EwasteService ewasteService;

    @Test
    void searchEwasteApplications_whenRepositoryEmpty_returnsEmptyList() {
        EwasteApplicationSearchCriteria criteria = EwasteApplicationSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(ewasteApplicationRepository.getApplication(criteria)).thenReturn(Collections.emptyList());

        assertThat(ewasteService.searchEwasteApplications(RequestInfo.builder().build(), criteria)).isEmpty();
    }
}
