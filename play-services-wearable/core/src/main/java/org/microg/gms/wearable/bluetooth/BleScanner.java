package org.microg.gms.wearable.bluetooth;

public interface BleScanner {
    boolean isScanning();
    void stopScan();
    void startScan(String address, ScanListener listener);

    interface ScanListener {
        void onDeviceFound(String address);
        void onScanFailed(int errorCode);
    }
}
