package org.upyog.dx.upyogdata.util;

import java.util.UUID;

/**
 * Utility class providing common helper methods used by the
 * UPYOG Data DX service.
 */

public class DxUtils {

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
