package org.egov.garbageservice.web.models.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.web.models.BillRequest;
import org.egov.garbageservice.util.RequestInfoWrapper;
import org.egov.garbageservice.util.RestCallRepository;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Client repository that calls the external billing/collection service for bill operations.
 * <p>
 * Behavior:
 * - {@link #fetchBill(GenerateBillCriteria, RequestInfo)} — GET fetch/generate bill by consumer and tenant.
 * - {@link #searchBill(BillSearchCriteria, RequestInfo)} — GET search bills; returns list of {@link Bill}.
 * - {@link #updateBill(RequestInfo, List)} — POST updated bill payloads; returns parsed bills.
 * - {@link #cancelBill(UpdateBillCriteria, RequestInfo)} — POST cancel/update via {@link UpdateBillRequest}.
 * - Builds URLs from {@link GrbgConstants}, invokes {@link RestCallRepository}, maps JSON with {@link ObjectMapper}.
 * <p>
 * Notes:
 * - Does not persist bills locally; all data lives in the billing service.
 * - Parsing failures throw {@link CustomException} with PARSING ERROR.
 * - cancelBill logs exceptions but does not rethrow — callers may not see remote failures.
 */
@Repository
@Slf4j
public class BillRepository {


    @Autowired
    private RestCallRepository restCallRepository;

    @Autowired
    private GarbageServiceConfig garbageServiceConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Generates a new bill synchronously by interacting with the billing service.
     *
     * @param requestInfo  the contextual request containing user/tenant info
     * @param billCriteria the criteria defining the bill to generate
     * @return the generated {@link BillResponse} containing the bill details
     */

    public BillResponse fetchBill(GenerateBillCriteria billCriteria, RequestInfo requestInfo) {
        String uri = garbageServiceConfig.getBillingHost().concat(garbageServiceConfig.getFetchBillEndpoint());
        uri = uri.concat("?consumerCode=").concat(billCriteria.getConsumerCode());
        uri = uri.concat("&tenantId=").concat(billCriteria.getTenantId());
        uri = uri.concat("&businessService=").concat(billCriteria.getBusinessService());
        if (billCriteria.getMobileNumber() != null)
            uri = uri.concat("&mobileNumber=").concat(billCriteria.getMobileNumber());
        if (billCriteria.getDemandId() != null)
            uri = uri.concat("&demandId=").concat(billCriteria.getDemandId());
        if (null != billCriteria.getEmail()) {
            uri = uri.concat("&email=").concat(billCriteria.getEmail());
        }

        Object result = restCallRepository.fetchResult(new StringBuilder(uri), RequestInfoWrapper.builder()
                .requestInfo(requestInfo).build());
        BillResponse response = null;
        try {
            response = objectMapper.convertValue(result, BillResponse.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException("PARSING ERROR", "Unable to parse response of generate bill");
        }

        return response;
    }



}
