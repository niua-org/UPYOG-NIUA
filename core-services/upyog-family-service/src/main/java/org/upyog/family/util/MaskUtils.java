package org.upyog.family.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Common utility class providing standardized masking methods for Personally
 * Identifiable Information (PII) such as mobile numbers, Aadhaar numbers,
 * reference identifiers, emails, and consent tokens across the UPYOG Family service.
 */
public final class MaskUtils {

    /** Default mask placeholder for short, blank, or sensitive values. */
    public static final String DEFAULT_MASK = "******";

    /** Standard UIDAI Aadhaar 8-character mask prefix. */
    public static final String AADHAAR_MASK_PREFIX = "XXXXXXXX";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MaskUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Masks a mobile number revealing the first 2 and last 2 digits
     * (e.g. {@code 98******10}).
     *
     * @param mobile raw mobile number
     * @param defaultIfBlank value returned if mobile is null or blank
     * @return masked mobile string, or {@code defaultIfBlank} if blank
     */
    public static String maskMobile(String mobile, String defaultIfBlank) {
        if (StringUtils.isBlank(mobile)) {
            return defaultIfBlank;
        }
        String trimmed = mobile.trim();
        if (trimmed.length() >= 6) {
            return trimmed.substring(0, 2) + DEFAULT_MASK + trimmed.substring(trimmed.length() - 2);
        }
        return DEFAULT_MASK;
    }

    /**
     * Masks a mobile number revealing the first 2 and last 2 digits (e.g. {@code 98******10}),
     * returning {@code null} if the input is null or blank.
     *
     * @param mobile raw mobile number
     * @return masked mobile string, or {@code null} if blank
     */
    public static String maskMobile(String mobile) {
        return maskMobile(mobile, null);
    }

    /**
     * Masks a mobile number for OTP dispatch hint purposes, revealing only the last 4 digits
     * (e.g. {@code ******3210}).
     *
     * @param mobile raw mobile number
     * @param defaultIfBlank fallback when input is blank
     * @return masked mobile string revealing last 4 digits
     */
    public static String maskMobileLast4(String mobile, String defaultIfBlank) {
        if (StringUtils.isBlank(mobile)) {
            return defaultIfBlank;
        }
        String trimmed = mobile.trim();
        if (trimmed.length() <= 4) {
            return DEFAULT_MASK + trimmed;
        }
        return DEFAULT_MASK + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * Masks a mobile number for OTP dispatch hint purposes, revealing only the last 4 digits
     * (e.g. {@code ******3210}), returning {@value #DEFAULT_MASK} if blank.
     *
     * @param mobile raw mobile number
     * @return masked mobile string revealing last 4 digits
     */
    public static String maskMobileLast4(String mobile) {
        return maskMobileLast4(mobile, DEFAULT_MASK);
    }

    /**
     * Masks a 12-digit Aadhaar number revealing only the last 4 digits
     * (e.g. {@code XXXXXXXX1234}).
     *
     * @param aadhaar raw 12-digit or partially masked Aadhaar number
     * @param defaultIfBlank value returned if aadhaar is null or blank
     * @return masked Aadhaar string (e.g. {@code XXXXXXXX1234}), or {@code defaultIfBlank} if blank
     */
    public static String maskAadhaar(String aadhaar, String defaultIfBlank) {
        if (StringUtils.isBlank(aadhaar)) {
            return defaultIfBlank;
        }
        String clean = aadhaar.trim().replaceAll("\\s+", "");
        if (clean.startsWith(AADHAAR_MASK_PREFIX) || clean.toLowerCase().startsWith("xxxxxxxx")) {
            return clean;
        }
        if (clean.length() == 12) {
            return AADHAAR_MASK_PREFIX + clean.substring(8);
        }
        return AADHAAR_MASK_PREFIX + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
    }

    /**
     * Masks a 12-digit Aadhaar number revealing only the last 4 digits (e.g. {@code XXXXXXXX1234}),
     * returning {@code null} if the input is null or blank.
     *
     * @param aadhaar raw 12-digit or partially masked Aadhaar number
     * @return masked Aadhaar string, or {@code null} if blank
     */
    public static String maskAadhaar(String aadhaar) {
        return maskAadhaar(aadhaar, null);
    }

    /**
     * Masks arbitrary transaction, reference, or record identifiers revealing only
     * the last 4 characters (e.g. {@code ******8662}).
     *
     * @param id identifier string (e.g. e-KYC reference ID, tracking ID)
     * @param defaultIfBlank fallback value if input is blank
     * @return masked identifier, or {@code defaultIfBlank} if blank
     */
    public static String maskIdentifier(String id, String defaultIfBlank) {
        if (StringUtils.isBlank(id)) {
            return defaultIfBlank;
        }
        String trimmed = id.trim();
        return trimmed.length() > 4 ? DEFAULT_MASK + trimmed.substring(trimmed.length() - 4) : DEFAULT_MASK;
    }

    /**
     * Masks arbitrary transaction, reference, or record identifiers revealing only
     * the last 4 characters (e.g. {@code ******8662}), returning {@value #DEFAULT_MASK} if blank.
     *
     * @param id identifier string (e.g. e-KYC reference ID, tracking ID)
     * @return masked identifier string
     */
    public static String maskIdentifier(String id) {
        return maskIdentifier(id, DEFAULT_MASK);
    }

    /**
     * Masks security or consent tokens revealing only the first 8 and last 4 characters
     * (e.g. {@code a1b2c3d4...ef01}).
     *
     * @param token bearer token, consent token, or auth credential
     * @param defaultIfBlank fallback value if input is blank
     * @return masked token representation, or {@code defaultIfBlank} if blank
     */
    public static String maskToken(String token, String defaultIfBlank) {
        if (StringUtils.isBlank(token)) {
            return defaultIfBlank;
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return DEFAULT_MASK;
        }
        return trimmed.substring(0, 8) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * Masks security or consent tokens revealing only the first 8 and last 4 characters
     * (e.g. {@code a1b2c3d4...ef01}), returning {@value #DEFAULT_MASK} if blank.
     *
     * @param token bearer token, consent token, or auth credential
     * @return masked token representation
     */
    public static String maskToken(String token) {
        return maskToken(token, DEFAULT_MASK);
    }

    /**
     * Masks an email address revealing only the first and last character of the username
     * (e.g. {@code j***e@domain.com}).
     *
     * @param email email address to mask
     * @param defaultIfBlank fallback value if input is blank
     * @return masked email address, or {@code defaultIfBlank} if blank
     */
    public static String maskEmail(String email, String defaultIfBlank) {
        if (StringUtils.isBlank(email)) {
            return defaultIfBlank;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 1) {
            return DEFAULT_MASK + (atIndex >= 0 ? trimmed.substring(atIndex) : "");
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "*" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * Masks an email address revealing only the first and last character of the username
     * (e.g. {@code j***e@domain.com}), returning {@code null} if blank.
     *
     * @param email email address to mask
     * @return masked email address, or {@code null} if blank
     */
    public static String maskEmail(String email) {
        return maskEmail(email, null);
    }
}
