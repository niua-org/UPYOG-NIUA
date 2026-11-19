package org.egov.dx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for PAN format validation, normalization, and secure PII masking.
 *
 * <p>Strictly enforces security best practices by preventing unmasked PAN exposure in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class PanNumberUtils {

    private static final Pattern PAN_PATTERN = Pattern.compile(PanConstants.PAN_REGEX);

    private PanNumberUtils() {
        // Utility class
    }

    /**
     * Normalizes a PAN number by trimming whitespace and converting to uppercase.
     *
     * @param pan raw PAN string
     * @return trimmed, upper-cased PAN string or null if blank
     */
    public static String normalizePan(String pan) {
        if (StringUtils.isBlank(pan)) {
            return null;
        }
        return pan.trim().toUpperCase();
    }

    /**
     * Validates whether a normalized PAN string matches the standard 10-character PAN format (e.g., ABCDE1234F).
     *
     * @param pan raw or normalized PAN
     * @return true if valid format, false otherwise
     */
    public static boolean isValidPan(String pan) {
        String normalized = normalizePan(pan);
        if (normalized == null || normalized.length() != 10) {
            return false;
        }
        return PAN_PATTERN.matcher(normalized).matches();
    }

    /**
     * Masks a 10-character PAN for safe logging and telemetry (e.g., "ABCDE****F").
     *
     * @param pan raw or normalized PAN
     * @return safely masked PAN or "XXXX****XX" if blank/invalid
     */
    public static String maskPan(String pan) {
        if (StringUtils.isBlank(pan)) {
            return "XXXX****XX";
        }
        String normalized = pan.trim().toUpperCase();
        if (normalized.length() < 6) {
            return "******";
        }
        if (normalized.length() == 10) {
            return normalized.substring(0, 5) + "****" + normalized.substring(9);
        }
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 2);
    }

    /**
     * Resolves the entity category based on the 4th character of the PAN.
     *
     * @param pan raw or normalized PAN
     * @return resolved entity category (e.g. "Individual", "Company", "Firm", "Local Authority")
     */
    public static String resolvePanCategory(String pan) {
        String normalized = normalizePan(pan);
        if (normalized == null || normalized.length() < 4) {
            return "Other";
        }
        char categoryChar = normalized.charAt(3);
        switch (categoryChar) {
            case 'P':
                return "Individual";
            case 'C':
                return "Company";
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
                return "Government";
            default:
                return "Other Entity";
        }
    }

    /**
     * Normalizes date of birth string to ITD standard DD/MM/YYYY format.
     * Accepts YYYY-MM-DD or DD/MM/YYYY.
     *
     * @param dob input date of birth
     * @return formatted DD/MM/YYYY string or trimmed original
     */
    public static String normalizeDobToDdMmYyyy(String dob) {
        if (StringUtils.isBlank(dob)) {
            return null;
        }
        String trimmed = dob.trim();
        // If YYYY-MM-DD
        if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = trimmed.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        }
        return trimmed;
    }
}
