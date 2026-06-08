package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public interface BluetoothGattHelper {
    boolean isConnected();

    void connect(BluetoothDevice device);
    void disconnect() throws BleException;
    void discoverServices() throws BleException;
    void refreshGatt();
    void setGattEventListener(GattEventListener listener);

    BluetoothGatt getGatt();
    void close();
}
