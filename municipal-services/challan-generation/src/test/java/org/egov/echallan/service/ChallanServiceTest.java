package org.egov.echallan.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ChallanRepository;
import org.egov.echallan.util.CommonUtils;
import org.egov.echallan.util.ResponseInfoFactory;
import org.egov.echallan.validator.ChallanValidator;
import org.egov.echallan.workflow.WorkflowIntegrator;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallanServiceTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private ResponseInfoFactory responseInfoFactory;
    @Mock private UserService userService;
    @Mock private ChallanRepository repository;
    @Mock private CalculationService calculationService;
    @Mock private ChallanValidator validator;
    @Mock private CommonUtils utils;
    @Mock private ChallanConfiguration config;
    @Mock private org.egov.echallan.repository.ServiceRequestRepository serviceRequestRepository;
    @Mock private WorkflowIntegrator workflowIntegrator;

    @InjectMocks
    private ChallanService challanService;

    @Test
    void getChallanCountResponse_whenNoRecords_throws() {
        Map<String, String> empty = new HashMap<>();
        empty.put("totalChallan", "0");
        when(repository.fetchChallanCount("pb.amritsar")).thenReturn(empty);
        org.mockito.Mockito.doNothing().when(validator).validateChallanCountRequest("pb.amritsar");
        when(responseInfoFactory.createResponseInfoFromRequestInfo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(org.egov.common.contract.response.ResponseInfo.builder().status("successful").build());

        assertThatThrownBy(() -> challanService.getChallanCountResponse(RequestInfo.builder().build(), "pb.amritsar"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No records found");
    }
}
