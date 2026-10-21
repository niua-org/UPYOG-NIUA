package org.egov.receipt.consumer.support;

import org.egov.receipt.consumer.model.Bill;
import org.egov.receipt.consumer.model.BillDetail;
import org.egov.receipt.consumer.model.BusinessService;
import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.Tenant;

import java.util.List;
import java.util.Map;

public final class ReceiptTestFixtures {

    private ReceiptTestFixtures() {
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

    public static BusinessService businessService(String code, boolean voucherEnabled) {
        BusinessService bs = new BusinessService();
        bs.setCode(code);
        bs.setVoucherCreationEnabled(voucherEnabled);
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
                                "TaxHeadMasterGlCodeMapping", List.of()
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
