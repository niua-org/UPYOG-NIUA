package org.egov.waterconnection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.repository.IdGenRepository;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.web.models.AuditDetails;
import org.egov.waterconnection.web.models.Idgen.IdGenerationResponse;
import org.egov.waterconnection.web.models.Idgen.IdResponse;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
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

    @Mock private WaterServicesUtil waterServicesUtil;
    @Mock private IdGenRepository idGenRepository;
    @Mock private WSConfiguration config;
    @Spy private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private EnrichmentService enrichmentService;

    @BeforeEach
    void stubIdGen() {
        when(waterServicesUtil.getAuditDetails(anyString(), anyBoolean()))
                .thenReturn(AuditDetails.builder().createdBy("user-1").build());
        when(config.getWaterApplicationIdGenName()).thenReturn("ws.application.id");
        when(config.getWaterApplicationIdGenFormat()).thenReturn("WS/[cy]/[seq]");
        when(config.getWaterConnectionIdGenName()).thenReturn("ws.connection.id");
        when(config.getWaterConnectionIdGenFormat()).thenReturn("WS-CON/[seq]");
        IdGenerationResponse idGenerationResponse = IdGenerationResponse.builder()
                .idResponses(List.of(IdResponse.builder().id("GEN-001").build()))
                .build();
        when(idGenRepository.getId(any(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(idGenerationResponse);
    }

    @Test
    void enrichWaterConnection_whenCreateApplication_setsIdsChannelAndStatus() {
        User employee = User.builder().uuid("emp-1").type("EMPLOYEE").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        ProcessInstance processInstance = ProcessInstance.builder().action(WCConstants.ACTION_INITIATE).build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setProcessInstance(processInstance);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(requestInfo)
                .waterConnection(waterConnection)
                .build();

        enrichmentService.enrichWaterConnection(request, WCConstants.CREATE_APPLICATION);

        assertThat(waterConnection.getApplicationNo()).isEqualTo("GEN-001");
        assertThat(waterConnection.getApplicationType()).isEqualTo(WCConstants.NEW_WATER_CONNECTION);
        assertThat(waterConnection.getChannel()).isEqualTo("CFC_COUNTER");
        assertThat(waterConnection.getApplicationStatus()).isEqualTo(WCConstants.STATUS_INITIATED);
        assertThat(waterConnection.getId()).isNotBlank();
    }

    @Test
    void filterConnections_whenDuplicateFinalStates_keepsNewestByAppCreatedDate() {
        HashMap<String, Object> olderDetails = new HashMap<>();
        olderDetails.put(WCConstants.APP_CREATED_DATE, BigDecimal.valueOf(1000L));
        HashMap<String, Object> newerDetails = new HashMap<>();
        newerDetails.put(WCConstants.APP_CREATED_DATE, BigDecimal.valueOf(2000L));
        WaterConnection older = new WaterConnection();
        older.setConnectionNo("CN-1");
        older.setApplicationStatus(WCConstants.STATUS_APPROVED);
        older.setAdditionalDetails(olderDetails);
        WaterConnection newer = new WaterConnection();
        newer.setConnectionNo("CN-1");
        newer.setApplicationStatus(WCConstants.STATUS_APPROVED);
        newer.setAdditionalDetails(newerDetails);

        List<WaterConnection> filtered = enrichmentService.filterConnections(List.of(older, newer));

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getAdditionalDetails()).isEqualTo(newerDetails);
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
    void postStatusEnrichment_whenActivateConnection_assignsConnectionNumber() {
        User user = User.builder().uuid("u1").type("EMPLOYEE").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(user).build();
        ProcessInstance processInstance = ProcessInstance.builder().action(WCConstants.ACTIVATE_CONNECTION).build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setProcessInstance(processInstance);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(requestInfo)
                .waterConnection(waterConnection)
                .build();

        enrichmentService.postStatusEnrichment(request);

        assertThat(waterConnection.getConnectionNo()).isEqualTo("GEN-001");
    }
}
