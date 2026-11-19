package org.egov.dx.adapter.vendor.pan.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;
import org.egov.dx.adapter.vendor.pan.PanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.PanConstants;
import org.egov.dx.util.PanNumberUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of {@link PanGatewayAdapter} providing simulated ITD PAN verification responses.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles simulated mock dataset lookup and deterministic response synthesis.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly fulfills {@link PanGatewayAdapter} contract with exact ITD vendor schemas.</li>
 * </ul>
 *
 * <p>Path to mock records is strictly loaded from configuration (zero hardcoded path).</p>
 * <p>Activated when {@code pan.verification.mock.enabled=true} (or if property is omitted).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "pan.verification.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockPanGatewayAdapter implements PanGatewayAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RES_PAN_PREFIX = "RES-PAN-";

    private final Configurations configurations;
    private final Map<String, VendorPanVerificationResponse.ResponseParams> mockRecordMap = new ConcurrentHashMap<>();

    @Autowired
    public MockPanGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;
    }

    @PostConstruct
    public void init() {
        loadMockRecords();
    }

    /**
     * Eagerly loads mock PAN records from the configured classpath JSON resource into memory.
     */
    private synchronized void loadMockRecords() {
        if (!mockRecordMap.isEmpty()) {
            return;
        }

        String mockRecordsPath = (configurations != null)
                ? configurations.getPanVerificationMockRecordsPath()
                : null;

        if (StringUtils.isBlank(mockRecordsPath)) {
            log.error("[MOCK-PAN-ADAPTER] pan.verification.mock.records.path is not configured");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST,
                    "pan.verification.mock.records.path is not configured in application.properties");
        }

        try {
            ClassPathResource resource = new ClassPathResource(mockRecordsPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<VendorPanVerificationResponse.ResponseParams> records = OBJECT_MAPPER.readValue(
                            is, new TypeReference<List<VendorPanVerificationResponse.ResponseParams>>() {});
                    populateRecords(records, mockRecordsPath);
                }
            } else {
                log.warn("[MOCK-PAN-ADAPTER] Mock PAN records resource not found at configured path: {}", mockRecordsPath);
            }
        } catch (IOException e) {
            log.error("[MOCK-PAN-ADAPTER] Failed to load mock PAN records: {}", e.getMessage(), e);
        }
    }

    private void populateRecords(List<VendorPanVerificationResponse.ResponseParams> records, String mockRecordsPath) {
        if (records != null) {
            for (VendorPanVerificationResponse.ResponseParams details : records) {
                if (StringUtils.isNotBlank(details.getPan())) {
                    mockRecordMap.put(details.getPan().trim().toUpperCase(), details);
                }
            }
            log.info("[MOCK-PAN-ADAPTER] Successfully loaded {} mock PAN records from configured path: {}",
                    mockRecordMap.size(), mockRecordsPath);
        }
    }

    @Override
    public VendorPanVerificationResponse verifyPan(VendorPanVerificationRequest request) {
        String pan = request != null && request.getSearchParams() != null
                ? request.getSearchParams().getPan()
                : null;
        String inputFullName = request != null && request.getSearchParams() != null
                ? request.getSearchParams().getFullName()
                : null;
        String inputDob = request != null && request.getSearchParams() != null
                ? request.getSearchParams().getDob()
                : null;

        String normalizedPan = PanNumberUtils.normalizePan(pan);
        String maskedPan = PanNumberUtils.maskPan(normalizedPan);
        String reqId = request != null && request.getHeader() != null
                ? request.getHeader().getRequestId()
                : UUID.randomUUID().toString();

        log.debug("[MOCK-PAN-ADAPTER] Processing PAN verification for masked PAN: {}, reqId: {}", maskedPan, reqId);

        // Explicit Negative Testing trigger: e.g. PAN containing "NOTF" or ending with "0000X"
        if (normalizedPan != null && (normalizedPan.contains("NOTF") || normalizedPan.endsWith("0000X"))) {
            log.info("[MOCK-PAN-ADAPTER] Simulating PAN NOT FOUND for masked PAN: {}", maskedPan);
            return VendorPanVerificationResponse.builder()
                    .header(VendorPanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .serviceId(PanConstants.DEFAULT_SERVICE_ID)
                            .responseId(RES_PAN_PREFIX + UUID.randomUUID())
                            .status(PanConstants.STATUS_FAILURE)
                            .statusCode("404")
                            .build())
                    .errorDetails(VendorPanVerificationResponse.VendorErrorDetails.builder()
                            .errorCode(PanConstants.ERR_CODE_RECORD_NOT_FOUND)
                            .errorMessage(PanConstants.MSG_RECORD_NOT_FOUND)
                            .build())
                    .build();
        }

        // 1. Check if record exists in curated static mock records
        VendorPanVerificationResponse.ResponseParams existing = mockRecordMap.get(normalizedPan);
        if (existing != null) {
            String nameMatch = evaluateNameMatch(inputFullName, existing.getFullName());
            String dobMatch = evaluateDobMatch(inputDob, existing.getDob());

            VendorPanVerificationResponse.ResponseParams responseParams = VendorPanVerificationResponse.ResponseParams.builder()
                    .pan(existing.getPan())
                    .panStatus(existing.getPanStatus())
                    .fullName(existing.getFullName())
                    .category(existing.getCategory())
                    .nameMatch(nameMatch)
                    .dobMatch(dobMatch)
                    .seedingStatus(existing.getSeedingStatus())
                    .build();

            log.info("[MOCK-PAN-ADAPTER] Found matching mock record for masked PAN: {}, nameMatch: {}, dobMatch: {}",
                    maskedPan, nameMatch, dobMatch);

            return VendorPanVerificationResponse.builder()
                    .header(VendorPanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .serviceId(PanConstants.DEFAULT_SERVICE_ID)
                            .responseId(RES_PAN_PREFIX + UUID.randomUUID())
                            .status(PanConstants.STATUS_SUCCESS)
                            .statusCode("200")
                            .build())
                    .responseParams(responseParams)
                    .build();
        }

        // 2. Deterministic Algorithmic Generation for arbitrary valid PAN
        if (PanNumberUtils.isValidPan(normalizedPan)) {
            String category = PanNumberUtils.resolvePanCategory(normalizedPan);
            String syntheticName = StringUtils.isNotBlank(inputFullName) ? inputFullName.trim().toUpperCase() : "SIMULATED TAXPAYER";

            VendorPanVerificationResponse.ResponseParams generatedParams = VendorPanVerificationResponse.ResponseParams.builder()
                    .pan(normalizedPan)
                    .panStatus(PanConstants.STATUS_EXISTING_AND_VALID)
                    .fullName(syntheticName)
                    .category(category)
                    .nameMatch(PanConstants.MATCH_YES)
                    .dobMatch(PanConstants.MATCH_YES)
                    .seedingStatus(PanConstants.MATCH_YES)
                    .build();

            log.info("[MOCK-PAN-ADAPTER] Generating deterministic mock response for masked PAN: {}, category: {}",
                    maskedPan, category);

            return VendorPanVerificationResponse.builder()
                    .header(VendorPanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .serviceId(PanConstants.DEFAULT_SERVICE_ID)
                            .responseId(RES_PAN_PREFIX + UUID.randomUUID())
                            .status(PanConstants.STATUS_SUCCESS)
                            .statusCode("200")
                            .build())
                    .responseParams(generatedParams)
                    .build();
        }

        // If not valid format
        return VendorPanVerificationResponse.builder()
                .header(VendorPanVerificationResponse.ResponseHeader.builder()
                        .requestId(reqId)
                        .serviceId(PanConstants.DEFAULT_SERVICE_ID)
                        .responseId(RES_PAN_PREFIX + UUID.randomUUID())
                        .status(PanConstants.STATUS_FAILURE)
                        .statusCode("400")
                        .build())
                .errorDetails(VendorPanVerificationResponse.VendorErrorDetails.builder()
                        .errorCode(PanConstants.ERR_CODE_INVALID_FORMAT)
                        .errorMessage(PanConstants.MSG_INVALID_FORMAT)
                        .build())
                .build();
    }

    private String evaluateNameMatch(String inputName, String recordName) {
        if (StringUtils.isBlank(inputName) || StringUtils.isBlank(recordName)) {
            return PanConstants.MATCH_NO;
        }
        String cleanInput = inputName.trim().replaceAll("\\s+", " ").toUpperCase();
        String cleanRecord = recordName.trim().replaceAll("\\s+", " ").toUpperCase();
        return (cleanInput.equalsIgnoreCase(cleanRecord) || cleanInput.contains(cleanRecord) || cleanRecord.contains(cleanInput))
                ? PanConstants.MATCH_YES
                : PanConstants.MATCH_NO;
    }

    private String evaluateDobMatch(String inputDob, String recordDob) {
        if (StringUtils.isBlank(inputDob) || StringUtils.isBlank(recordDob)) {
            return PanConstants.MATCH_NO;
        }
        String normInput = PanNumberUtils.normalizeDobToDdMmYyyy(inputDob);
        String normRecord = PanNumberUtils.normalizeDobToDdMmYyyy(recordDob);
        return StringUtils.equalsIgnoreCase(normInput, normRecord)
                ? PanConstants.MATCH_YES
                : PanConstants.MATCH_NO;
    }
}
