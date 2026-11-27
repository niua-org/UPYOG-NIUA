package org.egov.dx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for TAN format validation, normalization, and secure PII masking.
 *
 * <p>Strictly enforces security best practices by preventing unmasked TAN leakage in application logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class TanNumberUtils {

    private static final Pattern TAN_PATTERN = Pattern.compile(TanConstants.TAN_REGEX);

    private TanNumberUtils() {
        // Utility class
    }

    /**
     * Normalizes a TAN number by trimming whitespace and converting to uppercase.
     *
     * @param tan raw TAN string
     * @return trimmed, upper-cased TAN string or null if blank
     */
    public static String normalizeTan(String tan) {
        // Return null if input is blank or whitespace-only
        if (StringUtils.isBlank(tan)) {
            return null;
        }
        return tan.trim().toUpperCase();
    }

    /**
     * Validates whether a normalized TAN string matches the standard 10-character TAN format (e.g., BLRM12345C).
     *
     * @param tan raw or normalized TAN
     * @return true if valid format, false otherwise
     */
    public static boolean isValidTan(String tan) {
        String normalized = normalizeTan(tan);
        // Pre-check length: all Indian TAN numbers are exactly 10 characters
        if (normalized == null || normalized.length() != 10) {
            return false;
        }
        return TAN_PATTERN.matcher(normalized).matches();
    }

    /**
     * Masks a 10-character TAN for safe logging and telemetry (e.g., "BLRM****5C").
     *
     * @param tan raw or normalized TAN
     * @return safely masked TAN or "XXXX****XX" if blank/invalid
     */
    public static String maskTan(String tan) {
        // Guard check for blank/null string to prevent NPE in log statements
        if (StringUtils.isBlank(tan)) {
            return "XXXX****XX";
        }
        String normalized = tan.trim().toUpperCase();
        // If string is truncated or short (<6 chars), mask completely
        if (normalized.length() < 6) {
            return "******";
        }
        // Standard 10-character TAN masking: retain first 4 characters and last 2 characters, mask middle 4 digits
        if (normalized.length() == 10) {
            return normalized.substring(0, 4) + "****" + normalized.substring(8);
        }
        // Non-standard length fallback: keep leading 2 and trailing 2 characters
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 2);
    }

    /**
     * Resolves the deductor category based on the 4th character of the TAN.
     *
     * @param tan raw or normalized TAN
     * @return resolved category string (e.g. "Company", "Local Authority", "Central Government")
     */
    public static String resolveDeductorCategory(String tan) {
        String normalized = normalizeTan(tan);
        // Bounds check: need at least 4 characters to inspect the 4th character (index 3)
        if (normalized == null || normalized.length() < 4) {
            return "Other";
        }
        char categoryChar = normalized.charAt(3);
        switch (categoryChar) {
            case 'C':
                return "Company";
            case 'P':
                return "Individual / HUF";
            case 'H':
                return "Hindu Undivided Family";
            case 'F':
                return "Firm";
            case 'A':
                return "Association of Persons (AOP)";
            case 'T':
                return "Trust";
            case 'B':
                return "Body of Individuals (BOI)";
            case 'L':
                return "Local Authority";
            case 'J':
                return "Artificial Juridical Person";
            case 'G':
                return "Central Government";
            case 'S':
                return "State Government";
            case 'D':
                return "Autonomous Body";
            default:
                return "Other Entity";
        }
    }
}
