package org.egov.garbageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.web.models.garbage.AmountCalculationResult;
import org.egov.garbageservice.web.models.garbage.GarbageApplication;
import org.egov.garbageservice.util.MdmsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for calculating garbage collection fees and applicable rebates.
 *
 * <p>This service interacts with the Master Data Management Service (MDMS) to retrieve
 * dynamic fee configurations and calculates the final payable amount for a garbage application.
 */
@Service
public class GarbageCalculationService {

    @Autowired
    private MdmsService mdmsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MdmsUtil mdmsUtil;

    /**
     * Calculates the billing amount for the given garbage application by retrieving
     * the applicable monthly fee from MDMS and applying any eligible rebate.
     *
     * @param requestInfo the request info containing authentication
     * @param application the garbage application for which the billing amount is calculated
     * @return an {@link AmountCalculationResult} containing the monthly amount,
     * rebate amount, and final payable amount
     */
    public AmountCalculationResult calculateAmount(RequestInfo requestInfo, GarbageApplication application) {
        if (requestInfo == null) {
            requestInfo = new RequestInfo();
        }
        Object mdmsResponse = mdmsService.fetchGarbageFeeFromMdms(requestInfo, application.getTenantId());
        List<String> errorList = new ArrayList<>();
        ObjectNode calculationBreakdown = objectMapper.createObjectNode();
        BigDecimal monthlyAmount = mdmsService.fetchGarbageAmountFromMDMSResponse(
                mdmsResponse, application, errorList, calculationBreakdown
        );
        if (monthlyAmount == null) {
            monthlyAmount = BigDecimal.ZERO;
        }

        String specialCategory = null;
        if (!CollectionUtils.isEmpty(application.getGrbgCollectionUnits())
                && application.getGrbgCollectionUnits().get(0) != null) {
            specialCategory = application.getGrbgCollectionUnits().get(0).getSpecialCategory();
        }

        BigDecimal rebateRate = mdmsUtil.getRebateRate(requestInfo, application.getTenantId(), specialCategory);
        if (rebateRate == null) {
            rebateRate = BigDecimal.ZERO;
        }
        BigDecimal rebateAmount = monthlyAmount.multiply(rebateRate);
        BigDecimal payableAmount = monthlyAmount.subtract(rebateAmount);

        return new AmountCalculationResult(monthlyAmount, rebateAmount, payableAmount);
    }
}
