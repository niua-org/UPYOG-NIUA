package org.egov.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.InstrumentSearchContract;
import org.egov.receipt.consumer.model.MdmsCriteria;
import org.egov.receipt.consumer.model.MdmsCriteriaReq;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.egov.tracer.model.ServiceCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MicroServiceUtilImplTest {

    @Mock
    private PropertiesManager manager;
    @Spy
    private MdmsCriteria mdmscriteria = new MdmsCriteria();
    @Spy
    private MdmsCriteriaReq mdmsrequest = new MdmsCriteriaReq();
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @InjectMocks
    private MicroServiceUtilImpl microServiceUtil;

    private FinanceMdmsModel finSerMdms;
    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        finSerMdms = new FinanceMdmsModel();
        requestInfo = FinanceConsumerTestFixtures.requestInfo();
        when(manager.getMdmsHostUrl()).thenReturn("http://mdms/");
        when(manager.getMdmsSearchUrl()).thenReturn("/search");
        when(manager.getInstrumentHostUrl()).thenReturn("http://instrument/");
        when(manager.getInstrumentSearch()).thenReturn("/search");
    }

    @Test
    void getBusinessService_loadsMdmsWhenMissing() throws VoucherCustomException {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getBusinessService(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .hasSize(1);
        verify(serviceRequestRepository).fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID));
    }

    @Test
    void getBusinessService_invalidJson_throws() {
        finSerMdms.setFinanceServiceMdmsData(Map.of("MdmsRes", Map.of("FinanceModule", Map.of("BusinessServiceMapping", "bad"))));

        assertThatThrownBy(() -> microServiceUtil.getBusinessService(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("business/account head mapping");
    }

    @Test
    void getTaxHeadMasters_returnsMappedEntries() throws VoucherCustomException {
        finSerMdms.setFinanceServiceMdmsData(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getTaxHeadMasters(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .hasSize(1);
    }

    @Test
    void getBusinessServiceName_returnsNameOrCode() throws VoucherCustomException {
        finSerMdms.setFinanceServiceMdmsData(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getBusinessServiceName(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .isEqualTo("Property Tax");
    }

    @Test
    void getGlcodeByInstrumentType_cashAndOnlinePaths() throws VoucherCustomException {
        finSerMdms.setFinanceServiceMdmsData(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getGlcodeByInstrumentType(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms, "Cash"))
                .isEqualTo("110001");
        assertThat(microServiceUtil.getGlcodeByInstrumentType(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms, "Online"))
                .isEqualTo("220001");
    }

    @Test
    void getFinancialStatusByCode_matchesStatus() throws VoucherCustomException {
        finSerMdms.setFinanceServiceMdmsData(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getFinancialStatusByCode(
                FinanceConsumerTestFixtures.TENANT_ID, requestInfo, finSerMdms, "New").getCode())
                .isEqualTo("New");
    }

    @Test
    void getFinanceTenantList_returnsTenants() throws VoucherCustomException {
        finSerMdms.setFinanceServiceMdmsData(FinanceConsumerTestFixtures.mdmsPayload());

        assertThat(microServiceUtil.getFinanceTenantList(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .extracting("code")
                .containsExactly(FinanceConsumerTestFixtures.TENANT_ID);
    }

    @Test
    void getInstruments_buildsSearchUrl() throws VoucherCustomException {
        Map<String, Object> response = Map.of("instruments", List.of(Map.of("id", "inst-1")));
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(response);

        assertThat(microServiceUtil.getInstruments(
                InstrumentSearchContract.builder().receiptIds("PAY-1").build(),
                requestInfo,
                FinanceConsumerTestFixtures.TENANT_ID)).isNotNull();
    }

    @Test
    void getFinanceServiceMdmsData_serviceCallException() throws VoucherCustomException {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenThrow(new ServiceCallException("mdms down"));

        assertThatThrownBy(() -> microServiceUtil.getFinanceServiceMdmsData(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("MDMS service call failed");
    }

    @Test
    void getFinanceServiceMdmsData_genericException() throws VoucherCustomException {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> microServiceUtil.getFinanceServiceMdmsData(
                FinanceConsumerTestFixtures.TENANT_ID, FinanceConsumerTestFixtures.BS_CODE, requestInfo, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("Error Occured While calling the URL");
    }

    @Test
    void prepareFilter_multiBusinessServiceCodes() throws VoucherCustomException {
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(FinanceConsumerTestFixtures.mdmsPayload());

        microServiceUtil.getBusinessService(
                FinanceConsumerTestFixtures.TENANT_ID, "PT,WT", requestInfo, finSerMdms);

        assertThat(finSerMdms.getFinanceServiceMdmsData()).isNotNull();
    }
}
