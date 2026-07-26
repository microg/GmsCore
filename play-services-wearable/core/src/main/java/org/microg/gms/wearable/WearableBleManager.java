/*
 * Copyright (C) 2025 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages BLE (Bluetooth Low Energy) connections for WearOS device discovery and pairing.
 * Scans for WearOS devices using the Google Wear advertising UUID, establishes
 * GATT connections, and manages the lifecycle of connected nodes.
 */
public class WearableBleManager {
    private static final String TAG = "GmsWearBleMgr";

    // Google Wear OS BLE service UUIDs
    public static final ParcelUuid WEAR_SERVICE_UUID =
            ParcelUuid.fromString("0000FED9-0000-1000-8000-00805F9B34FB");
    public static final UUID WEAR_CHARACTERISTIC_UUID =
            UUID.fromString("0000FED9-0000-1000-8000-00805F9B34FB");

    // Standard WearOS BLE GATT service UUID (used for association)
    public static final String WEAR_BLE_SERVICE = "0000FE05-0000-1000-8000-00805F9B34FB";
    public static final String WEAR_BLE_CHARACTERISTIC = "0000FE06-0000-1000-8000-00805F9B34FB";

    private static final long SCAN_PERIOD_MS = 30000;
    private static final long RECONNECT_DELAY_MS = 10000;

    private final Context context;
    private final WearableImpl wearable;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private boolean scanning = false;
    private boolean running = false;

    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private List<BluetoothGatt> activeGattConnections = new ArrayList<>();

    public WearableBleManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
        this.handler = new Handler(Looper.getMainLooper());

        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    /**
     * Start scanning for WearOS devices via BLE
     */
    public void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available or not enabled");
            return;
        }

        if (scanning) {
            Log.d(TAG, "Already scanning");
            return;
        }

        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        Log.d(TAG, "Starting BLE scan for WearOS devices");
        scanning = true;
        running = true;

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(WEAR_SERVICE_UUID)
                .build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bleScanner.startScan(filters, settings, scanCallback);

        // Stop scanning after SCAN_PERIOD_MS
        handler.postDelayed(() -> {
            if (scanning) {
                stopScanning();
            }
        }, SCAN_PERIOD_MS);
    }

    /**
     * Stop scanning for WearOS devices
     */
    public void stopScanning() {
        if (!scanning || bleScanner == null) return;
        Log.d(TAG, "Stopping BLE scan");
        try {
            bleScanner.stopScan(scanCallback);
        } catch (Exception e) {
            Log.w(TAG, "Error stopping scan", e);
        }
        scanning = false;
    }

    /**
     * Try to connect to a discovered WearOS device via GATT
     */
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) return;
        Log.d(TAG, "Connecting to device: " + device.getAddress() + " " + device.getName());
        BluetoothGatt gatt = device.connectGatt(context, false, gattCallback);
        if (gatt != null) {
            activeGattConnections.add(gatt);
        }
    }

    /**
     * Disconnect from all active GATT connections
     */
    public void disconnectAll() {
        stopScanning();
        for (BluetoothGatt gatt : activeGattConnections) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing GATT connection", e);
            }
        }
        activeGattConnections.clear();
        discoveredDevices.clear();
        running = false;
    }

    /**
     * Create a ConnectionConfiguration from a discovered BLE device
     */
    public ConnectionConfiguration createConfigurationFromDevice(BluetoothDevice device) {
        String name = device.getName() != null ? device.getName() : "WearOS device";
        String address = device.getAddress();
        return new ConnectionConfiguration(
                name,           // name
                address,         // pairedBtAddress
                1,               // connectionType (1 = BLE)
                1,               // role (1 = companion)
                true,            // connectionEnabled
                null             // nodeId (will be assigned on connect)
        );
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && !discoveredDevices.contains(device)) {
                Log.d(TAG, "Discovered WearOS device: " + device.getName()
                        + " [" + device.getAddress() + "]");
                discoveredDevices.add(device);

                // Notify wearable about the discovered device via config
                ConnectionConfiguration config = createConfigurationFromDevice(device);
                wearable.createConnection(config);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
            scanning = false;
            // Retry after delay
            if (running) {
                handler.postDelayed(() -> startScanning(), RECONNECT_DELAY_MS);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT connected to: " + gatt.getDevice().getName());
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT disconnected from: " + gatt.getDevice().getName());
                if (running) {
                    // Attempt reconnection
                    handler.postDelayed(() -> {
                        if (running) {
                            gatt.connect();
                        }
                    }, RECONNECT_DELAY_MS);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for: " + gatt.getDevice().getName());
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "  Service: " + service.getUuid().toString());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                Log.d(TAG, "Characteristic read: " + characteristic.getUuid()
                        + " = " + bytesToHex(value));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.d(TAG, "Characteristic changed: " + characteristic.getUuid()
                    + " = " + bytesToHex(value));
        }
    };

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}