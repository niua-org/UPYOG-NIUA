package org.egov.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.InstrumentContract;
import org.egov.receipt.consumer.model.InstrumentResponse;
import org.egov.receipt.consumer.model.InstrumentSearchContract;
import org.egov.receipt.consumer.model.MdmsCriteria;
import org.egov.receipt.consumer.model.MdmsCriteriaReq;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.TaxHeadMaster;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicroServiceUtilImplTest {

    @Mock
    private org.egov.reciept.consumer.config.PropertiesManager manager;
    @Mock
    private org.egov.receipt.consumer.repository.ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private MicroServiceUtilImpl microServiceUtil;

    private FinanceMdmsModel mdmsWithPtPayload;

    @BeforeEach
    void wireMapperAndMdms() {
        org.springframework.test.util.ReflectionTestUtils.setField(microServiceUtil, "mapper", new ObjectMapper());
        org.springframework.test.util.ReflectionTestUtils.setField(microServiceUtil, "mdmscriteria", new MdmsCriteria());
        org.springframework.test.util.ReflectionTestUtils.setField(microServiceUtil, "mdmsrequest", new MdmsCriteriaReq());
        mdmsWithPtPayload = new FinanceMdmsModel();
        mdmsWithPtPayload.setFinanceServiceMdmsData(ReceiptTestFixtures.financeMdmsPtPayload());
    }

    @Test
    void getBusinessService_readsPreloadedMdmsPayload() throws VoucherCustomException {
        var services = microServiceUtil.getBusinessService("pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload);

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getCode()).isEqualTo("PT");
        assertThat(services.get(0).isVoucherCreationEnabled()).isTrue();
    }

    @Test
    void getBusinessServiceName_readsBillingServiceMaster() throws VoucherCustomException {
        String name = microServiceUtil.getBusinessServiceName("pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload);

        assertThat(name).isEqualTo("Property Tax");
    }

    @Test
    void getTaxHeadMasters_readsGlCodeMapping() throws VoucherCustomException {
        List<TaxHeadMaster> masters =
                microServiceUtil.getTaxHeadMasters("pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload);

        assertThat(masters).hasSize(1);
        assertThat(masters.get(0).getTaxhead()).isEqualTo("PT_TAX");
        assertThat(masters.get(0).getGlcode()).isEqualTo("110100");
    }

    @Test
    void getFinancialStatusByCode_resolvesNewAndCancelled() throws VoucherCustomException {
        var newStatus = microServiceUtil.getFinancialStatusByCode(
                "pb.amritsar", new RequestInfo(), mdmsWithPtPayload, "New");
        var cancelled = microServiceUtil.getFinancialStatusByCode(
                "pb.amritsar", new RequestInfo(), mdmsWithPtPayload, "Cancelled");

        assertThat(newStatus.getCode()).isEqualTo("New");
        assertThat(cancelled.getCode()).isEqualTo("Cancelled");
    }

    @Test
    void getGlcodeByInstrumentType_resolvesCashAndOnline() throws VoucherCustomException {
        String cashGl = microServiceUtil.getGlcodeByInstrumentType(
                "pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload, "Cash");
        String onlineGl = microServiceUtil.getGlcodeByInstrumentType(
                "pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload, "Online");

        assertThat(cashGl).isEqualTo("450100");
        assertThat(onlineGl).isEqualTo("450200");
    }

    @Test
    void getFinanceTenantList_includesConfiguredTenant() throws VoucherCustomException {
        var tenants = microServiceUtil.getFinanceTenantList("pb.amritsar", "PT", new RequestInfo(), mdmsWithPtPayload);

        assertThat(tenants).extracting("code").containsExactly("pb.amritsar");
    }

    @Test
    void getFinanceServiceMdmsData_fetchesAndStoresPayload() throws VoucherCustomException {
        FinanceMdmsModel empty = new FinanceMdmsModel();
        Map<String, Object> payload = ReceiptTestFixtures.financeMdmsPtPayload();
        when(manager.getMdmsHostUrl()).thenReturn("http://mdms/");
        when(manager.getMdmsSearchUrl()).thenReturn("mdms/_search");
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar"))).thenReturn(payload);

        microServiceUtil.getFinanceServiceMdmsData("pb.amritsar", "PT", new RequestInfo(), empty);

        assertThat(empty.getFinanceServiceMdmsData()).isEqualTo(payload);
        verify(serviceRequestRepository).fetchResult(any(), any(), eq("pb.amritsar"));
    }

    @Test
    void getBusinessService_throwsWhenMdmsMappingInvalid() {
        FinanceMdmsModel broken = new FinanceMdmsModel();
        broken.setFinanceServiceMdmsData(Map.of("MdmsRes", "not-an-object"));

        assertThatThrownBy(() -> microServiceUtil.getBusinessService("pb.amritsar", "PT", new RequestInfo(), broken))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("Error while parsing mdms data");
    }

    @Test
    void getInstruments_buildsSearchUrlAndReturnsContracts() throws VoucherCustomException {
        InstrumentContract contract = InstrumentContract.builder().id("inst-1").tenantId("pb.amritsar").build();
        InstrumentResponse response = new InstrumentResponse();
        response.setInstruments(Collections.singletonList(contract));
        when(manager.getInstrumentHostUrl()).thenReturn("http://instrument/");
        when(manager.getInstrumentSearch()).thenReturn("instrument/_search");
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar"))).thenReturn(response);

        List<InstrumentContract> instruments = microServiceUtil.getInstruments(
                InstrumentSearchContract.builder().receiptIds("pay-001").build(),
                new RequestInfo(),
                "pb.amritsar");

        assertThat(instruments).hasSize(1);
        assertThat(instruments.get(0).getId()).isEqualTo("inst-1");
    }
}
