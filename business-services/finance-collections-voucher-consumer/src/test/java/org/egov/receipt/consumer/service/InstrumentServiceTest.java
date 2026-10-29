package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.DishonorReasonContract;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.InstrumentContract;
import org.egov.receipt.consumer.model.InstrumentRequest;
import org.egov.receipt.consumer.model.InstrumentResponse;
import org.egov.receipt.consumer.model.InstrumentType;
import org.egov.receipt.consumer.model.InstrumentVoucherContract;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.v2.model.PaymentRequest;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
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

    private FinanceMdmsModel finSerMdms;

    @BeforeEach
    void setUp() throws VoucherCustomException {
        finSerMdms = FinanceConsumerTestFixtures.financeMdmsWithData();
        when(propertiesManager.getInstrumentHostUrl()).thenReturn("http://instrument/");
        when(propertiesManager.getInstrumentCreate()).thenReturn("/create");
        when(propertiesManager.getInstrumentCancel()).thenReturn("/cancel");
        when(microServiceUtil.getFinancialStatusByCode(any(), any(), any(), eq("New")))
                .thenReturn(FinanceConsumerTestFixtures.financialStatus("New"));
        when(microServiceUtil.getFinancialStatusByCode(any(), any(), eq(finSerMdms), eq("Cancelled")))
                .thenReturn(FinanceConsumerTestFixtures.financialStatus("Cancelled"));
    }

    @Test
    void createInstrument_v1_postsInstrumentWithVoucherLink() throws VoucherCustomException {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        VoucherResponse voucherResponse = FinanceConsumerTestFixtures.voucherResponse("VN-100", "0");
        InstrumentResponse apiResponse = new InstrumentResponse();
        apiResponse.setInstruments(List.of(new InstrumentContract()));
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(apiResponse);

        InstrumentResponse result = instrumentService.createInstrument(receiptReq, voucherResponse, finSerMdms, null);

        assertThat(result.getInstruments()).hasSize(1);
        verify(serviceRequestRepository).fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID));
    }

    @Test
    void createInstrument_v2_usesPaymentIdAsReceiptReference() throws VoucherCustomException {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        receiptReq.getReceipt().get(0).setPaymentId("PAY-88");
        receiptReq.getReceipt().get(0).getInstrument().setPaymentId("PAY-88");
        VoucherResponse voucherResponse = FinanceConsumerTestFixtures.voucherResponse("VN-200", "0");
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(Map.of("instruments", List.of(Map.of("id", "inst-1"))));

        assertThat(instrumentService.createInstrument(receiptReq, voucherResponse, finSerMdms, "V2")).isNotNull();
    }

    @Test
    void cancelInstrument_updatesStatusAndPostsCancel() throws VoucherCustomException {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", "VN-OLD", null);
        receiptReq.getReceipt().get(0).getInstrument().setTransactionDateInput(1_700_000_000_000L);
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(new InstrumentResponse());

        instrumentService.cancelInstrument(receiptReq, finSerMdms);

        verify(serviceRequestRepository).fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID));
    }

    @Test
    void cancelInstrumentForPayment_successWhenInstrumentIsNew() throws VoucherCustomException {
        PaymentRequest payRequest = FinanceConsumerTestFixtures.paymentRequest(null);
        InstrumentContract instrument = InstrumentContract.builder()
                .financialStatus(FinanceConsumerTestFixtures.financialStatus("New"))
                .build();
        when(microServiceUtil.getInstruments(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(List.of(instrument));
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(new InstrumentResponse());

        instrumentService.cancelInstrumentForPayment(payRequest, finSerMdms);

        verify(serviceRequestRepository).fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID));
    }

    @Test
    void cancelInstrumentForPayment_missingInstrument_throws() throws VoucherCustomException {
        PaymentRequest payRequest = FinanceConsumerTestFixtures.paymentRequest(null);
        when(microServiceUtil.getInstruments(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> instrumentService.cancelInstrumentForPayment(payRequest, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("not exist");
    }

    @Test
    void cancelInstrumentForPayment_wrongStatus_throws() throws VoucherCustomException {
        PaymentRequest payRequest = FinanceConsumerTestFixtures.paymentRequest(null);
        InstrumentContract instrument = InstrumentContract.builder()
                .financialStatus(FinanceConsumerTestFixtures.financialStatus("Cancelled"))
                .build();
        when(microServiceUtil.getInstruments(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(List.of(instrument));

        assertThatThrownBy(() -> instrumentService.cancelInstrumentForPayment(payRequest, finSerMdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("already in");
    }

    @Test
    void processDishonorIntruments_createsReversalAndUpdatesInstrument() throws Exception {
        InstrumentContract contract = InstrumentContract.builder()
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .instrumentType(InstrumentType.builder().name("Cheque").build())
                .instrumentVouchers(new ArrayList<>(List.of(InstrumentVoucherContract.builder().voucherHeaderId("VH-1").build())))
                .dishonor(DishonorReasonContract.builder().dishonorDate(FinanceConsumerTestFixtures.RECEIPT_DATE).build())
                .build();
        InstrumentRequest request = new InstrumentRequest();
        request.setInstruments(List.of(contract));
        request.setRequestInfo(FinanceConsumerTestFixtures.requestInfo());

        when(voucherService.processReversalVoucher(any(), any()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("REV-1", "0"));
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(new InstrumentResponse());

        instrumentService.processDishonorIntruments(request);

        assertThat(contract.getDishonor().getReversalVoucherId()).isEqualTo("REV-1");
    }
}
