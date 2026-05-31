package org.microg.gms.maps;

public class MapsCompat {
    public static boolean isAvailable() {
        try {
            Class.forName("com.google.android.gms.maps.MapView");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}