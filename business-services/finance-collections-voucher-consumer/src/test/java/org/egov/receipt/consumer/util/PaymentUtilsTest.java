package org.egov.receipt.consumer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.BillAccountDetail;
import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.v2.model.BillDetailV2;
import org.egov.receipt.consumer.v2.model.BillV2;
import org.egov.receipt.consumer.v2.model.Payment;
import org.egov.receipt.consumer.v2.model.PaymentDetail;
import org.egov.receipt.consumer.v2.model.PaymentModeEnum;
import org.egov.receipt.custom.exception.VoucherCustomException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentUtilsTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private PaymentUtils paymentUtils;

    @BeforeEach
    void stubIfscLookup() throws VoucherCustomException {
        Map<String, Object> bankPayload = new HashMap<>();
        bankPayload.put("BANK", "State Bank");
        bankPayload.put("BANKCODE", "SBIN");
        bankPayload.put("BRANCH", "Main");
        when(serviceRequestRepository.fetchResultGet(anyString())).thenReturn(bankPayload);
    }

    @Test
    void getReceiptsFromPayments_mapsCashPaymentAndAggregatesByBusinessService() throws VoucherCustomException {
        Payment payment = cashPayment(PaymentModeEnum.CASH, null);
        PaymentDetail detail1 = paymentDetail("pd-1", "RCPT-1", BigDecimal.TEN);
        PaymentDetail detail2 = paymentDetail("pd-2", "RCPT-2", BigDecimal.ONE);
        payment.setPaymentDetails(List.of(detail1, detail2));

        List<Receipt> receipts = new ArrayList<>();
        paymentUtils.getReceiptsFromPayments(List.of(payment), receipts);

        assertThat(receipts).hasSize(1);
        Receipt receipt = receipts.get(0);
        assertThat(receipt.getPaymentId()).isEqualTo("PAY-1");
        assertThat(receipt.getBill()).hasSize(1);
        assertThat(receipt.getBill().get(0).getBillDetails()).hasSize(1);
        assertThat(receipt.getBill().get(0).getBillDetails().get(0).getAmountPaid())
                .isEqualByComparingTo(new BigDecimal("11"));
        assertThat(receipt.getInstrument().getInstrumentType().getName()).isEqualTo("Cash");
    }

    @Test
    void getReceiptsFromPayments_ddMode_uppercasesInstrumentType() throws VoucherCustomException {
        Payment payment = cashPayment(PaymentModeEnum.DD, null);
        payment.setPaymentDetails(List.of(paymentDetail("pd-1", "RCPT-DD", BigDecimal.TEN)));

        List<Receipt> receipts = new ArrayList<>();
        paymentUtils.getReceiptsFromPayments(List.of(payment), receipts);

        assertThat(receipts.get(0).getInstrument().getInstrumentType().getName()).isEqualTo("DD");
    }

    @Test
    void getReceiptsFromPayments_withIfsc_populatesBankDetails() throws VoucherCustomException {
        Payment payment = cashPayment(PaymentModeEnum.CHEQUE, "SBIN0001234");
        payment.setPaymentDetails(List.of(paymentDetail("pd-1", "RCPT-IFSC", BigDecimal.TEN)));

        List<Receipt> receipts = new ArrayList<>();
        paymentUtils.getReceiptsFromPayments(List.of(payment), receipts);

        assertThat(receipts.get(0).getInstrument().getBank().getCode()).isEqualTo("SBIN");
        assertThat(receipts.get(0).getInstrument().getBranchName()).isEqualTo("Main");
    }

    @Test
    void getReceiptsFromPayments_aggregatesDuplicateTaxHeads() throws VoucherCustomException {
        BillAccountDetail line1 = BillAccountDetail.builder()
                .taxHeadCode("TAX_HEAD")
                .amount(new BigDecimal("5"))
                .adjustedAmount(new BigDecimal("5"))
                .build();
        BillAccountDetail line2 = BillAccountDetail.builder()
                .taxHeadCode("TAX_HEAD")
                .amount(new BigDecimal("3"))
                .adjustedAmount(new BigDecimal("2"))
                .build();
        BillDetailV2 billDetail = BillDetailV2.builder()
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .amountPaid(new BigDecimal("8"))
                .billAccountDetails(new ArrayList<>(List.of(line1, line2)))
                .build();
        BillV2 bill = billWithDetails(List.of(billDetail));
        PaymentDetail detail1 = PaymentDetail.builder()
                .id("pd-a")
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .businessService(FinanceConsumerTestFixtures.BS_CODE)
                .receiptNumber("RCPT-A")
                .receiptDate(FinanceConsumerTestFixtures.RECEIPT_DATE)
                .bill(bill)
                .build();
        PaymentDetail detail2 = PaymentDetail.builder()
                .id("pd-b")
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .businessService(FinanceConsumerTestFixtures.BS_CODE)
                .receiptNumber("RCPT-B")
                .receiptDate(FinanceConsumerTestFixtures.RECEIPT_DATE)
                .bill(bill)
                .build();
        Payment payment = cashPayment(PaymentModeEnum.CASH, null);
        payment.setPaymentDetails(List.of(detail1, detail2));

        List<Receipt> receipts = new ArrayList<>();
        paymentUtils.getReceiptsFromPayments(List.of(payment), receipts);

        List<BillAccountDetail> merged = receipts.get(0).getBill().get(0).getBillDetails().get(0).getBillAccountDetails();
        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("16"));
        assertThat(merged.get(0).getAdjustedAmount()).isEqualByComparingTo(new BigDecimal("14"));
    }

    @Test
    void getReceiptsFromPayments_ifscLookupFailure_propagates() throws VoucherCustomException {
        when(serviceRequestRepository.fetchResultGet(anyString()))
                .thenThrow(new VoucherCustomException(null, "invalid ifsc"));
        Payment payment = cashPayment(PaymentModeEnum.CHEQUE, "BADIFSC");
        payment.setPaymentDetails(List.of(paymentDetail("pd-1", "RCPT-X", BigDecimal.TEN)));

        assertThatThrownBy(() -> paymentUtils.getReceiptsFromPayments(List.of(payment), new ArrayList<>()))
                .isInstanceOf(VoucherCustomException.class);
    }

    private static Payment cashPayment(PaymentModeEnum mode, String ifsc) {
        return Payment.builder()
                .id("PAY-1")
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .totalAmountPaid(BigDecimal.TEN)
                .transactionDate(FinanceConsumerTestFixtures.RECEIPT_DATE)
                .paymentMode(mode)
                .ifscCode(ifsc)
                .paidBy("citizen")
                .mobileNumber("9999999999")
                .build();
    }

    private static PaymentDetail paymentDetail(String id, String receiptNumber, BigDecimal amount) {
        BillDetailV2 billDetail = BillDetailV2.builder()
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .amountPaid(amount)
                .billAccountDetails(new ArrayList<>(List.of(BillAccountDetail.builder()
                        .taxHeadCode("TAX_HEAD")
                        .amount(amount)
                        .adjustedAmount(amount)
                        .build())))
                .build();
        BillV2 bill = billWithDetails(List.of(billDetail));
        return PaymentDetail.builder()
                .id(id)
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .businessService(FinanceConsumerTestFixtures.BS_CODE)
                .receiptNumber(receiptNumber)
                .receiptDate(FinanceConsumerTestFixtures.RECEIPT_DATE)
                .bill(bill)
                .build();
    }

    private static BillV2 billWithDetails(List<BillDetailV2> details) {
        return BillV2.builder()
                .tenantId(FinanceConsumerTestFixtures.TENANT_ID)
                .consumerCode("CC-001")
                .businessService(FinanceConsumerTestFixtures.BS_CODE)
                .status(BillV2.StatusEnum.PAID)
                .billDetails(details)
                .build();
    }
}
