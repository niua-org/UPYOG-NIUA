package org.bel.birthdeath.birth.service;

import org.bel.birthdeath.birth.model.SearchCriteria;
import org.bel.birthdeath.birth.repository.BirthRepository;
import org.bel.birthdeath.birth.validator.BirthValidator;
import org.bel.birthdeath.common.repository.ServiceRequestRepository;
import org.bel.birthdeath.config.BirthDeathConfiguration;
import org.bel.birthdeath.utils.CommonUtils;
import org.bel.birthdeath.common.consumer.ReceiptConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthServiceTest {

    @Mock private BirthRepository repository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private BirthValidator validator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private CalculationService calculationService;
    @Mock private CommonUtils commUtils;
    @Mock private BirthDeathConfiguration config;
    @Mock private ReceiptConsumer consumer;

    @InjectMocks private BirthService birthService;

    @Test
    void plainSearch_delegatesToRepository() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(repository.getBirthCertificateForPlainSearch(criteria)).thenReturn(Collections.emptyList());

        assertThat(birthService.plainSearch(criteria)).isEmpty();
    }

    @Test
    void search_whenEmployeeValidatorFails_returnsEmpty() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(User.builder().type("EMPLOYEE").build()).build();
        when(validator.validateFieldsEmployee(criteria)).thenReturn(false);

        assertThat(birthService.search(criteria, requestInfo)).isEmpty();
    }

    @Test
    void search_whenEmployeeValidatorPasses_returnsRepositoryResults() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(User.builder().type("EMPLOYEE").build()).build();
        when(validator.validateFieldsEmployee(criteria)).thenReturn(true);
        when(repository.getBirthDtls(criteria)).thenReturn(Collections.emptyList());

        assertThat(birthService.search(criteria, requestInfo)).isEmpty();
    }
}
