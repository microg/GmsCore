package com.mapbox.android.accounts.v1;

import java.util.UUID;

/**
 * This class is used from within the Mapbox library
 */
@SuppressWarnings("unused")
public class MapboxAccounts {
    public final static String SKU_ID_MAPS_MAUS = "00";
    private final static String MAPS_SKU_PREFIX = "100";

    /**
     * Generates random UUID without hyphens
     */
    public static String obtainEndUserId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 8) + uuid.substring(9, 13) + uuid.substring(14, 18) + uuid.substring(19, 23) + uuid.substring(24, 36);
    }

    /**
     * Generates a SKU which is the user id prefixed with 100 and base-36 encoded timestamp
     */
    public static String obtainMapsSkuUserToken(String userId) {
        String stamp = Long.toString(System.currentTimeMillis(), 36);
        return MAPS_SKU_PREFIX + stamp + userId;
    }
}
