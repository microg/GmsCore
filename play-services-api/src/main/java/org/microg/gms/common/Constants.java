package org.microg.gms.common;

public class Constants {
    /**
     * This is the highest version that was looked at during development.
     * Does not necessarily mean anything.
     */
    public static final int MAX_REFERENCE_VERSION = 6599436;
    public static final String ACTION_GMS_LOCATION_MANAGER_SERVICE_START = "com.google.android.location.internal.GoogleLocationManagerService.START";
    public static final String ACTION_GMS_PEOPLE_SERVICE_START = "com.google.android.gms.people.service.START";
    public static final String KEY_MOCK_LOCATION = "mockLocation";
    public static final String DEFAULT_ACCOUNT = "<<default account>>";
    public static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    public static final String GMS_PACKAGE_SIGNATURE_SHA1 = "38918a453d07199354f8b19af05ec6562ced5788";

    /**
     * No base map tiles.
     */
    public static final int MAP_TYPE_NONE = 0;

    /**
     * Basic maps.
     */
    public static final int MAP_TYPE_NORMAL = 1;

    /**
     * Satellite maps with no labels.
     */
    public static final int MAP_TYPE_SATELLITE = 2;

    /**
     * Terrain maps.
     */
    public static final int MAP_TYPE_TERRAIN = 3;

    /**
     * Satellite maps with a transparent layer of major streets.
     */
    public static final int MAP_TYPE_HYBRID = 4;
}
