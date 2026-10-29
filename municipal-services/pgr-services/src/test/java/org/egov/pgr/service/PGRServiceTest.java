package org.egov.pgr.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.producer.Producer;
import org.egov.pgr.repository.PGRRepository;
import org.egov.pgr.util.MDMSUtils;
import org.egov.pgr.validator.ServiceRequestValidator;
import org.egov.pgr.web.models.Address;
import org.egov.pgr.web.models.AuditDetails;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.pgr.web.models.ServiceWrapper;
import org.egov.pgr.web.models.Workflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PGRServiceTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private UserService userService;
    @Mock private WorkflowService workflowService;
    @Mock private ServiceRequestValidator validator;
    @Mock private Producer producer;
    @Mock private PGRConfiguration config;
    @Mock private PGRRepository repository;
    @Mock private MDMSUtils mdmsUtils;

    private PGRService pgrService;

    @BeforeEach
    void setUp() {
        pgrService = new PGRService(
                enrichmentService,
                userService,
                workflowService,
                validator,
                validator,
                producer,
                config,
                repository,
                mdmsUtils);
        when(config.getCreateTopic()).thenReturn("pgr-create");
        when(config.getUpdateTopic()).thenReturn("pgr-update");
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getMaxLimit()).thenReturn(100);
        when(config.getComplaintTypes()).thenReturn("42");
    }

    @Test
    void create_orchestratesValidationEnrichmentWorkflowAndKafka() {
        ServiceRequest request = serviceRequest("pg.citya", "SR-1");
        Object mdms = new Object();
        when(mdmsUtils.mDMSCall(request)).thenReturn(mdms);

        ServiceRequest result = pgrService.create(request);

        assertThat(result).isSameAs(request);
        verify(validator).validateCreate(request, mdms);
        verify(enrichmentService).enrichCreateRequest(request);
        verify(workflowService).updateWorkflowStatus(request);
        verify(producer).push("pg.citya", "pgr-create", request);
    }

    @Test
    void update_orchestratesValidationEnrichmentWorkflowAndKafka() {
        ServiceRequest request = serviceRequest("pg.citya", "SR-1");
        Object mdms = new Object();
        when(mdmsUtils.mDMSCall(request)).thenReturn(mdms);

        ServiceRequest result = pgrService.update(request);

        assertThat(result).isSameAs(request);
        verify(validator).validateUpdate(request, mdms);
        verify(enrichmentService).enrichUpdateRequest(request);
        verify(workflowService).updateWorkflowStatus(request);
        verify(producer).push("pg.citya", "pgr-update", request);
    }

    @Test
    void search_whenCriteriaEmpty_returnsEmptyWithoutRepositoryCall() {
        RequestInfo requestInfo = employeeRequestInfo();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().build();

        List<ServiceWrapper> result = pgrService.search(requestInfo, criteria);

        assertThat(result).isEmpty();
        verify(repository, never()).getServiceWrappers(any());
    }

    @Test
    void search_whenMobileWithoutUserIds_returnsEmpty() {
        RequestInfo requestInfo = employeeRequestInfo();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .mobileNumber("9999999999")
                .build();

        List<ServiceWrapper> result = pgrService.search(requestInfo, criteria);

        assertThat(result).isEmpty();
        verify(repository, never()).getServiceWrappers(any());
    }

    @Test
    void search_whenWrappersFound_enrichesUsersWorkflowAndSortsByCreatedTimeDesc() {
        RequestInfo requestInfo = employeeRequestInfo();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .serviceRequestId("SR-OLD")
                .build();
        ServiceWrapper older = wrapper("SR-OLD", 100L);
        ServiceWrapper newer = wrapper("SR-NEW", 200L);
        when(repository.getServiceWrappers(criteria)).thenReturn(List.of(older, newer));
        when(workflowService.enrichWorkflow(requestInfo, List.of(older, newer))).thenReturn(List.of(older, newer));

        List<ServiceWrapper> result = pgrService.search(requestInfo, criteria);

        assertThat(result).extracting(w -> w.getService().getServiceRequestId())
                .containsExactly("SR-NEW", "SR-OLD");
        verify(userService).enrichUsers(List.of(older, newer), requestInfo, "pg.citya");
        verify(workflowService).enrichWorkflow(requestInfo, List.of(older, newer));
    }

    @Test
    void plainSearch_appliesPaginationDefaultsAndReturnsSortedWrappers() {
        RequestInfo requestInfo = employeeRequestInfo();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantIds(Set.of("pg.citya"))
                .limit(500)
                .build();
        ServiceWrapper wrapper = wrapper("SR-1", 50L);
        when(repository.getServiceWrappers(criteria)).thenReturn(List.of(wrapper));
        when(workflowService.enrichWorkflow(requestInfo, List.of(wrapper))).thenReturn(List.of(wrapper));

        List<ServiceWrapper> result = pgrService.plainSearch(requestInfo, criteria);

        assertThat(result).hasSize(1);
        assertThat(criteria.getLimit()).isEqualTo(100);
        assertThat(criteria.getOffset()).isEqualTo(0);
        assertThat(criteria.getIsPlainSearch()).isTrue();
        verify(validator).validatePlainSearch(criteria);
    }

    @Test
    void count_delegatesToRepository() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().tenantId("pg.citya").build();
        when(repository.getCount(criteria)).thenReturn(7);

        assertThat(pgrService.count(RequestInfo.builder().build(), criteria)).isEqualTo(7);
        assertThat(criteria.getIsPlainSearch()).isFalse();
    }

    @Test
    void getDynamicData_returnsRepositoryMap() {
        Map<String, Integer> data = new HashMap<>();
        data.put("open", 3);
        when(repository.fetchDynamicData("pg.citya")).thenReturn(data);

        assertThat(pgrService.getDynamicData("pg.citya")).containsEntry("open", 3);
    }

    @Test
    void getComplaintTypes_parsesConfiguration() {
        assertThat(pgrService.getComplaintTypes()).isEqualTo(42);
    }

    private static RequestInfo employeeRequestInfo() {
        User employee = User.builder().type("EMPLOYEE").uuid("emp-1").tenantId("pg.citya").build();
        return RequestInfo.builder().userInfo(employee).build();
    }

    private static ServiceRequest serviceRequest(String tenantId, String serviceRequestId) {
        Service service = Service.builder()
                .tenantId(tenantId)
                .serviceRequestId(serviceRequestId)
                .address(Address.builder().build())
                .build();
        return ServiceRequest.builder()
                .requestInfo(employeeRequestInfo())
                .service(service)
                .workflow(Workflow.builder().action("APPLY").build())
                .build();
    }

    private static ServiceWrapper wrapper(String serviceRequestId, long createdTime) {
        Service service = Service.builder()
                .serviceRequestId(serviceRequestId)
                .tenantId("pg.citya")
                .auditDetails(AuditDetails.builder().createdTime(createdTime).build())
                .build();
        return ServiceWrapper.builder().service(service).build();
    }
}
