package org.egov.user.web.contract.profile;

import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration of modular citizen entity sections that can be requested in profile search.
 * Designed to be forward-compatible for future entity expansions (e.g., FAMILY, KYC).
 */
public enum ProfileIncludeEntity {
    PROFILE,
    ADDRESS,
    DOCUMENT;

    public static ProfileIncludeEntity fromString(String val) {
        if (StringUtils.isBlank(val)) return null;
        for (ProfileIncludeEntity e : values()) {
            if (e.name().equalsIgnoreCase(val.trim())) {
                return e;
            }
        }
        return null;
    }
}
