package org.upyog.pgrai.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.pgrai.config.PGRConfiguration;
import org.upyog.pgrai.producer.Producer;
import org.upyog.pgrai.repository.PGRRepository;
import org.upyog.pgrai.util.MDMSUtils;
import org.upyog.pgrai.validator.ServiceRequestValidator;
import org.upyog.pgrai.web.models.RequestSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PGRServiceTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private UserService userService;
    @Mock private WorkflowService workflowService;
    @Mock private ServiceRequestValidator validator;
    @Mock private Producer producer;
    @Mock private PGRConfiguration config;
    @Mock private PGRRepository repository;
    @Mock private MDMSUtils mdmsUtils;

    @InjectMocks
    private PGRService pgrService;

    @Test
    void search_whenCriteriaEmpty_returnsEmpty() {
        RequestSearchCriteria criteria = new RequestSearchCriteria();

        assertThat(pgrService.search(RequestInfo.builder().build(), criteria)).isEmpty();
    }
}
