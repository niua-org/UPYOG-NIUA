package org.egov.swservice.service;

import org.egov.common.contract.request.PlainAccessRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.config.SWConfiguration;
import org.egov.swservice.repository.SewerageDao;
import org.egov.swservice.repository.SewerageDaoImpl;
import org.egov.swservice.util.EncryptionDecryptionUtil;
import org.egov.swservice.util.SWConstants;
import org.egov.swservice.util.SewerageServicesUtil;
import org.egov.swservice.util.UnmaskingUtil;
import org.egov.swservice.validator.ActionValidator;
import org.egov.swservice.validator.MDMSValidator;
import org.egov.swservice.validator.SewerageConnectionValidator;
import org.egov.swservice.validator.ValidateProperty;
import org.egov.swservice.web.models.Connection.StatusEnum;
import org.egov.swservice.web.models.Property;
import org.egov.swservice.web.models.SearchCriteria;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.egov.swservice.workflow.WorkflowIntegrator;
import org.egov.swservice.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SewerageServiceImplTest {

    @Mock private SewerageServicesUtil sewerageServicesUtil;
    @Mock private SewerageConnectionValidator sewerageConnectionValidator;
    @Mock private ValidateProperty validateProperty;
    @Mock private MDMSValidator mDMSValidator;
    @Mock private WorkflowIntegrator wfIntegrator;
    @Mock private SWConfiguration config;
    @Mock private EnrichmentService enrichmentService;
    @Mock private SewerageDao sewerageDao;
    @Mock private ActionValidator actionValidator;
    @Mock private WorkflowService workflowService;
    @Mock private SewerageDaoImpl sewerageDaoImpl;
    @Mock private CalculationService calculationService;
    @Mock private UserService userService;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;
    @Mock private PaymentUpdateService paymentUpdateService;
    @Mock private UnmaskingUtil unmaskingUtil;

    @InjectMocks
    private SewerageServiceImpl sewerageServiceImpl;

    private RequestInfo requestInfo;

    @BeforeEach
    void stubEncryptionPassthrough() {
        requestInfo = RequestInfo.builder().build();
        lenient().when(encryptionDecryptionUtil.encryptObject(any(), anyString(), any(Class.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(encryptionDecryptionUtil.decryptObject(any(), anyString(), any(Class.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(encryptionDecryptionUtil.decryptObject(anyList(), anyString(), any(Class.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(config.getIsExternalWorkFlowEnabled()).thenReturn(false);
    }

    private static SewerageConnection connection(String applicationNo) {
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setApplicationNo(applicationNo);
        return sewerageConnection;
    }

    @Test
    void getSewerageConnectionsList_whenDaoReturnsRows_returnsSameList() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        SewerageConnection row = connection("SW-1");
        when(sewerageDao.getSewerageConnectionList(criteria, requestInfo)).thenReturn(List.of(row));

        List<SewerageConnection> result = sewerageServiceImpl.getSewerageConnectionsList(criteria, requestInfo);

        assertThat(result).containsExactly(row);
    }

    @Test
    void getSewerageConnectionsCount_whenDaoReturnsCount_returnsInteger() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(sewerageDao.getSewerageConnectionsCount(criteria, requestInfo)).thenReturn(7);

        assertThat(sewerageServiceImpl.getSewerageConnectionsCount(criteria, requestInfo)).isEqualTo(7);
    }

    @Test
    void countAllSewerageApplications_whenCalled_setsCountFlagAndReturnsDaoCount() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(sewerageDao.getSewerageConnectionsCount(criteria, requestInfo)).thenReturn(4);

        Integer count = sewerageServiceImpl.countAllSewerageApplications(criteria, requestInfo);

        assertThat(count).isEqualTo(4);
        assertThat(criteria.getIsCountCall()).isTrue();
    }

    @Test
    void search_whenStandardSearch_returnsConnectionsFromDao() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        SewerageConnection row = connection("SW-100");
        row.setId("uuid-1");
        when(sewerageDao.getSewerageConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));

        List<SewerageConnection> result = sewerageServiceImpl.search(criteria, requestInfo);

        assertThat(result).hasSize(1);
        verify(sewerageConnectionValidator).validateSearch(criteria);
        verify(enrichmentService).enrichConnectionHolderDeatils(anyList(), eq(criteria), eq(requestInfo));
        verify(enrichmentService).enrichProcessInstance(anyList(), eq(criteria), eq(requestInfo));
    }

    @Test
    void search_whenConnectionSearchType_filtersThroughEnrichmentService() {
        SearchCriteria criteria = SearchCriteria.builder()
                .tenantId("pb.amritsar")
                .searchType(SWConstants.SEARCH_TYPE_CONNECTION)
                .build();
        SewerageConnection row = connection("SW-200");
        row.setConnectionNo("CN-1");
        when(sewerageDao.getSewerageConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));
        when(enrichmentService.filterConnections(anyList())).thenReturn(List.of(row));

        List<SewerageConnection> result = sewerageServiceImpl.search(criteria, requestInfo);

        assertThat(result).hasSize(1);
        verify(enrichmentService).filterConnections(anyList());
    }

    @Test
    void search_whenPropertyDetailsRequired_enrichesPropertyDetails() {
        SearchCriteria criteria = SearchCriteria.builder()
                .tenantId("pb.amritsar")
                .isPropertyDetailsRequired(true)
                .build();
        SewerageConnection row = connection("SW-250");
        when(sewerageDao.getSewerageConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));
        when(enrichmentService.enrichPropertyDetails(anyList(), eq(criteria), eq(requestInfo))).thenReturn(List.of(row));

        sewerageServiceImpl.search(criteria, requestInfo);

        verify(enrichmentService).enrichPropertyDetails(anyList(), eq(criteria), eq(requestInfo));
        verify(validateProperty).validatePropertyForConnection(anyList());
    }

    @Test
    void search_whenInternalCall_usesDisabledDecryptionModels() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").isInternalCall(true).build();
        when(sewerageDao.getSewerageConnectionList(any(), eq(requestInfo))).thenReturn(List.of(connection("SW-300")));

        sewerageServiceImpl.search(criteria, requestInfo);

        verify(encryptionDecryptionUtil).decryptObject(anyList(), eq("WnSConnectionPlumberDecrypDisabled"),
                eq(SewerageConnection.class), eq(requestInfo));
        verify(encryptionDecryptionUtil).decryptObject(anyList(), eq("WnSConnectionDecrypDisabled"),
                eq(SewerageConnection.class), eq(requestInfo));
    }

    @Test
    void search_whenPlainAccessRequestPresent_preservesPlainAccessOnRequestInfo() {
        PlainAccessRequest plainAccessRequest = new PlainAccessRequest("rec-1", List.of("mobileNumber"));
        RequestInfo infoWithPlain = RequestInfo.builder().plainAccessRequest(plainAccessRequest).build();
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(sewerageDao.getSewerageConnectionList(any(), eq(infoWithPlain))).thenReturn(List.of(connection("SW-301")));

        sewerageServiceImpl.search(criteria, infoWithPlain);

        assertThat(infoWithPlain.getPlainAccessRequest()).isNotNull();
        assertThat(infoWithPlain.getPlainAccessRequest().getRecordId()).isEqualTo("rec-1");
    }

    @Test
    void getConnectionForUpdateRequest_whenNoMatch_throwsInvalidSearch() {
        when(sewerageDao.getSewerageConnectionList(any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> sewerageServiceImpl.getConnectionForUpdateRequest("pb.amritsar", "missing-id", requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Sewerage Connection not found");
    }

    @Test
    void getConnectionForUpdateRequest_whenSingleMatch_returnsConnection() {
        SewerageConnection row = connection("SW-400");
        row.setId("conn-id");
        when(sewerageDao.getSewerageConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));

        SewerageConnection result = sewerageServiceImpl.getConnectionForUpdateRequest("pb.amritsar", "conn-id", requestInfo);

        assertThat(result).isSameAs(row);
    }

    @Test
    void plainSearch_whenDaoReturnsRows_enrichesAndValidatesProperty() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        SewerageConnection row = connection("SW-500");
        when(sewerageDao.getSewerageConnectionPlainSearchList(any(), eq(requestInfo))).thenReturn(List.of(row));
        when(enrichmentService.enrichPropertyDetails(anyList(), eq(criteria), eq(requestInfo))).thenReturn(List.of(row));

        List<SewerageConnection> result = sewerageServiceImpl.plainSearch(criteria, requestInfo);

        assertThat(result).hasSize(1);
        assertThat(criteria.getIsSkipLevelSearch()).isTrue();
        verify(validateProperty).validatePropertyForConnection(anyList());
        verify(enrichmentService).enrichConnectionHolderDeatils(anyList(), eq(criteria), eq(requestInfo));
    }

    @Test
    void getSewerageConnectionsPlainSearchList_whenDaoReturnsRows_returnsList() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        SewerageConnection row = connection("SW-600");
        when(sewerageDao.getSewerageConnectionPlainSearchList(criteria, requestInfo)).thenReturn(List.of(row));

        assertThat(sewerageServiceImpl.getSewerageConnectionsPlainSearchList(criteria, requestInfo)).containsExactly(row);
    }

    @Test
    void createSewerageConnection_whenDisconnectOnInactiveConnection_throwsInvalidRequest() {
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setStatus(StatusEnum.INACTIVE);
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .disconnectRequest(true)
                .build();

        assertThatThrownBy(() -> sewerageServiceImpl.createSewerageConnection(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("must be active for disconnection");
        verify(sewerageConnectionValidator, never()).validateSewerageConnection(any(), anyInt());
    }

    @Test
    void createSewerageConnection_whenReconnectOnActiveConnection_throwsInvalidRequest() {
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setStatus(StatusEnum.ACTIVE);
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .reconnectRequest(true)
                .build();

        assertThatThrownBy(() -> sewerageServiceImpl.createSewerageConnection(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("must be inactive for reconnection");
    }

    @Test
    void createSewerageConnection_whenStandardCreate_persistsAndReturnsConnection() {
        ProcessInstance processInstance = ProcessInstance.builder().action(SWConstants.ACTION_INITIATE).build();
        SewerageConnection sewerageConnection = new SewerageConnection();
        sewerageConnection.setTenantId("pb.amritsar");
        sewerageConnection.setProcessInstance(processInstance);
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(requestInfo)
                .sewerageConnection(sewerageConnection)
                .build();
        Property property = Property.builder().tenantId("pb.amritsar").build();
        when(validateProperty.getOrValidateProperty(request)).thenReturn(property);
        when(sewerageServicesUtil.isModifyConnectionRequest(request)).thenReturn(false);

        List<SewerageConnection> created = sewerageServiceImpl.createSewerageConnection(request);

        assertThat(created).hasSize(1);
        verify(sewerageConnectionValidator).validateSewerageConnection(request, SWConstants.CREATE_APPLICATION);
        verify(mDMSValidator).validateMasterForCreateRequest(request);
        verify(enrichmentService).enrichSewerageConnection(request, SWConstants.CREATE_APPLICATION);
        verify(userService).createUser(request);
        verify(sewerageDao).saveSewerageConnection(request);
        verify(wfIntegrator, never()).callWorkFlow(any(), any());
    }
}
