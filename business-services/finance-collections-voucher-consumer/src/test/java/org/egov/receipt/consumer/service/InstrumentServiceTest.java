package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.FinancialStatus;
import org.egov.receipt.consumer.model.InstrumentContract;
import org.egov.receipt.consumer.model.InstrumentResponse;
import org.egov.receipt.consumer.model.InstrumentVoucherContract;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.consumer.v2.model.Payment;
import org.egov.receipt.consumer.v2.model.PaymentRequest;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstrumentServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @Mock
    private MicroServiceUtil microServiceUtil;
    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private InstrumentService instrumentService;

    private FinanceMdmsModel mdms;

    @BeforeEach
    void stubHosts() {
        mdms = new FinanceMdmsModel();
        mdms.setFinanceServiceMdmsData(ReceiptTestFixtures.financeMdmsPtPayload());
        when(propertiesManager.getInstrumentHostUrl()).thenReturn("http://instrument/");
        when(propertiesManager.getInstrumentCreate()).thenReturn("instrument/_create");
        when(propertiesManager.getInstrumentCancel()).thenReturn("instrument/_cancel");
    }

    @Test
    void createInstrument_validReceipt_buildsRequestAndReturnsResponse() throws Exception {
        ReceiptReq receiptReq = ReceiptTestFixtures.receiptReqWithInstrument("pb.amritsar", "PT");
        FinancialStatus status = financialStatus("New");
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), eq("New"))).thenReturn(status);

        InstrumentResponse apiResponse = new InstrumentResponse();
        apiResponse.setInstruments(Collections.emptyList());
        when(serviceRequestRepository.fetchResult(any(), any(), anyString())).thenReturn(apiResponse);

        InstrumentResponse response = instrumentService.createInstrument(receiptReq, null, mdms, "V1");

        assertThat(response).isNotNull();
    }

    @Test
    void createInstrument_withVoucher_attachesInstrumentVouchers() throws Exception {
        ReceiptReq receiptReq = ReceiptTestFixtures.receiptReqWithInstrument("pb.amritsar", "PT");
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), eq("New")))
                .thenReturn(financialStatus("New"));
        VoucherResponse voucherResponse = new VoucherResponse();
        voucherResponse.setVouchers(List.of(Voucher.builder().voucherNumber("VCH-200").build()));
        when(serviceRequestRepository.fetchResult(any(), any(), anyString())).thenReturn(new InstrumentResponse());

        instrumentService.createInstrument(receiptReq, voucherResponse, mdms, "V1");

        ArgumentCaptor<org.egov.receipt.consumer.model.InstrumentRequest> requestCaptor =
                ArgumentCaptor.forClass(org.egov.receipt.consumer.model.InstrumentRequest.class);
        verify(serviceRequestRepository).fetchResult(any(), requestCaptor.capture(), eq("pb.amritsar"));
        InstrumentVoucherContract iv = requestCaptor.getValue().getInstruments().get(0).getInstrumentVouchers().get(0);
        assertThat(iv.getVoucherHeaderId()).isEqualTo("VCH-200");
        assertThat(iv.getReceiptHeaderId()).isEqualTo("RCPT-001");
    }

    @Test
    void cancelInstrument_setsCancelledStatusAndCallsCancelEndpoint() throws Exception {
        ReceiptReq receiptReq = ReceiptTestFixtures.receiptReqFromFixture();
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), eq("Cancelled")))
                .thenReturn(financialStatus("Cancelled"));
        when(serviceRequestRepository.fetchResult(any(), any(), anyString())).thenReturn(new InstrumentResponse());

        instrumentService.cancelInstrument(receiptReq, mdms);

        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), any(), eq("pb.amritsar"));
        assertThat(urlCaptor.getValue().toString()).isEqualTo("http://instrument/instrument/_cancel");
    }

    @Test
    void cancelInstrumentForPayment_cancelsWhenInstrumentIsNew() throws Exception {
        FinancialStatus newStatus = financialStatus("New");
        newStatus.setId("New");
        FinancialStatus cancelStatus = financialStatus("Cancelled");
        InstrumentContract instrument = InstrumentContract.builder()
                .financialStatus(newStatus)
                .tenantId("pb.amritsar")
                .build();
        when(microServiceUtil.getFinancialStatusByCode(eq("pb.amritsar"), any(), eq(mdms), eq("Cancelled")))
                .thenReturn(cancelStatus);
        when(microServiceUtil.getFinancialStatusByCode(eq("pb.amritsar"), any(), eq(mdms), eq("New")))
                .thenReturn(newStatus);
        when(microServiceUtil.getInstruments(any(), any(), eq("pb.amritsar"))).thenReturn(List.of(instrument));
        when(serviceRequestRepository.fetchResult(any(), any(), anyString())).thenReturn(new InstrumentResponse());

        PaymentRequest payRequest = new PaymentRequest();
        payRequest.setRequestInfo(receiptReqFromFixture().getRequestInfo());
        Payment payment = new Payment();
        payment.setId("pay-001");
        payment.setTenantId("pb.amritsar");
        payRequest.setPayment(payment);

        instrumentService.cancelInstrumentForPayment(payRequest, mdms);

        verify(serviceRequestRepository).fetchResult(any(), any(), eq("pb.amritsar"));
    }

    @Test
    void cancelInstrumentForPayment_throwsWhenInstrumentMissing() throws VoucherCustomException {
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), eq("Cancelled")))
                .thenReturn(financialStatus("Cancelled"));
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), eq("New")))
                .thenReturn(financialStatus("New"));
        when(microServiceUtil.getInstruments(any(), any(), anyString())).thenReturn(Collections.emptyList());

        PaymentRequest payRequest = new PaymentRequest();
        payRequest.setRequestInfo(new org.egov.receipt.consumer.model.RequestInfo());
        Payment payment = new Payment();
        payment.setId("pay-missing");
        payment.setTenantId("pb.amritsar");
        payRequest.setPayment(payment);

        assertThatThrownBy(() -> instrumentService.cancelInstrumentForPayment(payRequest, mdms))
                .isInstanceOf(VoucherCustomException.class)
                .extracting("status")
                .isEqualTo(ProcessStatus.FAILED);
    }

    private static FinancialStatus financialStatus(String code) {
        FinancialStatus status = new FinancialStatus();
        status.setCode(code);
        status.setId(code);
        return status;
    }

    private static ReceiptReq receiptReqFromFixture() {
        return ReceiptTestFixtures.receiptReqFromFixture();
    }
}
