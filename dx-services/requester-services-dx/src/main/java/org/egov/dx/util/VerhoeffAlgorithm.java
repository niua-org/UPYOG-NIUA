package org.egov.dx.util;

/**
 * Standard UIDAI Verhoeff algorithm implementation for validating Aadhaar card checksums.
 *
 * <p>UIDAI Aadhaar numbers are 12 digits where the 12th digit is a checksum computed using the
 * dihedral group D5 on decimal digits (the Verhoeff check).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class VerhoeffAlgorithm {

    /** Multiplication table (dihedral group D5). */
    private static final int[][] D = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    /** Permutation table based on powers of permutation (1 5 8 9 4 2 7 0)(3 6). */
    private static final int[][] P = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    private VerhoeffAlgorithm() {
        // Utility class
    }

    /**
     * Validates whether a 12-digit numeric Aadhaar string satisfies the Verhoeff checksum.
     *
     * @param num the 12-digit string to validate
     * @return true if valid Verhoeff checksum and non-repeating digits, false otherwise
     */
    public static boolean validateVerhoeff(String num) {
        if (num == null || num.trim().length() != 12) {
            return false;
        }

        String trimmed = num.trim();

        // Reject trivial repetitive numbers like 222222222222, 333333333333, etc.
        if (trimmed.chars().distinct().count() <= 1) {
            return false;
        }

        int c = 0;
        int[] myArray = stringToReversedIntArray(trimmed);
        for (int i = 0; i < myArray.length; i++) {
            c = D[c][P[i % 8][myArray[i]]];
        }
        return c == 0;
    }

    private static int[] stringToReversedIntArray(String num) {
        int[] myArray = new int[num.length()];
        for (int i = 0; i < num.length(); i++) {
            myArray[i] = Integer.parseInt(num.substring(i, i + 1));
        }
        int[] reversed = new int[myArray.length];
        for (int i = 0; i < myArray.length; i++) {
            reversed[i] = myArray[myArray.length - (i + 1)];
        }
        return reversed;
    }
}
