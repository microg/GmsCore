package org.microg.gms.wearable.bluetooth;

public interface BluetoothGattHelper {
    boolean isConnected();

    void disconnect() throws BleException;
    void discoverServices() throws BleException;
    void refreshGatt();
    void setGattEventListener(GattEventListener listener);
}
