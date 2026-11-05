package org.egov.swservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.swservice.config.SWConfiguration;
import org.egov.swservice.repository.IdGenRepository;
import org.egov.swservice.util.SWConstants;
import org.egov.swservice.util.SewerageServicesUtil;
import org.egov.swservice.web.models.AuditDetails;
import org.egov.swservice.web.models.Idgen.IdGenerationResponse;
import org.egov.swservice.web.models.Idgen.IdResponse;
import org.egov.swservice.web.models.SearchCriteria;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrichmentServiceTest {

    @Mock private SewerageServicesUtil sewerageServicesUtil;
    @Mock private IdGenRepository idGenRepository;
    @Mock private SWConfiguration config;
    @Spy private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private EnrichmentService enrichmentService;

    @BeforeEach
    void stubIdGen() {
        when(sewerageServicesUtil.getAuditDetails(anyString(), anyBoolean()))
                .thenReturn(AuditDetails.builder().createdBy("user-1").build());
        when(config.getSewerageApplicationIdGenName()).thenReturn("sw.application.id");
        when(config.getSewerageApplicationIdGenFormat()).thenReturn("SW/[cy]/[seq]");
        when(config.getSewerageDisconnectionIdGenName()).thenReturn("sw.disconnect.id");
        when(config.getSewerageDisconnectionIdGenFormat()).thenReturn("SW-D/[seq]");
        IdGenerationResponse idGenerationResponse = IdGenerationResponse.builder()
                .idResponses(List.of(IdResponse.builder().id("GEN-001").build()))
                .build();
        when(idGenRepository.getId(any(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(idGenerationResponse);
    }

    @Test
    void enrichSewerageConnection_whenCreateApplication_setsIdsChannelAndStatus() {
        User employee = User.builder().uuid("emp-1").type("EMPLOYEE").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        ProcessInstance processInstance = ProcessInstance.builder().action(SWConstants.ACTION_INITIATE).build();
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setTenantId("pb.amritsar");
        sewerageConnection.setProcessInstance(processInstance);
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .build();

        enrichmentService.enrichSewerageConnection(request, SWConstants.CREATE_APPLICATION);

        assertThat(sewerageConnection.getApplicationNo()).isEqualTo("GEN-001");
        assertThat(sewerageConnection.getApplicationType()).isEqualTo(SWConstants.NEW_SEWERAGE_CONNECTION);
        assertThat(sewerageConnection.getChannel()).isEqualTo("CFC_COUNTER");
        assertThat(sewerageConnection.getApplicationStatus()).isEqualTo(SWConstants.STATUS_INITIATED);
        assertThat(sewerageConnection.getId()).isNotBlank();
    }

    @Test
    void enrichSewerageConnection_whenDisconnectRequest_usesDisconnectionIdGen() {
        User employee = User.builder().uuid("emp-1").type("EMPLOYEE").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        ProcessInstance processInstance = ProcessInstance.builder().action(SWConstants.ACTION_INITIATE).build();
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setTenantId("pb.amritsar");
        sewerageConnection.setProcessInstance(processInstance);
        sewerageConnection.setApplicationStatus(SWConstants.STATUS_INITIATED);
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .disconnectRequest(true)
                .build();

        enrichmentService.enrichSewerageConnection(request, SWConstants.DISCONNECT_CONNECTION);

        assertThat(sewerageConnection.getApplicationType()).isEqualTo(SWConstants.DISCONNECT_SEWERAGE_CONNECTION);
        assertThat(sewerageConnection.getApplicationNo()).isEqualTo("GEN-001");
    }

    @Test
    void filterConnections_whenDuplicateFinalStates_keepsNewestByAppCreatedDate() {
        HashMap<String, Object> olderDetails = new HashMap<>();
        olderDetails.put(SWConstants.APP_CREATED_DATE, BigDecimal.valueOf(1000L));
        HashMap<String, Object> newerDetails = new HashMap<>();
        newerDetails.put(SWConstants.APP_CREATED_DATE, BigDecimal.valueOf(2000L));
        SewerageConnection older = new SewerageConnection();
        older.setConnectionNo("CN-1");
        older.setApplicationStatus(SWConstants.STATUS_APPROVED);
        older.setAdditionalDetails(olderDetails);
        SewerageConnection newer = new SewerageConnection();
        newer.setConnectionNo("CN-1");
        newer.setApplicationStatus(SWConstants.STATUS_APPROVED);
        newer.setAdditionalDetails(newerDetails);

        List<SewerageConnection> filtered = enrichmentService.filterConnections(List.of(older, newer));

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getAdditionalDetails()).isEqualTo(newerDetails);
    }

    @Test
    void filterConnections_whenModifiedFinalState_replacesExistingEntry() {
        HashMap<String, Object> details = new HashMap<>();
        details.put(SWConstants.APP_CREATED_DATE, BigDecimal.valueOf(1000L));
        SewerageConnection activated = new SewerageConnection();
        activated.setConnectionNo("CN-2");
        activated.setApplicationStatus(SWConstants.STATUS_APPROVED);
        activated.setAdditionalDetails(details);
        SewerageConnection modified = new SewerageConnection();
        modified.setConnectionNo("CN-2");
        modified.setApplicationStatus(SWConstants.MODIFIED_FINAL_STATE);
        modified.setAdditionalDetails(details);

        List<SewerageConnection> filtered = enrichmentService.filterConnections(List.of(activated, modified));

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getApplicationStatus()).isEqualTo(SWConstants.MODIFIED_FINAL_STATE);
    }

    @Test
    void isSearchOpen_whenSystemAnonymousRole_returnsTrue() {
        User systemUser = User.builder()
                .type("SYSTEM")
                .roles(List.of(Role.builder().code("ANONYMOUS").build()))
                .build();

        assertThat(enrichmentService.isSearchOpen(systemUser)).isTrue();
    }

    @Test
    void isSearchOpen_whenEmployeeUser_returnsFalse() {
        User employee = User.builder().type("EMPLOYEE").roles(List.of()).build();

        assertThat(enrichmentService.isSearchOpen(employee)).isFalse();
    }

    @Test
    void enrichPropertyDetails_whenEmptyList_returnsEmptyList() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();

        List<SewerageConnection> result = enrichmentService.enrichPropertyDetails(List.of(), criteria, RequestInfo.builder().build());

        assertThat(result).isEmpty();
    }

    @Test
    void enrichUpdateSewerageConnection_whenNewDocument_assignsIdsAndAudit() {
        User employee = User.builder().uuid("emp-1").type("EMPLOYEE").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        org.egov.swservice.web.models.Document document = org.egov.swservice.web.models.Document.builder().build();
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setDocuments(List.of(document));
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .build();

        enrichmentService.enrichUpdateSewerageConnection(request);

        assertThat(document.getId()).isNotBlank();
        assertThat(document.getDocumentUid()).isNotBlank();
        assertThat(sewerageConnection.getAuditDetails()).isNotNull();
    }
}
