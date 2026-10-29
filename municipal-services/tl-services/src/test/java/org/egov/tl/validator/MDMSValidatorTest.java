package org.egov.tl.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.TLConstants;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tl.web.models.TradeUnit;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private TradeUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private MDMSValidator mdmsValidator;

    @Test
    void validateMdmsData_whenOwnershipCategoryInvalid_throws() {
        TradeLicenseRequest request = tlRequest("UNKNOWN", "PERMANENT", "SHOP", "NOS");
        Object mdmsData = sampleMdms();
        Object billingSlabs = sampleBillingSlabs();

        assertThatThrownBy(() -> mdmsValidator.validateMdmsData(request, mdmsData, billingSlabs))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID OWNERSHIPCATEGORY");
    }

    @Test
    void validateMdmsData_whenValidTlRequest_passes() {
        TradeLicenseRequest request = tlRequest("INDIVIDUAL", "PERMANENT", "SHOP", "NOS");
        mdmsValidator.validateMdmsData(request, sampleMdms(), sampleBillingSlabs());
    }

    @Test
    void validateMdmsData_whenUomValueMissingForRequiredUom_throws() {
        TradeUnit unit = TradeUnit.builder().tradeType("SHOP").uom("NOS").uomValue(null).build();
        TradeLicenseDetail detail = TradeLicenseDetail.builder()
                .subOwnerShipCategory("INDIVIDUAL")
                .structureType("PERMANENT")
                .tradeUnits(List.of(unit))
                .build();
        TradeLicense license = TradeLicense.builder().businessService("TL").tradeLicenseDetail(detail).build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        assertThatThrownBy(() -> mdmsValidator.validateMdmsData(request, sampleMdms(), sampleBillingSlabs()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("uomValue cannot be null");
    }

    private static TradeLicenseRequest tlRequest(
            String ownership, String structureType, String tradeType, String uom) {
        TradeUnit unit = TradeUnit.builder()
                .tradeType(tradeType)
                .uom(uom)
                .uomValue("1")
                .build();
        TradeLicenseDetail detail = TradeLicenseDetail.builder()
                .subOwnerShipCategory(ownership)
                .structureType(structureType)
                .tradeUnits(List.of(unit))
                .build();
        TradeLicense license = TradeLicense.builder().businessService("TL").tradeLicenseDetail(detail).build();
        return TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();
    }

    private static Map<String, Object> sampleMdms() {
        Map<String, Object> tradeLicense = new HashMap<>();
        tradeLicense.put(TLConstants.ACCESSORIES_CATEGORY, List.of("AC1"));
        tradeLicense.put(TLConstants.TRADE_TYPE, List.of("SHOP"));
        Map<String, Object> commonMasters = new HashMap<>();
        commonMasters.put(TLConstants.OWNERSHIP_CATEGORY, List.of("INDIVIDUAL"));
        commonMasters.put(TLConstants.STRUCTURE_TYPE, List.of("PERMANENT"));
        Map<String, Object> mdmsRes = new HashMap<>();
        mdmsRes.put("TradeLicense", tradeLicense);
        mdmsRes.put("common-masters", commonMasters);
        return Map.of("MdmsRes", mdmsRes);
    }

    private static Map<String, Object> sampleBillingSlabs() {
        return Map.of(
                "billingSlab",
                List.of(
                        Map.of("tradeType", "SHOP", "uom", "NOS"),
                        Map.of("accessoryCategory", "AC1", "uom", "NOS")));
    }
}
