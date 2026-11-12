package org.egov.user.util;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;

/**
 * Normalization and equality helpers for 12-digit Aadhaar numbers stored in {@code eg_user},
 * and utilities for generating valid 10-digit Indian mobile numbers.
 *
 * <p>TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class AadhaarNumberUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AadhaarNumberUtils() {
    }

    /**
     * Extracts exactly twelve digits when the input is a raw or legacy masked value.
     *
     * @param value raw 12-digit, masked {@code XXXXXXXX####}, or mixed input
     * @return normalized 12-digit string, or {@code null} if fewer than 12 digits are available
     */
    public static String normalizeToTwelveDigits(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("^[0-9]{12}$")) {
            return trimmed;
        }
        String upper = trimmed.toUpperCase();
        if (upper.startsWith("XXXXXXXX") && upper.length() == 12) {
            String lastFour = upper.substring(8);
            if (lastFour.matches("[0-9]{4}")) {
                return null;
            }
        }
        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() == 12) {
            return digitsOnly;
        }
        return null;
    }

    /**
     * Compares two Aadhaar representations on full 12-digit equality after normalization.
     *
     * @param left  first value
     * @param right second value
     * @return {@code true} when both normalize to the same 12-digit number
     */
    public static boolean sameTwelveDigits(String left, String right) {
        String normalizedLeft = normalizeToTwelveDigits(left);
        String normalizedRight = normalizeToTwelveDigits(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    /**
     * Generates a random valid 10-digit Indian mobile number matching {@code ^[6-9][0-9]{9}$}.
     *
     * @return 10-digit Indian mobile number string
     */
    public static String generateRandomIndianMobileNumber() {
        int firstDigit = 6 + SECURE_RANDOM.nextInt(4); // 6, 7, 8, or 9
        long remaining9Digits = (long) (SECURE_RANDOM.nextDouble() * 1_000_000_000L);
        return String.format("%d%09d", firstDigit, remaining9Digits);
    }
}
