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
     * Validates whether a normalized GSTIN matches the standard 15-character GSTIN format.
     *
     * @param gstin raw or normalized GSTIN
     * @return true if valid format, false otherwise
     */
    public static boolean isValidGstin(String gstin) {
        String normalized = normalizeGstin(gstin);
        if (normalized == null || normalized.length() != 15) {
            return false;
        }
        return GSTIN_PATTERN.matcher(normalized).matches();
    }

    /**
     * Masks a 15-character GSTIN for safe logging and telemetry (e.g., "03AAAP****1Z5").
     *
     * @param gstin raw or normalized GSTIN
     * @return safely masked GSTIN or "XX****XX" if blank/invalid
     */
    public static String maskGstin(String gstin) {
        if (StringUtils.isBlank(gstin)) {
            return "XX****XX";
        }
        String normalized = gstin.trim().toUpperCase();
        if (normalized.length() < 6) {
            return "******";
        }
        if (normalized.length() == 15) {
            return normalized.substring(0, 6) + "****" + normalized.substring(12);
        }
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
        if (StringUtils.isBlank(stateCode)) {
            return "Unknown State";
        }
        return STATE_CODE_MAP.getOrDefault(stateCode.trim(), "Unknown State");
    }
}
