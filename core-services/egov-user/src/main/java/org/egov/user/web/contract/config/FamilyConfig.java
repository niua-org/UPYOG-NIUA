package org.egov.user.web.contract.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contract representing dynamic state-level Family Configuration retrieved from MDMS, Redis, or Database.
 *
 * <p>Determines whether a state/tenant supports the family concept, and if so,
 * whether it uses UPYOG Native Internal Family lifecycle or external State Family Registry (e.g. Haryana PPP).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FamilyConfig {

    public static final String TYPE_UPYOG_INTERNAL = "UPYOG_INTERNAL";
    public static final String TYPE_STATE_REGISTRY = "STATE_REGISTRY";
    public static final String TYPE_NONE = "NONE";

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("isFamilySupported")
    private Boolean isFamilySupported;

    @JsonProperty("familyType")
    private String familyType;

    @JsonProperty("active")
    private Boolean active;

    /**
     * Checks if family concept is supported for this tenant.
     *
     * @return true if family is supported and type is not NONE
     */
    public boolean isFamilySupported() {
        return Boolean.TRUE.equals(isFamilySupported) && !TYPE_NONE.equalsIgnoreCase(familyType);
    }

    /**
     * Checks if tenant uses UPYOG Native Internal Family lifecycle.
     *
     * @return true if family is supported and familyType is UPYOG_INTERNAL
     */
    public boolean isUpyogInternal() {
        return isFamilySupported() && TYPE_UPYOG_INTERNAL.equalsIgnoreCase(familyType);
    }

    /**
     * Checks if tenant uses an external State Family Registry (e.g. Haryana PPP).
     *
     * @return true if family is supported and familyType is STATE_REGISTRY
     */
    public boolean isStateRegistry() {
        return isFamilySupported() && TYPE_STATE_REGISTRY.equalsIgnoreCase(familyType);
    }

    /**
     * Creates a default UPYOG-internal configuration as safe fallback when MDMS has no record.
     *
     * @param tenantId state tenant identifier
     * @return default active UPYOG_INTERNAL FamilyConfig
     */
    public static FamilyConfig createDefault(String tenantId) {
        return FamilyConfig.builder()
                .code(TYPE_UPYOG_INTERNAL)
                .name("Default UPYOG Native Family Lifecycle")
                .isFamilySupported(true)
                .familyType(TYPE_UPYOG_INTERNAL)
                .active(true)
                .build();
    }
}
