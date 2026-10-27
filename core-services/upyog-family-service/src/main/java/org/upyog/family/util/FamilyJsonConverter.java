package org.upyog.family.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility component dedicated to JSON serialization and deserialization for family aggregates.
 * Adheres strictly to Single Responsibility Principle (SRP) by decoupling JSON mapping from repositories.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class FamilyJsonConverter {

    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Converts an arbitrary object or map into a JSON string safely.
     *
     * @param obj object to serialize
     * @return serialized JSON string, or null on error / null input
     */
    public String convertToJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[FAMILY-JSON-CONVERTER] Error converting object to JSON string: {}", e.getMessage());
            return null;
        }
    }
}
