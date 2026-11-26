package org.egov.dx.adapter.vendor.gst.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;
import org.egov.dx.adapter.vendor.gst.GstGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.GstConstants;
import org.egov.dx.util.GstNumberUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of {@link GstGatewayAdapter} providing simulated IRIS Peridot / Taxpayer API responses.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles simulated mock dataset lookup and deterministic response synthesis.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly fulfills {@link GstGatewayAdapter} contract with exact IRIS vendor schemas.</li>
 * </ul>
 *
 * <p>Path to mock records is strictly loaded from configuration (zero hardcoded path).</p>
 * <p>Activated when {@code gst.verification.mock.enabled=true} (or if property is omitted).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "gst.verification.mock.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class MockGstGatewayAdapter implements GstGatewayAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Configurations configurations;
    private final Map<String, VendorGstProfileResponse.TaxpayerData> mockRecordMap = new ConcurrentHashMap<>();

    /**
     * Constructs {@link MockGstGatewayAdapter} with injected application configurations.
     *
     * @param configurations centralized application configurations
     */
    @Autowired
    public MockGstGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;
    }

    /**
     * Initializes the mock adapter and eagerly loads curated mock GST records.
     * Emits a visible security warning alerting that simulated mock verification is active.
     */
    @PostConstruct
    public void init() {
        log.warn("[SECURITY-WARNING] MockGstGatewayAdapter is ACTIVE. Mock GST verification must NOT be enabled in production environments!");
        loadMockRecords();
    }

    /**
     * Eagerly loads mock GST records from the configured classpath JSON resource into memory.
     */
    private synchronized void loadMockRecords() {
        if (!mockRecordMap.isEmpty()) {
            return;
        }

        String mockRecordsPath = (configurations != null)
                ? configurations.getGstVerificationMockRecordsPath()
                : null;

        if (StringUtils.isBlank(mockRecordsPath)) {
            log.error("[MOCK-GST-ADAPTER] gst.verification.mock.records.path is not configured");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST,
                    "gst.verification.mock.records.path is not configured in application.properties");
        }

        try {
            ClassPathResource resource = new ClassPathResource(mockRecordsPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<VendorGstProfileResponse.TaxpayerData> records = OBJECT_MAPPER.readValue(
                            is, new TypeReference<List<VendorGstProfileResponse.TaxpayerData>>() {});
                    populateRecords(records, mockRecordsPath);
                }
            } else {
                log.warn("[MOCK-GST-ADAPTER] Mock GST records resource not found at configured path: {}", mockRecordsPath);
            }
        } catch (IOException e) {
            log.error("[MOCK-GST-ADAPTER] Failed to load mock GST records: {}", e.getMessage(), e);
        }
    }

    private void populateRecords(List<VendorGstProfileResponse.TaxpayerData> records, String mockRecordsPath) {
        if (records != null) {
            for (VendorGstProfileResponse.TaxpayerData data : records) {
                if (StringUtils.isNotBlank(data.getGstin())) {
                    mockRecordMap.put(data.getGstin().trim().toUpperCase(), data);
                }
            }
            log.info("[MOCK-GST-ADAPTER] Successfully loaded {} mock GST records from configured path: {}",
                    mockRecordMap.size(), mockRecordsPath);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Searches taxpayer details against curated mock records. For unlisted valid GSTINs,
     * returns NOT_FOUND unless {@code gst.verification.mock.synthesize-unknown} is enabled.</p>
     */
    @Override
    public VendorGstProfileResponse searchGstin(String gstin) {
        String normalizedGstin = GstNumberUtils.normalizeGstin(gstin);
        String maskedGstin = GstNumberUtils.maskGstin(normalizedGstin);

        log.info("[MOCK-GST-ADAPTER] Executing simulated GST taxpayer search for masked GSTIN: {}", maskedGstin);

        // Step 1: Check if record exists in curated static mock records
        VendorGstProfileResponse.TaxpayerData existing = mockRecordMap.get(normalizedGstin);
        // If matched in mock records, return pre-configured active/cancelled taxpayer profile
        if (existing != null) {
            log.info("[MOCK-GST-ADAPTER] Found matching mock record for masked GSTIN: {}, legalName: {}, status: {}",
                    maskedGstin, existing.getLegalName(), existing.getStatus());

            return VendorGstProfileResponse.builder()
                    .statusCode("1")
                    .statusDesc(GstConstants.STATUS_SUCCESS)
                    .data(existing)
                    .build();
        }

        // Step 2: Handle valid GSTIN syntax not found in curated static records
        if (GstNumberUtils.isValidGstin(normalizedGstin, false)) {
            // Safe fallback check (Review comment High #3): Only synthesize unknown records if explicitly enabled in config
            if (configurations != null && configurations.isGstVerificationMockSynthesizeUnknownEnabled()) {
                String stateCode = GstNumberUtils.extractStateCode(normalizedGstin);
                String stateName = GstNumberUtils.resolveStateName(stateCode);
                String embeddedPan = GstNumberUtils.extractPanFromGstin(normalizedGstin);

                log.info("[MOCK-GST-ADAPTER] Synthesizing deterministic mock taxpayer for valid masked GSTIN: {}, state: {}",
                        maskedGstin, stateName);

                VendorGstProfileResponse.TaxpayerData generatedData = VendorGstProfileResponse.TaxpayerData.builder()
                        .gstin(normalizedGstin)
                        .legalName("ENTERPRISE " + (embeddedPan != null ? embeddedPan : "CORP"))
                        .tradeName("COMMERCIAL TRADING CO")
                        .constitutionOfBusiness("Private Limited Company")
                        .taxpayerType("Regular")
                        .status(GstConstants.STATUS_ACTIVE)
                        .registrationDate("01/07/2017")
                        .cancellationDate("")
                        .principalPlaceOfBusiness(VendorGstProfileResponse.PlaceOfBusiness.builder()
                                .address(VendorGstProfileResponse.Address.builder()
                                        .doorNumber("Plot 101")
                                        .buildingName("Commercial Complex")
                                        .street("Main Commercial Road")
                                        .location("Industrial Area")
                                        .city("State Capital")
                                        .district("Central District")
                                        .state(stateName)
                                        .pincode("143001")
                                        .build())
                                .natureOfBusiness("Retail and Commercial Services")
                                .build())
                        .additionalPlacesOfBusiness(Collections.emptyList())
                        .build();

                return VendorGstProfileResponse.builder()
                        .statusCode("1")
                        .statusDesc(GstConstants.STATUS_SUCCESS)
                        .data(generatedData)
                        .build();
            } else {
                // Production-safe default: unlisted GSTINs return 404 NOT_FOUND instead of synthetic approvals
                log.info("[MOCK-GST-ADAPTER] Unlisted GSTIN returning RECORD_NOT_FOUND for masked GSTIN: {}", maskedGstin);
                return VendorGstProfileResponse.builder()
                        .statusCode("0")
                        .statusDesc(GstConstants.STATUS_FAILURE)
                        .error(VendorGstProfileResponse.VendorError.builder()
                                .errorCode(GstConstants.ERR_CODE_RECORD_NOT_FOUND)
                                .message(GstConstants.MSG_RECORD_NOT_FOUND)
                                .build())
                        .build();
            }
        }

        // Step 3: Handle malformed or invalid GSTIN format
        return VendorGstProfileResponse.builder()
                .statusCode("0")
                .statusDesc(GstConstants.STATUS_FAILURE)
                .error(VendorGstProfileResponse.VendorError.builder()
                        .errorCode(GstConstants.ERR_CODE_INVALID_FORMAT)
                        .message(GstConstants.MSG_INVALID_FORMAT)
                        .build())
                .build();
    }
}
