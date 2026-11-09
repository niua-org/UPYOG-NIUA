package org.egov.dx.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Normalization helpers for Aadhaar numbers exchanged with egov-user during e-KYC flows.
 *
 * <p>TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class AadhaarNumberUtils {

    private AadhaarNumberUtils() {
    }

    /**
     * Returns a 12-digit Aadhaar string when the input contains exactly twelve digits.
     *
     * @param value candidate Aadhaar value
     * @return normalized 12-digit string, or {@code null} if not available
     */
    public static String normalizeToTwelveDigits(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String digitsOnly = value.trim().replaceAll("\\D", "");
        if (digitsOnly.length() == 12) {
            return digitsOnly;
        }
        return null;
    }
}
