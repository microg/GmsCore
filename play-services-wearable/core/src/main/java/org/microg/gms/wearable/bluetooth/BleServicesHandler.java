package org.microg.gms.wearable.bluetooth;

public interface BleServicesHandler {
    void cleanup();
    void updateCurrentTime() throws BleException;
}
