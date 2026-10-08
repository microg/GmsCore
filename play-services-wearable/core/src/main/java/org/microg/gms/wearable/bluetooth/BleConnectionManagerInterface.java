package org.microg.gms.wearable.bluetooth;

import com.google.android.gms.wearable.ConnectionConfiguration;

public interface BleConnectionManagerInterface {
    void updateConfiguration(ConnectionConfiguration config);
    void quit();
    void quitSafely();
}
