package org.egov.user.domain.service.multiauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service managing sequential and deterministic dispensing of mock Aadhaar citizen demographics
 * for local development and mock mode testing.
 *
 * <p>Loads 500+ realistic citizen records from {@code mock/aadhaar-mock-citizens.json}
 * when {@code aadhaar.mock.enabled=true}. Supports deterministic lookup based on Aadhaar hash
 * as well as sequential round-robin dispensing.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
// Fix for PR Comment #16 (MockAadhaarRecordService.java:144):
// Conditionally load mock bean only when aadhaar.mock.enabled is true, preventing unnecessary loading in prod.
@ConditionalOnProperty(name = "aadhaar.mock.enabled", havingValue = "true")
public class MockAadhaarRecordService {

    /**
     * Persona model representing a mock Aadhaar citizen identity.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MockCitizenRecord {
        /** Unique record sequential index */
        private Integer id;
        /** Full citizen name */
        private String name;
        /** Gender code ("M" or "F") */
        private String gender;
        /** Date of birth formatted as YYYY-MM-DD */
        private String dob;
        /** Care-of or guardian relation string (e.g. "S/O Rajesh Sharma") */
        private String careOf;
        /** Father or guardian name */
        private String fatherName;
        /**
         * Fix for PR Comment #17 (MockAadhaarRecordService.java:57):
         * Reserved dummy 10-digit mobile number in non-routable test range (99999XXXXX)
         * to prevent triggering SMS/OTPs to real citizens if sent downstream.
         */
        private String mobileNumber;
        /** House or flat number */
        private String house;
        /** Street or road address line */
        private String street;
        /** Locality landmark */
        private String landmark;
        /** City locality or sector */
        private String locality;
        /** Subdistrict or tehsil */
        private String subdistrict;
        /** Village/Town/City identifier */
        private String vtc;
        /** District name */
        private String district;
        /** State name */
        private String state;
        /** 6-digit postal PIN code */
        private String pincode;
        /** Full composed single-line address */
        private String fullAddress;
    }

    private static final String MOCK_RECORDS_PATH = "mock/aadhaar-mock-citizens.json";
    private static final String DEFAULT_NAME = "Aarav Sharma";
    private static final String DEFAULT_GENDER = "M";
    private static final String DEFAULT_DOB = "1990-01-01";
    private static final String DEFAULT_CARE_OF = "S/O Ramesh Sharma";
    private static final List<MockCitizenRecord> RECORDS = new ArrayList<>();
    private static final AtomicInteger CURRENT_INDEX = new AtomicInteger(0);

    /**
     * Initializes the mock persona registry by loading citizen records on bean startup.
     */
    @PostConstruct
    public void init() {
        loadRecords();
    }

    /**
     * Loads the 500+ citizen records from classpath JSON into memory.
     * Synchronized to guarantee safe singleton loading across concurrent threads.
     */
    private static synchronized void loadRecords() {
        if (!RECORDS.isEmpty()) {
            return;
        }
        try {
            ClassPathResource resource = new ClassPathResource(MOCK_RECORDS_PATH);
            if (resource.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                try (InputStream is = resource.getInputStream()) {
                    List<MockCitizenRecord> loaded = mapper.readValue(is, new TypeReference<List<MockCitizenRecord>>() {});
                    if (loaded != null && !loaded.isEmpty()) {
                        RECORDS.addAll(loaded);
                        log.info("[MOCK-AADHAAR-SERVICE] Successfully loaded {} mock Aadhaar citizen records from {}",
                                RECORDS.size(), MOCK_RECORDS_PATH);
                    }
                }
            } else {
                log.warn("[MOCK-AADHAAR-SERVICE] Mock Aadhaar resource not found at: {}", MOCK_RECORDS_PATH);
            }
        } catch (Exception e) {
            log.error("[MOCK-AADHAAR-SERVICE] Failed to load mock Aadhaar citizens: {}", e.getMessage(), e);
        }
    }

    /**
     * Fix for PR Comment #16 (MockAadhaarRecordService.java:144):
     * Retrieves a deterministic mock citizen record derived deterministically from the Aadhaar number hash.
     * Guarantees that the same Aadhaar number always resolves to the exact same citizen persona
     * across repeated calls, preventing non-deterministic test failures in verification and deduplication flows.
     *
     * @param aadhaar plain or masked Aadhaar number
     * @return deterministic {@link MockCitizenRecord} persona
     */
    public static MockCitizenRecord getRecordForAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) {
            return getNextRecord();
        }
        if (RECORDS.isEmpty()) {
            loadRecords();
        }

        if (RECORDS.isEmpty()) {
            return MockCitizenRecord.builder()
                    .name(DEFAULT_NAME)
                    .gender(DEFAULT_GENDER)
                    .dob(DEFAULT_DOB)
                    .careOf(DEFAULT_CARE_OF)
                    .build();
        }

        int index = Math.floorMod(aadhaar.trim().hashCode(), RECORDS.size());
        MockCitizenRecord record = RECORDS.get(index);
        log.info("[MOCK-AADHAAR-SERVICE] Deterministically dispensing mock citizen record [{}/{}]: {} ({}) for Aadhaar hash",
                index + 1, RECORDS.size(), record.getName(), record.getGender());
        return record;
    }

    /**
     * Retrieves the next mock citizen record sequentially.
     * Guaranteed to cycle through all loaded records before repeating.
     *
     * @return next {@link MockCitizenRecord} persona
     */
    public static MockCitizenRecord getNextRecord() {
        if (RECORDS.isEmpty()) {
            loadRecords();
        }

        if (RECORDS.isEmpty()) {
            return MockCitizenRecord.builder()
                    .name(DEFAULT_NAME)
                    .gender(DEFAULT_GENDER)
                    .dob(DEFAULT_DOB)
                    .careOf(DEFAULT_CARE_OF)
                    .build();
        }

        int index = Math.floorMod(CURRENT_INDEX.getAndIncrement(), RECORDS.size());
        MockCitizenRecord record = RECORDS.get(index);
        log.info("[MOCK-AADHAAR-SERVICE] Dispensing mock citizen record [{}/{}]: {} ({})",
                index + 1, RECORDS.size(), record.getName(), record.getGender());
        return record;
    }

    /**
     * Returns total number of mock records loaded.
     *
     * @return count of mock records
     */
    public static int getTotalRecords() {
        if (RECORDS.isEmpty()) {
            loadRecords();
        }
        return RECORDS.size();
    }

    /**
     * Resets the sequential counter to 0 (useful for reproducible unit test suites).
     */
    public static void resetIndex() {
        CURRENT_INDEX.set(0);
    }
}
