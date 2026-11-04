package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.model.AppConfigValues;
import org.egov.receipt.consumer.model.BusinessService;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.TaxHeadMaster;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.model.VoucherSearchCriteria;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceImplTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private MicroServiceUtil microServiceUtil;
    @Mock
    private org.egov.receipt.consumer.repository.ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private FinanceMdmsModel mdms;

    @BeforeEach
    void wireMapper() throws VoucherCustomException {
        org.springframework.test.util.ReflectionTestUtils.setField(voucherService, "mapper", new ObjectMapper());
        mdms = new FinanceMdmsModel();
        mdms.setFinanceServiceMdmsData(ReceiptTestFixtures.financeMdmsPtPayload());
        when(propertiesManager.getErpURLBytenantId("pb.amritsar")).thenReturn("http://erp/");
        when(propertiesManager.getVoucherCreateUrl()).thenReturn("voucher/_create");
        when(propertiesManager.getVoucherCancelUrl()).thenReturn("voucher/_cancel");
        when(propertiesManager.getVoucherSearchUrl()).thenReturn("voucher/_search");
        when(propertiesManager.getVoucherSearchByRefUrl()).thenReturn("voucher/_searchbyref");
        when(propertiesManager.getManualReceiptDateConfigUrl()).thenReturn("config/manual-receipt-date");
        when(propertiesManager.getModuleIdSearchUrl()).thenReturn("module/_search");
        when(propertiesManager.getReceiptViewSourceUrl()).thenReturn("http://receipt/view");
    }

    @Test
    void isVoucherCreationEnabled_trueWhenMappingAllows() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        when(microServiceUtil.getBusinessService(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.businessService("PT", true)));

        assertThat(voucherService.isVoucherCreationEnabled(req.getReceipt().get(0), req.getRequestInfo(), mdms))
                .isTrue();
    }

    @Test
    void isVoucherCreationEnabled_falseWhenMappingDisabled() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        when(microServiceUtil.getBusinessService(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.businessService("PT", false)));

        assertThat(voucherService.isVoucherCreationEnabled(req.getReceipt().get(0), req.getRequestInfo(), mdms))
                .isFalse();
    }

    @Test
    void isTenantEnabledInFinanceModule_throwsWhenTenantMissing() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        when(microServiceUtil.getFinanceTenantList(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.tenant("pb.other")));

        assertThatThrownBy(() -> voucherService.isTenantEnabledInFinanceModule(req, mdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void isTenantEnabledInFinanceModule_trueWhenTenantPresent() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        when(microServiceUtil.getFinanceTenantList(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.tenant("pb.amritsar")));

        assertThat(voucherService.isTenantEnabledInFinanceModule(req, mdms)).isTrue();
    }

    @Test
    void createVoucher_postsToErpAndReturnsResponse() throws Exception {
        VoucherResponse apiResponse = new VoucherResponse();
        apiResponse.setVouchers(List.of(Voucher.builder().voucherNumber("VCH-1").build()));
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar"))).thenReturn(apiResponse);

        VoucherResponse result = voucherService.createVoucher(
                List.of(Voucher.builder().tenantId("pb.amritsar").build()),
                new RequestInfo(),
                "pb.amritsar");

        assertThat(result.getVouchers().get(0).getVoucherNumber()).isEqualTo("VCH-1");
        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), any(), eq("pb.amritsar"));
        assertThat(urlCaptor.getValue().toString()).isEqualTo("http://erp/voucher/_create");
    }

    @Test
    void cancelReceiptVoucher_buildsCancelRequest() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar")))
                .thenReturn(new VoucherResponse());

        voucherService.cancelReceiptVoucher(req, "pb.amritsar", Set.of("VCH-9", "VCH-10"));

        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), any(), eq("pb.amritsar"));
        assertThat(urlCaptor.getValue().toString()).isEqualTo("http://erp/voucher/_cancel");
    }

    @Test
    void getVoucherByServiceAndRefDoc_buildsQueryWithEncodedReference() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar")))
                .thenReturn(new VoucherResponse());

        voucherService.getVoucherByServiceAndRefDoc(new RequestInfo(), "pb.amritsar", "PT", "RCPT/001");

        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), any(), eq("pb.amritsar"));
        assertThat(urlCaptor.getValue().toString()).contains("servicecode=PT");
        assertThat(urlCaptor.getValue().toString()).contains("referencedocument=RCPT%2F001");
    }

    @Test
    void getVouchers_appendsSearchCriteriaToUrl() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar")))
                .thenReturn(new VoucherResponse());

        voucherService.getVouchers(
                VoucherSearchCriteria.builder().voucherNumbers(Set.of("VCH-1", "VCH-2")).build(),
                new RequestInfo(),
                "pb.amritsar");

        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), any(), eq("pb.amritsar"));
        String url = urlCaptor.getValue().toString();
        assertThat(url).contains("voucherNumbers=");
        assertThat(url).contains("VCH-1");
        assertThat(url).contains("VCH-2");
    }

    @Test
    void createReceiptVoucher_buildsLedgerAndCreatesVoucher() throws Exception {
        var req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        BusinessService bs = ReceiptTestFixtures.businessService("PT", true);
        TaxHeadMaster taxHead = new TaxHeadMaster();
        taxHead.setTaxhead("PT_TAX");
        taxHead.setGlcode("110100");
        when(microServiceUtil.getBusinessService(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(bs));
        when(microServiceUtil.getTaxHeadMasters(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(taxHead));
        when(microServiceUtil.getBusinessServiceName(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn("Property Tax");
        when(microServiceUtil.getGlcodeByInstrumentType(eq("pb.amritsar"), eq("PT"), any(), eq(mdms), eq("Cash")))
                .thenReturn("450100");
        when(serviceRequestRepository.fetchResult(any(), any(), eq("pb.amritsar"))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, StringBuilder.class).toString();
            if (url.contains("manual-receipt")) {
                return Map.of("value", "No");
            }
            if (url.contains("module")) {
                return Map.of("id", 10, "name", "Collections");
            }
            if (url.contains("voucher/_create")) {
                return Map.of("Vouchers", List.of(Map.of("voucherNumber", "VCH-NEW", "tenantId", "pb.amritsar")));
            }
            return Map.of();
        });

        VoucherResponse response = voucherService.createReceiptVoucher(req, mdms, "V1");

        assertThat(response.getVouchers().get(0).getVoucherNumber()).isEqualTo("VCH-NEW");
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(serviceRequestRepository, atLeastOnce()).fetchResult(any(), bodyCaptor.capture(), eq("pb.amritsar"));
        org.egov.receipt.consumer.model.VoucherRequest submittedRequest = bodyCaptor.getAllValues().stream()
                .filter(org.egov.receipt.consumer.model.VoucherRequest.class::isInstance)
                .map(org.egov.receipt.consumer.model.VoucherRequest.class::cast)
                .filter(request -> request.getVouchers() != null && !request.getVouchers().isEmpty())
                .findFirst()
                .orElseThrow();
        Voucher submitted = submittedRequest.getVouchers().get(0);
        assertThat(submitted.getLedgers()).hasSize(2);
        assertThat(submitted.getReferenceDocument()).isEqualTo("RCPT-001");
    }

}
