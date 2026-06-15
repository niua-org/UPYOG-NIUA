package upyog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import upyog.web.models.AllotmentRequest;
import upyog.web.models.billing.DemandDetail;

@Service
@Slf4j
public class CalculationService {

    public List<DemandDetail> calculateDemand(AllotmentRequest allotmentRequest, List<Map<String, Object>> taxRateList) {
        List<DemandDetail> demandDetails = new ArrayList<>();
        String tenantId = allotmentRequest.getAllotments().get(0).getTenantId();

        for (Map<String, Object> taxRate : taxRateList) {
            String feeType = (String) taxRate.get("feeType");
            Object amountObj = taxRate.get("amount");

            BigDecimal amount;
            if (amountObj instanceof Integer) {
                amount = BigDecimal.valueOf((Integer) amountObj);
            } else if (amountObj instanceof Double) {
                amount = BigDecimal.valueOf((Double) amountObj);
            } else {
                amount = new BigDecimal(amountObj.toString());
            }

            DemandDetail demandDetail = DemandDetail.builder()
                    .taxHeadMasterCode(feeType)
                    .taxAmount(amount)
                    .collectionAmount(BigDecimal.ZERO)
                    .tenantId(tenantId)
                    .additionalDetails(null)
                    .build();

            demandDetails.add(demandDetail);
            log.info("Added demand detail - TaxHead: {}, Amount: {}", feeType, amount);
        }

        return demandDetails;
    }
}
