package org.egov.receipt.consumer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.receipt.consumer.model.Bill;
import org.egov.receipt.consumer.model.BillAccountDetail;
import org.egov.receipt.consumer.model.BillDetail;
import org.egov.receipt.consumer.model.BusinessService;
import org.egov.receipt.consumer.model.EgwStatus;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.FinancialStatus;
import org.egov.receipt.consumer.model.Instrument;
import org.egov.receipt.consumer.model.InstrumentType;
import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.TaxHeadMaster;
import org.egov.receipt.consumer.model.Tenant;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.v2.model.BillDetailV2;
import org.egov.receipt.consumer.v2.model.BillV2;
import org.egov.receipt.consumer.v2.model.Payment;
import org.egov.receipt.consumer.v2.model.PaymentDetail;
import org.egov.receipt.consumer.v2.model.PaymentModeEnum;
import org.egov.receipt.consumer.v2.model.PaymentRequest;
import org.egov.receipt.consumer.v2.model.PaymentStatusEnum;

public final class FinanceConsumerTestFixtures {

    public static final String TENANT_ID = "pg.citya";
    public static final String BS_CODE = "PT";
    public static final long RECEIPT_DATE = 1_700_000_000_000L;

    private FinanceConsumerTestFixtures() {
    }

    public static RequestInfo requestInfo() {
        return new RequestInfo();
    }

    public static ReceiptReq receiptReq(String receiptNumber, String voucherHeader, EgwStatus voucherStatus) {
        BillAccountDetail line = BillAccountDetail.builder()
                .taxHeadCode("TAX_HEAD")
                .adjustedAmount(BigDecimal.TEN)
                .amount(BigDecimal.TEN)
                .build();
        BillDetail billDetail = new BillDetail();
        billDetail.setBusinessService(BS_CODE);
        billDetail.setReceiptNumber(receiptNumber);
        billDetail.setConsumerCode("CC-001");
        billDetail.setReceiptDate(RECEIPT_DATE);
        billDetail.setAmountPaid(BigDecimal.TEN);
        billDetail.setVoucherHeader(voucherHeader);
        billDetail.setBillAccountDetails(new ArrayList<>(List.of(line)));

        Bill bill = Bill.builder().billDetails(List.of(billDetail)).build();
        Instrument instrument = Instrument.builder()
                .tenantId(TENANT_ID)
                .amount(BigDecimal.TEN)
                .instrumentType(InstrumentType.builder().name("Cash").build())
                .build();
        Receipt receipt = Receipt.builder()
                .tenantId(TENANT_ID)
                .bill(List.of(bill))
                .instrument(instrument)
                .build();
        return ReceiptReq.builder().requestInfo(requestInfo()).receipt(List.of(receipt)).build();
    }

    public static VoucherResponse voucherResponse(String voucherNumber, String statusCode) {
        Voucher voucher = Voucher.builder()
                .voucherNumber(voucherNumber)
                .status(EgwStatus.builder().code(statusCode).build())
                .build();
        return VoucherResponse.builder().vouchers(new ArrayList<>(List.of(voucher))).build();
    }

    public static FinancialStatus financialStatus(String code) {
        return FinancialStatus.builder().id(code).code(code).build();
    }

    public static Map<String, Object> mdmsPayload() {
        Map<String, Object> businessMapping = Map.of("code", BS_CODE, "voucherCreationEnabled", true);
        Map<String, Object> taxHead = Map.of("billingservicecode", BS_CODE, "taxhead", "TAX_HEAD", "glcode", "450001");
        Map<String, Object> instrumentGl = Map.of("instrumenttype", "Cash", "glcode", "110001");
        Map<String, Object> onlineGl = Map.of("servicecode", BS_CODE, "glcode", "220001");
        Map<String, Object> finStatus = Map.of("code", "New", "id", "New");
        Map<String, Object> billingBs = Map.of("code", BS_CODE, "businessService", "Property Tax");
        Map<String, Object> tenant = Map.of("code", TENANT_ID);

        Map<String, Object> financeModule = new HashMap<>();
        financeModule.put("BusinessServiceMapping", List.of(businessMapping));
        financeModule.put("TaxHeadMasterGlCodeMapping", List.of(taxHead));
        financeModule.put("InstrumentGLcodeMapping", List.of(instrumentGl));
        financeModule.put("FinanceInstrumentStatusMapping", List.of(finStatus));
        financeModule.put("OnlineGLCodeMapping", List.of(onlineGl));

        Map<String, Object> billingService = Map.of("BusinessService", List.of(billingBs));
        Map<String, Object> cityModule = Map.of("tenants", List.of(tenant));
        Map<String, Object> tenantModule = Map.of("citymodule", List.of(cityModule));
        Map<String, Object> mdmsRes = Map.of(
                "FinanceModule", financeModule,
                "BillingService", billingService,
                "tenant", tenantModule);
        return Map.of("MdmsRes", mdmsRes);
    }

    public static FinanceMdmsModel financeMdmsWithData() {
        FinanceMdmsModel model = new FinanceMdmsModel();
        model.setFinanceServiceMdmsData(mdmsPayload());
        return model;
    }

    public static BusinessService businessService() {
        BusinessService bs = new BusinessService();
        bs.setCode(BS_CODE);
        bs.setVoucherCreationEnabled(true);
        return bs;
    }

    public static List<TaxHeadMaster> taxHeadMasters() {
        return List.of(new TaxHeadMaster(BS_CODE, "TAX_HEAD", "450001", null, null, null));
    }

    public static List<Tenant> financeTenants() {
        return List.of(new Tenant(TENANT_ID));
    }

    public static PaymentRequest paymentRequest(PaymentStatusEnum status) {
        BillAccountDetail account = BillAccountDetail.builder()
                .taxHeadCode("TAX_HEAD")
                .amount(BigDecimal.TEN)
                .adjustedAmount(BigDecimal.TEN)
                .build();
        BillDetailV2 billDetail = BillDetailV2.builder()
                .tenantId(TENANT_ID)
                .amountPaid(BigDecimal.TEN)
                .billAccountDetails(List.of(account))
                .build();
        BillV2 bill = BillV2.builder()
                .tenantId(TENANT_ID)
                .consumerCode("CC-001")
                .businessService(BS_CODE)
                .status(BillV2.StatusEnum.PAID)
                .billDetails(List.of(billDetail))
                .build();
        PaymentDetail paymentDetail = PaymentDetail.builder()
                .id("pd-1")
                .tenantId(TENANT_ID)
                .businessService(BS_CODE)
                .receiptNumber("RCPT-PAY-1")
                .receiptDate(RECEIPT_DATE)
                .bill(bill)
                .build();
        Payment payment = Payment.builder()
                .id("PAY-1")
                .tenantId(TENANT_ID)
                .totalAmountPaid(BigDecimal.TEN)
                .transactionDate(RECEIPT_DATE)
                .paymentMode(PaymentModeEnum.CASH)
                .paidBy("citizen")
                .mobileNumber("9999999999")
                .paymentStatus(status)
                .paymentDetails(List.of(paymentDetail))
                .build();
        PaymentRequest request = new PaymentRequest();
        request.setRequestInfo(requestInfo());
        request.setPayment(payment);
        return request;
    }
}
