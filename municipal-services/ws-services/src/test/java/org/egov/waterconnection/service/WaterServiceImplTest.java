package org.egov.waterconnection.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.repository.WaterDao;
import org.egov.waterconnection.util.EncryptionDecryptionUtil;
import org.egov.waterconnection.validator.WaterConnectionValidator;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.egov.waterconnection.web.models.WaterConnection;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaterServiceImplTest {

    @Mock private WaterDao waterDao;
    @Mock private WaterConnectionValidator waterConnectionValidator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @InjectMocks
    private WaterServiceImpl waterServiceImpl;

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
    }

    private static WaterConnection connection(String applicationNo) {
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setApplicationNo(applicationNo);
        return waterConnection;
    }

    @Test
    void getWaterConnectionsList_whenDaoReturnsRows_returnsSameList() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        WaterConnection row = connection("WS-1");
        when(waterDao.getWaterConnectionList(criteria, requestInfo)).thenReturn(List.of(row));

        List<WaterConnection> result = waterServiceImpl.getWaterConnectionsList(criteria, requestInfo);

        assertThat(result).containsExactly(row);
    }

    @Test
    void getWaterConnectionsCount_whenDaoReturnsCount_returnsInteger() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(waterDao.getWaterConnectionsCount(criteria, requestInfo)).thenReturn(12);

        assertThat(waterServiceImpl.getWaterConnectionsCount(criteria, requestInfo)).isEqualTo(12);
    }

    @Test
    void countAllWaterApplications_whenCalled_setsCountFlagAndReturnsDaoCount() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        when(waterDao.getWaterConnectionsCount(criteria, requestInfo)).thenReturn(3);

        Integer count = waterServiceImpl.countAllWaterApplications(criteria, requestInfo);

        assertThat(count).isEqualTo(3);
        assertThat(criteria.getIsCountCall()).isTrue();
    }

    @Test
    void search_whenStandardSearch_returnsConnectionsFromDao() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        WaterConnection row = connection("WS-100");
        row.setId("uuid-1");
        when(waterDao.getWaterConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));

        List<WaterConnection> result = waterServiceImpl.search(criteria, requestInfo);

        assertThat(result).hasSize(1);
        verify(waterConnectionValidator).validateSearch(criteria);
        verify(enrichmentService).enrichConnectionHolderDeatils(anyList(), eq(criteria), eq(requestInfo));
        verify(enrichmentService).enrichProcessInstance(anyList(), eq(criteria), eq(requestInfo));
    }

    @Test
    void search_whenConnectionSearchType_filtersThroughEnrichmentService() {
        SearchCriteria criteria = SearchCriteria.builder()
                .tenantId("pb.amritsar")
                .searchType(WCConstants.SEARCH_TYPE_CONNECTION)
                .build();
        WaterConnection row = connection("WS-200");
        row.setConnectionNo("CN-1");
        when(waterDao.getWaterConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));
        when(enrichmentService.filterConnections(anyList())).thenReturn(List.of(row));

        List<WaterConnection> result = waterServiceImpl.search(criteria, requestInfo);

        assertThat(result).hasSize(1);
        verify(enrichmentService).filterConnections(anyList());
    }

    @Test
    void search_whenInternalCall_usesDisabledDecryptionModels() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").isInternalCall(true).build();
        when(waterDao.getWaterConnectionList(any(), eq(requestInfo))).thenReturn(List.of(connection("WS-300")));

        waterServiceImpl.search(criteria, requestInfo);

        verify(encryptionDecryptionUtil).decryptObject(anyList(), eq("WnSConnectionPlumberDecrypDisabled"),
                eq(WaterConnection.class), eq(requestInfo));
        verify(encryptionDecryptionUtil).decryptObject(anyList(), eq("WnSConnectionDecrypDisabled"),
                eq(WaterConnection.class), eq(requestInfo));
    }

    @Test
    void getConnectionForUpdateRequest_whenNoMatch_throwsInvalidSearch() {
        when(waterDao.getWaterConnectionList(any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> waterServiceImpl.getConnectionForUpdateRequest("pb.amritsar", "missing-id", requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("WATER CONNECTION NOT FOUND");
    }

    @Test
    void getConnectionForUpdateRequest_whenSingleMatch_returnsConnection() {
        WaterConnection row = connection("WS-400");
        row.setId("conn-id");
        when(waterDao.getWaterConnectionList(any(), eq(requestInfo))).thenReturn(List.of(row));

        WaterConnection result = waterServiceImpl.getConnectionForUpdateRequest("pb.amritsar", "conn-id", requestInfo);

        assertThat(result).isSameAs(row);
    }
}
