package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.io.Closeable;
import java.util.UUID;

public class BleClientConnection extends Thread implements Closeable {
    private static final String TAG = "GmsWearBleConn";

    private static final UUID WEAR_SERVICE_UUID = UUID.fromString("0000fef7-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final ConnectionConfiguration config;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private volatile boolean running = true;
    private BluetoothGatt bluetoothGatt;

    public BleClientConnection(Context context, ConnectionConfiguration config, BluetoothAdapter adapter) {
        super("BleClientConn-" + config.address);
        this.context = context;
        this.config = config;
        this.bluetoothAdapter = adapter;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void run() {
        Log.d(TAG, "BLE connection thread started for " + config.address);

        while (running && !isInterrupted()) {
            try {
                connect();
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "BLE connection interrupted");
                break;
            } catch (Exception e) {
                Log.w(TAG, "BLE connection error: " + e.getMessage(), e);
                disconnect();

                if (running) {
                    try {
                        Thread.sleep(5000); // Wait before retry
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }

        disconnect();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connect() {
        if (!running || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new IllegalStateException("Bluetooth not available");
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(config.address);
        if (device == null) {
            throw new IllegalStateException("Could not get remote device");
        }

        Log.d(TAG, "Connecting to BLE device " + config.address);

        handler.post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback,
                        BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback);
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void disconnect() {
        if (bluetoothGatt != null) {
            handler.post(() -> {
                try {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error disconnecting GATT", e);
                }
                bluetoothGatt = null;
            });
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "BLE connected to " + config.address);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "BLE disconnected from " + config.address);
                synchronized (BleClientConnection.this) {
                    BleClientConnection.this.notifyAll();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE services discovered for " + config.address);
                // Handle service discovery and setup characteristics
            } else {
                Log.w(TAG, "BLE service discovery failed: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE characteristic read");
                // Handle characteristic read
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE characteristic written");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "BLE characteristic changed");
            // Handle notifications
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void close() {
        Log.d(TAG, "Closing BLE connection for " + config.address);
        running = false;
        interrupt();
        disconnect();
    }

}
