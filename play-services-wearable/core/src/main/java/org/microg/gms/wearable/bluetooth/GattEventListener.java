package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

public interface GattEventListener {
    void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);
    void onServiceChanged();
    void onCharacteristicWritten(BluetoothGattCharacteristic characteristic);
    void onServicesDiscovered();

    default void onGattConnected() {}

    default void onGattDisconnected() {}
}
