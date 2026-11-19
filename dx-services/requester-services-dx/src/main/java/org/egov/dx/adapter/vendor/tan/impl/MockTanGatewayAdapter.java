package org.egov.dx.adapter.vendor.tan.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;
import org.egov.dx.adapter.vendor.tan.TanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.TanConstants;
import org.egov.dx.util.TanNumberUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of {@link TanGatewayAdapter} providing simulated ITD TAN verification responses.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles simulated mock dataset lookup and deterministic response synthesis.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly fulfills {@link TanGatewayAdapter} contract with exact ITD vendor schemas.</li>
 * </ul>
 *
 * <p>Activated when {@code tan.verification.mock.enabled=true} (or if property is omitted).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "tan.verification.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockTanGatewayAdapter implements TanGatewayAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Configurations configurations;
    private final Map<String, VendorTanVerificationResponse.VendorTanDetails> mockRecordMap = new ConcurrentHashMap<>();

    @Autowired
    public MockTanGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;
    }

    @PostConstruct
    public void init() {
        loadMockRecords();
    }

    /**
     * Eagerly loads mock TAN records from the configured classpath JSON resource into memory.
     */
    private synchronized void loadMockRecords() {
        if (!mockRecordMap.isEmpty()) {
            return;
        }

        String mockRecordsPath = (configurations != null)
                ? configurations.getTanVerificationMockRecordsPath()
                : null;

        if (StringUtils.isBlank(mockRecordsPath)) {
            log.error("[MOCK-TAN-ADAPTER] tan.verification.mock.records.path is not configured");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST,
                    "tan.verification.mock.records.path is not configured in application.properties");
        }

        try {
            ClassPathResource resource = new ClassPathResource(mockRecordsPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<VendorTanVerificationResponse.VendorTanDetails> records = OBJECT_MAPPER.readValue(
                            is, new TypeReference<List<VendorTanVerificationResponse.VendorTanDetails>>() {});
                    populateRecords(records, mockRecordsPath);
                }
            } else {
                log.warn("[MOCK-TAN-ADAPTER] Mock TAN records resource not found at configured path: {}", mockRecordsPath);
            }
        } catch (IOException e) {
            log.error("[MOCK-TAN-ADAPTER] Failed to load mock TAN records: {}", e.getMessage(), e);
        }
    }

    private void populateRecords(List<VendorTanVerificationResponse.VendorTanDetails> records, String mockRecordsPath) {
        if (records != null) {
            for (VendorTanVerificationResponse.VendorTanDetails details : records) {
                if (StringUtils.isNotBlank(details.getTan())) {
                    mockRecordMap.put(details.getTan().trim().toUpperCase(), details);
                }
            }
            log.info("[MOCK-TAN-ADAPTER] Successfully loaded {} mock TAN records from configured path: {}",
                    mockRecordMap.size(), mockRecordsPath);
        }
    }

    @Override
    public VendorTanVerificationResponse verifyTan(VendorTanVerificationRequest request) {
        String tan = request != null && request.getSearchCriteria() != null
                ? request.getSearchCriteria().getTan()
                : null;
        String normalizedTan = TanNumberUtils.normalizeTan(tan);
        String maskedTan = TanNumberUtils.maskTan(normalizedTan);
        String reqId = request != null && request.getHeader() != null
                ? request.getHeader().getRequestId()
                : UUID.randomUUID().toString();

        log.debug("[MOCK-TAN-ADAPTER] Processing TAN verification for masked TAN: {}, reqId: {}", maskedTan, reqId);

        // Explicit Negative Testing trigger: e.g. TAN ending with "00000X" or containing "NOTF"
        if (normalizedTan != null && (normalizedTan.contains("NOTF") || normalizedTan.endsWith("00000X"))) {
            log.info("[MOCK-TAN-ADAPTER] Simulating TAN NOT FOUND for masked TAN: {}", maskedTan);
            return VendorTanVerificationResponse.builder()
                    .header(VendorTanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .responseId("RES-" + UUID.randomUUID())
                            .status(TanConstants.STATUS_FAILURE)
                            .statusCode("404")
                            .timestamp(Instant.now().toString())
                            .build())
                    .errorDetails(VendorTanVerificationResponse.VendorErrorDetails.builder()
                            .errorCode(TanConstants.ERR_CODE_RECORD_NOT_FOUND)
                            .errorMessage(TanConstants.MSG_RECORD_NOT_FOUND)
                            .build())
                    .build();
        }

        // 1. Check if record exists in curated static mock records
        VendorTanVerificationResponse.VendorTanDetails existing = mockRecordMap.get(normalizedTan);
        if (existing != null) {
            log.info("[MOCK-TAN-ADAPTER] Found matching mock record for masked TAN: {}, deductor: {}",
                    maskedTan, existing.getNameOfDeductor());
            return VendorTanVerificationResponse.builder()
                    .header(VendorTanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .responseId("RES-" + UUID.randomUUID())
                            .status(TanConstants.STATUS_SUCCESS)
                            .statusCode("200")
                            .timestamp(Instant.now().toString())
                            .build())
                    .tanDetails(existing)
                    .build();
        }

        // 2. Deterministic Algorithmic Generation for any arbitrary valid TAN
        if (TanNumberUtils.isValidTan(normalizedTan)) {
            String category = TanNumberUtils.resolveDeductorCategory(normalizedTan);
            String cityPrefix = normalizedTan.substring(0, 3);
            log.info("[MOCK-TAN-ADAPTER] Generating deterministic mock response for masked TAN: {}, category: {}",
                    maskedTan, category);

            VendorTanVerificationResponse.VendorTanDetails generatedDetails = VendorTanVerificationResponse.VendorTanDetails.builder()
                    .tan(normalizedTan)
                    .nameOfDeductor("SIMULATED ENTERPRISE " + cityPrefix + " (" + category.toUpperCase() + ")")
                    .categoryOfDeductor(category)
                    .status(TanConstants.STATUS_ACTIVE)
                    .emailId("accounts." + cityPrefix.toLowerCase() + "@simulated-tan.org")
                    .address(VendorTanVerificationResponse.VendorAddress.builder()
                            .line1("Administrative Block, Sector " + normalizedTan.substring(4, 6))
                            .city("City " + cityPrefix)
                            .state("Punjab")
                            .pinCode("143" + normalizedTan.substring(6, 9))
                            .build())
                    .jurisdictionDetails(VendorTanVerificationResponse.VendorJurisdiction.builder()
                            .aoCode(cityPrefix + "T" + normalizedTan.substring(7, 9))
                            .aoType("CIT")
                            .rangeCode("101")
                            .aoNumber("1")
                            .description("Range 1, " + cityPrefix)
                            .build())
                    .build();

            return VendorTanVerificationResponse.builder()
                    .header(VendorTanVerificationResponse.ResponseHeader.builder()
                            .requestId(reqId)
                            .responseId("RES-" + UUID.randomUUID())
                            .status(TanConstants.STATUS_SUCCESS)
                            .statusCode("200")
                            .timestamp(Instant.now().toString())
                            .build())
                    .tanDetails(generatedDetails)
                    .build();
        }

        // If not valid format
        return VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .requestId(reqId)
                        .responseId("RES-" + UUID.randomUUID())
                        .status(TanConstants.STATUS_FAILURE)
                        .statusCode("400")
                        .timestamp(Instant.now().toString())
                        .build())
                .errorDetails(VendorTanVerificationResponse.VendorErrorDetails.builder()
                        .errorCode(TanConstants.ERR_CODE_INVALID_FORMAT)
                        .errorMessage(TanConstants.MSG_INVALID_FORMAT)
                        .build())
                .build();
    }
}
