package org.microg.gms.wearable;

public class WearOsSupport {
    public static boolean isSupported() {
        return true;
    }

    public void pairDevice(String deviceId) {
        System.out.println("Pairing WearOS device: " + deviceId);
    }
}
