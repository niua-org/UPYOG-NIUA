package org.egov.receipt.consumer.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.model.Bill;
import org.egov.receipt.consumer.model.BillAccountDetail;
import org.egov.receipt.consumer.model.BillDetail;
import org.egov.receipt.consumer.model.BusinessService;
import org.egov.receipt.consumer.model.Instrument;
import org.egov.receipt.consumer.model.InstrumentType;
import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.Tenant;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ReceiptTestFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReceiptTestFixtures() {
    }

    public static Map<String, Object> loadJsonMap(String classpathResource) {
        try (InputStream in = ReceiptTestFixtures.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture: " + classpathResource);
            }
            return MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load fixture " + classpathResource, e);
        }
    }

    public static Map<String, Object> financeMdmsPtPayload() {
        return loadJsonMap("/fixtures/finance-mdms-pt.json");
    }

    public static ReceiptReq receiptReqFromFixture() {
        try (InputStream in = ReceiptTestFixtures.class.getResourceAsStream("/fixtures/receipt-req-pt.json")) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture: receipt-req-pt.json");
            }
            return MAPPER.readValue(in, ReceiptReq.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load receipt fixture", e);
        }
    }

    public static ReceiptReq receiptReq(String tenantId, String businessServiceCode) {
        BillDetail billDetail = new BillDetail();
        billDetail.setBusinessService(businessServiceCode);
        billDetail.setReceiptNumber("RCPT-001");

        Bill bill = new Bill();
        bill.setBillDetails(List.of(billDetail));

        Receipt receipt = new Receipt();
        receipt.setTenantId(tenantId);
        receipt.setBill(List.of(bill));

        ReceiptReq req = new ReceiptReq();
        req.setRequestInfo(new RequestInfo());
        req.setReceipt(List.of(receipt));
        return req;
    }

    public static ReceiptReq receiptReqWithInstrument(String tenantId, String businessServiceCode) {
        ReceiptReq req = receiptReq(tenantId, businessServiceCode);
        InstrumentType type = new InstrumentType();
        type.setName("Cash");
        Instrument instrument = new Instrument();
        instrument.setInstrumentType(type);
        req.getReceipt().get(0).setInstrument(instrument);
        return req;
    }

    public static ReceiptReq receiptReqForVoucherCreation(String tenantId, String businessServiceCode) {
        ReceiptReq req = receiptReqWithInstrument(tenantId, businessServiceCode);
        BillDetail billDetail = req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0);
        billDetail.setConsumerCode("PT-001");
        billDetail.setReceiptDate(1_700_000_000_000L);
        billDetail.setAmountPaid(new BigDecimal("100"));
        BillAccountDetail line = new BillAccountDetail();
        line.setTaxHeadCode("PT_TAX");
        line.setAdjustedAmount(new BigDecimal("100"));
        billDetail.setBillAccountDetails(List.of(line));
        return req;
    }

    public static BusinessService businessService(String code, boolean voucherEnabled) {
        BusinessService bs = new BusinessService();
        bs.setCode(code);
        bs.setVoucherCreationEnabled(voucherEnabled);
        bs.setFund("01");
        bs.setFunction("001");
        bs.setDepartment("DEPT");
        return bs;
    }

    public static Tenant tenant(String code) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        return tenant;
    }

    public static Map<String, Object> mdmsWithBusinessMapping(String code, boolean voucherEnabled) {
        return Map.of(
                "MdmsRes", Map.of(
                        "FinanceModule", Map.of(
                                "BusinessServiceMapping", List.of(Map.of(
                                        "code", code,
                                        "voucherCreationEnabled", voucherEnabled,
                                        "fund", "01",
                                        "function", "001",
                                        "department", "DEPT"
                                )),
                                "TaxHeadMasterGlCodeMapping", List.of(),
                                "InstrumentGLcodeMapping", List.of(),
                                "FinanceInstrumentStatusMapping", List.of(),
                                "OnlineGLCodeMapping", List.of()
                        ),
                        "BillingService", Map.of(
                                "BusinessService", List.of(Map.of(
                                        "code", code,
                                        "businessService", "Property Tax"
                                ))
                        ),
                        "tenant", Map.of(
                                "citymodule", List.of(Map.of("code", "Finance"))
                        )
                )
        );
    }
}
