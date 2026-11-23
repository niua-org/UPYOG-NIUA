package org.egov.dx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for GSTIN validation, normalization, state resolution, and safe PII masking.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class GstNumberUtils {

    private static final Pattern GSTIN_PATTERN = Pattern.compile(GstConstants.GSTIN_REGEX);
    private static final String BASE36_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Map<String, String> STATE_CODE_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("01", "Jammu and Kashmir");
        map.put("02", "Himachal Pradesh");
        map.put("03", "Punjab");
        map.put("04", "Chandigarh");
        map.put("05", "Uttarakhand");
        map.put("06", "Haryana");
        map.put("07", "Delhi");
        map.put("08", "Rajasthan");
        map.put("09", "Uttar Pradesh");
        map.put("10", "Bihar");
        map.put("11", "Sikkim");
        map.put("12", "Arunachal Pradesh");
        map.put("13", "Nagaland");
        map.put("14", "Manipur");
        map.put("15", "Mizoram");
        map.put("16", "Tripura");
        map.put("17", "Meghalaya");
        map.put("18", "Assam");
        map.put("19", "West Bengal");
        map.put("20", "Jharkhand");
        map.put("21", "Odisha");
        map.put("22", "Chhattisgarh");
        map.put("23", "Madhya Pradesh");
        map.put("24", "Gujarat");
        map.put("26", "Dadra and Nagar Haveli and Daman and Diu");
        map.put("27", "Maharashtra");
        map.put("29", "Karnataka");
        map.put("30", "Goa");
        map.put("31", "Lakshadweep");
        map.put("32", "Kerala");
        map.put("33", "Tamil Nadu");
        map.put("34", "Puducherry");
        map.put("35", "Andaman and Nicobar Islands");
        map.put("36", "Telangana");
        map.put("37", "Andhra Pradesh");
        map.put("38", "Ladakh");
        STATE_CODE_MAP = Collections.unmodifiableMap(map);
    }

    private GstNumberUtils() {
        // Utility class
    }

    /**
     * Normalizes a GSTIN by trimming whitespace and converting to uppercase.
     *
     * @param gstin raw GSTIN string
     * @return trimmed, uppercase GSTIN or null if blank
     */
    public static String normalizeGstin(String gstin) {
        if (StringUtils.isBlank(gstin)) {
            return null;
        }
        return gstin.trim().toUpperCase();
    }

    /**
     * Validates whether a normalized GSTIN matches the standard 15-character GSTIN format
     * and passes the Mod-36 checksum validation.
     *
     * @param gstin raw or normalized GSTIN
     * @return true if valid format and valid check digit, false otherwise
     */
    public static boolean isValidGstin(String gstin) {
        return isValidGstin(gstin, true);
    }

    /**
     * Validates whether a normalized GSTIN matches the standard 15-character GSTIN format,
     * with optional check digit validation.
     *
     * @param gstin raw or normalized GSTIN
     * @param validateChecksum whether to enforce Mod-36 check digit validation
     * @return true if valid format (and valid check digit if enabled), false otherwise
     */
    public static boolean isValidGstin(String gstin, boolean validateChecksum) {
        String normalized = normalizeGstin(gstin);
        // Length guard: GSTIN must be exactly 15 alphanumeric characters
        if (normalized == null || normalized.length() != 15) {
            return false;
        }
        // Pattern guard: verify 2-digit state code, 10-char PAN, 1-char entity number, 'Z' default character
        if (!GSTIN_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        // Checksum verification (Review comment High #6): Enforce Mod-36 Luhn-like check digit validation if requested
        return !validateChecksum || validateCheckDigit(normalized);
    }

    /**
     * Calculates the 15th check digit for a 14-character GSTIN prefix using the Mod-36 algorithm.
     *
     * @param gstin14 the first 14 characters of the GSTIN
     * @return the calculated check digit character
     * @throws IllegalArgumentException if input is null, not 14 characters, or contains invalid characters
     */
    public static char calculateCheckDigit(String gstin14) {
        // Input validation: algorithm requires exactly the 14 leading characters of the GSTIN
        if (gstin14 == null || gstin14.length() != 14) {
            throw new IllegalArgumentException("GSTIN prefix must be exactly 14 characters to calculate check digit");
        }
        String normalized = gstin14.trim().toUpperCase();
        int factor = 1;
        int totalSum = 0;
        int mod = 36;

        for (int i = 0; i < 14; i++) {
            char c = normalized.charAt(i);
            int code = BASE36_CHARS.indexOf(c);
            // Character set check: characters must belong to Base36 set [0-9A-Z]
            if (code == -1) {
                throw new IllegalArgumentException("Invalid character in GSTIN prefix: " + c);
            }
            int product = code * factor;
            // Alternating factor: toggles between 1 and 2 per character position
            factor = (factor == 1) ? 2 : 1;
            int quotient = product / mod;
            int remainder = product % mod;
            totalSum += (quotient + remainder);
        }

        int remainderTotal = totalSum % mod;
        int checkCode = (mod - remainderTotal) % mod;
        return BASE36_CHARS.charAt(checkCode);
    }

    /**
     * Validates the 15th check digit of a 15-character GSTIN using the Mod-36 algorithm.
     *
     * @param gstin raw or normalized 15-character GSTIN
     * @return true if the 15th character matches the calculated check digit, false otherwise
     */
    public static boolean validateCheckDigit(String gstin) {
        String normalized = normalizeGstin(gstin);
        // Precondition check: must have full 15 characters to compare calculated vs actual 15th character
        if (normalized == null || normalized.length() != 15) {
            return false;
        }
        try {
            char expected = calculateCheckDigit(normalized.substring(0, 14));
            // Checksum comparison: ensure 15th char matches the calculated Mod-36 check digit
            return normalized.charAt(14) == expected;
        } catch (IllegalArgumentException e) {
            // Catches any unexpected illegal characters in the 14-char prefix
            return false;
        }
    }

    /**
     * Masks a 15-character GSTIN for safe logging and telemetry (e.g., "03AAAP****1Z5").
     *
     * @param gstin raw or normalized GSTIN
     * @return safely masked GSTIN or "XX****XX" if blank/invalid
     */
    public static String maskGstin(String gstin) {
        // Fallback for empty/blank values to prevent logging null or throwing NPE
        if (StringUtils.isBlank(gstin)) {
            return "XX****XX";
        }
        String normalized = gstin.trim().toUpperCase();
        // If string is truncated or malformed (<6 chars), mask entirely to protect partial identifier
        if (normalized.length() < 6) {
            return "******";
        }
        // Standard 15-character GSTIN masking: preserve 2-digit state and 4 PAN prefix chars, mask center, retain last 3 chars
        if (normalized.length() == 15) {
            return normalized.substring(0, 6) + "****" + normalized.substring(12);
        }
        // Non-standard length fallback: keep leading 2 and trailing 2 characters
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 2);
    }

    /**
     * Extracts the 10-character PAN embedded within the 15-character GSTIN.
     *
     * @param gstin raw or normalized GSTIN
     * @return 10-character PAN or null if invalid
     */
    public static String extractPanFromGstin(String gstin) {
        String normalized = normalizeGstin(gstin);
        // Bounds check: GSTIN must be at least 12 characters to extract characters 2 through 12
        if (normalized == null || normalized.length() < 12) {
            return null;
        }
        return normalized.substring(2, 12);
    }

    /**
     * Extracts the 2-digit state code from the GSTIN.
     *
     * @param gstin raw or normalized GSTIN
     * @return 2-digit state code or null if invalid
     */
    public static String extractStateCode(String gstin) {
        String normalized = normalizeGstin(gstin);
        // Bounds check: must have at least 2 characters for state code prefix
        if (normalized == null || normalized.length() < 2) {
            return null;
        }
        return normalized.substring(0, 2);
    }

    /**
     * Resolves the Indian state name corresponding to the 2-digit GST state code.
     *
     * @param stateCode 2-digit state code
     * @return state name or "Unknown State" if not found
     */
    public static String resolveStateName(String stateCode) {
        // Null/blank guard: return graceful placeholder instead of NPE
        if (StringUtils.isBlank(stateCode)) {
            return "Unknown State";
        }
        return STATE_CODE_MAP.getOrDefault(stateCode.trim(), "Unknown State");
    }
}
