package org.egov.dx.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;
import org.egov.dx.adapter.vendor.gst.GstGatewayAdapter;
import org.egov.dx.service.GstProfileService;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.GstConstants;
import org.egov.dx.util.GstNumberUtils;
import org.egov.dx.validator.GstVerificationValidator;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;
import org.egov.dx.web.models.GstTaxpayerDetails;
import org.egov.dx.web.models.ResponseInfoFactory;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link GstProfileService} orchestrating validation, vendor transformation,
 * gateway execution, and domain response enrichment for GST search.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles GST domain business logic, separating it from the HTTP transport controller.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects polymorphic {@link GstGatewayAdapter} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Vendor schema changes are isolated to this service layer without polluting REST controllers.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class GstProfileServiceImpl implements GstProfileService {

    private final GstGatewayAdapter gatewayAdapter;
    private final GstVerificationValidator validator;
    private final TenantVerificationConfigService tenantConfigService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param gatewayAdapter abstraction for active vendor/mock GST gateway adapter
     * @param validator component handling request validation
     * @param tenantConfigService component checking tenant-level MDMS enablement
     */
    @Autowired
    public GstProfileServiceImpl(GstGatewayAdapter gatewayAdapter,
                                 GstVerificationValidator validator,
                                 TenantVerificationConfigService tenantConfigService) {
        this.gatewayAdapter = gatewayAdapter;
        this.validator = validator;
        this.tenantConfigService = tenantConfigService;
    }

    @Override
    public GstProfileResponse searchGstin(GstProfileRequest request) {
        // 1. Validate incoming request (SRP)
        validator.validateGstRequest(request);

        // 2. Validate tenant-level MDMS configuration
        if (!tenantConfigService.isVerificationEnabled(request.getRequestInfo(), request.getTenantId(), "GST")) {
            log.warn("GST profile search is not enabled in MDMS for tenant: {}", request.getTenantId());
            throw new CustomException(GstConstants.ERR_CODE_TENANT_DISABLED,
                    "GST profile search is not enabled for tenant: " + request.getTenantId() + " in MDMS configuration.");
        }

        String rawGstin = request.getGstin();
        String normalizedGstin = GstNumberUtils.normalizeGstin(rawGstin);
        String maskedGstin = GstNumberUtils.maskGstin(normalizedGstin);

        log.debug("Processing GST profile search in service for masked GSTIN: {}, apiId: {}",
                maskedGstin, request.getRequestInfo() != null ? request.getRequestInfo().getApiId() : "null");
        log.info("Processing GST profile search in service for masked GSTIN: {}, tenantId: {}",
                maskedGstin, request.getTenantId());

        // 2. Delegate to gateway adapter abstraction (DIP)
        VendorGstProfileResponse vendorRes = gatewayAdapter.searchGstin(normalizedGstin);

        if (vendorRes == null) {
            log.error("GST Gateway returned null response for masked GSTIN: {}", maskedGstin);
            throw new CustomException(GstConstants.ERR_CODE_GATEWAY_ERROR, GstConstants.MSG_GATEWAY_ERROR);
        }

        // 3. Handle vendor-level error payload
        if (vendorRes.getError() != null) {
            log.warn("GST gateway returned error for masked GSTIN {}: {} - {}",
                    maskedGstin, vendorRes.getError().getErrorCode(), vendorRes.getError().getMessage());

            ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), false);
            return GstProfileResponse.builder()
                    .responseInfo(responseInfo)
                    .isVerified(false)
                    .maskedGstin(maskedGstin)
                    .errorDetails(GstProfileResponse.GstErrorDetails.builder()
                            .errorCode(vendorRes.getError().getErrorCode())
                            .errorMessage(vendorRes.getError().getMessage())
                            .build())
                    .build();
        }

        // 4. Enrich domain response from vendor details
        VendorGstProfileResponse.TaxpayerData vData = vendorRes.getData();
        GstTaxpayerDetails uDetails = mapToGstTaxpayerDetails(vData, maskedGstin);
        boolean isVerified = vData != null && GstConstants.STATUS_ACTIVE.equalsIgnoreCase(vData.getStatus());

        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), isVerified);
        GstProfileResponse response = GstProfileResponse.builder()
                .responseInfo(responseInfo)
                .isVerified(isVerified)
                .maskedGstin(maskedGstin)
                .taxpayerDetails(uDetails)
                .build();

        log.info("GST profile search completed in service for masked GSTIN: {}, isVerified: {}", maskedGstin, isVerified);
        return response;
    }

    /**
     * Maps vendor-specific GST details into clean domain {@link GstTaxpayerDetails}.
     *
     * @param vData vendor taxpayer details DTO
     * @param maskedGstin masked GSTIN for telemetry
     * @return enriched domain {@link GstTaxpayerDetails}
     */
    private GstTaxpayerDetails mapToGstTaxpayerDetails(VendorGstProfileResponse.TaxpayerData vData, String maskedGstin) {
        if (vData == null) {
            return null;
        }

        String pan = GstNumberUtils.extractPanFromGstin(vData.getGstin());
        String stateCode = GstNumberUtils.extractStateCode(vData.getGstin());
        String stateName = GstNumberUtils.resolveStateName(stateCode);
        GstTaxpayerDetails.GstAddress addr = mapToGstAddress(vData, stateName);

        return GstTaxpayerDetails.builder()
                .gstin(vData.getGstin())
                .maskedGstin(maskedGstin)
                .legalName(vData.getLegalName())
                .tradeName(vData.getTradeName())
                .pan(pan)
                .taxpayerType(vData.getTaxpayerType())
                .constitutionOfBusiness(vData.getConstitutionOfBusiness())
                .status(vData.getStatus())
                .registrationDate(vData.getRegistrationDate())
                .cancellationDate(vData.getCancellationDate())
                .stateCode(stateCode)
                .state(stateName)
                .principalPlaceOfBusiness(addr)
                .build();
    }

    private GstTaxpayerDetails.GstAddress mapToGstAddress(VendorGstProfileResponse.TaxpayerData vData, String fallbackStateName) {
        if (vData.getPrincipalPlaceOfBusiness() == null || vData.getPrincipalPlaceOfBusiness().getAddress() == null) {
            return null;
        }

        VendorGstProfileResponse.Address vAddr = vData.getPrincipalPlaceOfBusiness().getAddress();
        List<String> addressParts = new ArrayList<>();
        if (StringUtils.isNotBlank(vAddr.getDoorNumber())) addressParts.add(vAddr.getDoorNumber().trim());
        if (StringUtils.isNotBlank(vAddr.getBuildingName())) addressParts.add(vAddr.getBuildingName().trim());
        if (StringUtils.isNotBlank(vAddr.getStreet())) addressParts.add(vAddr.getStreet().trim());
        if (StringUtils.isNotBlank(vAddr.getLocation())) addressParts.add(vAddr.getLocation().trim());
        if (StringUtils.isNotBlank(vAddr.getCity())) addressParts.add(vAddr.getCity().trim());
        if (StringUtils.isNotBlank(vAddr.getState())) addressParts.add(vAddr.getState().trim());
        if (StringUtils.isNotBlank(vAddr.getPincode())) addressParts.add(vAddr.getPincode().trim());
        String fullAddress = String.join(", ", addressParts);

        return GstTaxpayerDetails.GstAddress.builder()
                .doorNumber(vAddr.getDoorNumber())
                .buildingName(vAddr.getBuildingName())
                .street(vAddr.getStreet())
                .location(vAddr.getLocation())
                .city(vAddr.getCity())
                .district(vAddr.getDistrict())
                .state(StringUtils.isNotBlank(vAddr.getState()) ? vAddr.getState() : fallbackStateName)
                .pincode(vAddr.getPincode())
                .fullAddress(fullAddress)
                .build();
    }
}
