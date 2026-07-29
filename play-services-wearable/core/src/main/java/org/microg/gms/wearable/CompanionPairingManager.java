/*
 * Copyright (C) 2024 microG Project Team
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.microg.gms.common.PackageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Companion Device pairing for Wear OS 3+ devices.
 *
 * Wear OS 3+ uses the CompanionDeviceManager API to discover and pair
 * with smartwatches. This manager handles BLE-based discovery of
 * Wear OS watches and manages the pairing lifecycle, bridging the
 * companion device pairing protocol with the existing Wearable
 * connection infrastructure.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@SuppressLint({"MissingPermission", "NewApi"})
public class CompanionPairingManager {
    private static final String TAG = "GmsWearCompanion";
    private static final String PREF_NAME = "companion_pairing";
    private static final String KEY_PAIRED_DEVICES = "paired_devices";

    /**
     * Wear OS BLE service UUIDs used for device discovery.
     */
    private static final UUID WEAR_OS_SERVICE_UUID =
            UUID.fromString("0000fce2-0000-1000-8000-00805f9b34fb");

    /**
     * Additional BLE service UUIDs commonly advertised by Wear OS devices.
     */
    private static final UUID DEVICE_INFORMATION_SERVICE_UUID =
            UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE_UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");

    /**
     * Known Wear OS manufacturer prefixes for device name filtering.
     */
    private static final String[] WEAR_OS_NAME_PREFIXES = {
            "Galaxy Watch", "Pixel Watch", "Wear OS", "Fossil",
            "TicWatch", "Mobvoi", "Skagen", "Michael Kors",
            "Montblanc", "TAG Heuer", "Suunto", "Casio",
            "Oppo Watch", "Xiaomi Watch"
    };

    private final Context context;
    private final WearableImpl wearable;
    private final String packageName;
    private final SharedPreferences preferences;
    private final Handler mainHandler;
    private final Map<String, CompanionDevice> pairedDevices;
    private final Set<PairingListener> listeners;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BroadcastReceiver bluetoothReceiver;
    private boolean isScanning = false;

    /**
     * Creates a new CompanionPairingManager.
     *
     * @param context    The application context
     * @param wearable   The WearableImpl instance for connection management
     * @param packageName The calling package name
     */
    public CompanionPairingManager(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.pairedDevices = new HashMap<>();
        this.listeners = new HashSet<>();

        loadPairedDevices();
        initBluetooth();
        registerBluetoothReceiver();
    }

    /**
     * Represents a paired companion device (Wear OS watch).
     */
    public static class CompanionDevice {
        public final String macAddress;
        public final String deviceName;
        public final String nodeId;
        public final int deviceType;
        public final long pairedTimestamp;
        public boolean isConnected;

        public CompanionDevice(String macAddress, String deviceName,
                               String nodeId, int deviceType, long pairedTimestamp) {
            this.macAddress = macAddress;
            this.deviceName = deviceName;
            this.nodeId = nodeId;
            this.deviceType = deviceType;
            this.pairedTimestamp = pairedTimestamp;
            this.isConnected = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompanionDevice that = (CompanionDevice) o;
            return macAddress.equals(that.macAddress);
        }

        @Override
        public int hashCode() {
            return macAddress.hashCode();
        }
    }

    /**
     * Listener interface for companion device events.
     */
    public interface PairingListener {
        void onDeviceDiscovered(CompanionDevice device);
        void onDevicePaired(CompanionDevice device);
        void onDeviceUnpaired(String macAddress);
        void onScanStarted();
        void onScanStopped();
        void onError(String message);
    }

    // === Bluetooth Initialization ===

    private void initBluetooth() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize Bluetooth", e);
        }
    }

    private void registerBluetoothReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                    if (device != null) {
                        onDeviceConnected(device);
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                    if (device != null) {
                        onDeviceDisconnected(device);
                    }
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_ON) {
                        initBluetooth();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothReceiver, filter);
    }

    // === Public API ===

    /**
     * Registers a pairing listener for companion device events.
     */
    public void addListener(PairingListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered pairing listener.
     */
    public void removeListener(PairingListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns the list of currently paired companion devices.
     */
    public List<CompanionDevice> getPairedDevices() {
        synchronized (pairedDevices) {
            return new ArrayList<>(pairedDevices.values());
        }
    }

    /**
     * Checks if Bluetooth is available and enabled.
     */
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Checks if BLE scanning is supported on this device.
     */
    public boolean isBleScanningSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && bleScanner != null;
    }

    /**
     * Starts BLE scanning for Wear OS companion devices.
     * Requires BLUETOOTH_SCAN and BLUETOOTH_ADMIN permissions.
     */
    public void startScanning() {
        if (!isBluetoothAvailable()) {
            notifyError("Bluetooth is not available or not enabled");
            return;
        }

        if (!isBleScanningSupported()) {
            notifyError("BLE scanning not supported on this device");
            return;
        }

        if (isScanning) {
            Log.d(TAG, "Already scanning, ignoring start request");
            return;
        }

        isScanning = true;
        notifyScanStarted();

        List<ScanFilter> filters = buildScanFilters();
        ScanSettings settings = buildScanSettings();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                bleScanner.startScan(filters, settings, bleScanCallback);
                Log.d(TAG, "BLE scan started for Wear OS devices");
            } catch (SecurityException e) {
                Log.w(TAG, "Missing Bluetooth permissions", e);
                notifyError("Missing Bluetooth permissions for BLE scanning");
                isScanning = false;
            }
        }
    }

    /**
     * Stops the current BLE scanning operation.
     */
    @SuppressLint({"MissingPermission", "NewApi"})
    public void stopScanning() {
        if (isScanning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                bleScanner.stopScan(bleScanCallback);
            } catch (SecurityException e) {
                Log.w(TAG, "Error stopping scan", e);
            }
        }
        isScanning = false;
        notifyScanStopped();
        Log.d(TAG, "BLE scan stopped");
    }

    /**
     * Pairs a discovered device as a Wear OS companion.
     * In production, this would trigger the CompanionDeviceManager pairing flow.
     *
     * @param macAddress The Bluetooth MAC address of the device
     * @param deviceName The human-readable device name
     */
    public void pairDevice(String macAddress, String deviceName) {
        Log.d(TAG, "Pairing device: " + deviceName + " (" + macAddress + ")");

        String nodeId = "companion:" + macAddress.replace(":", "").toLowerCase();

        CompanionDevice device = new CompanionDevice(
                macAddress,
                deviceName,
                nodeId,
                1, // TYPE_WATCH
                System.currentTimeMillis()
        );

        synchronized (pairedDevices) {
            pairedDevices.put(macAddress, device);
        }
        savePairedDevices();

        // Create a connection configuration for the paired device
        // This allows the existing wearable infrastructure to manage the connection
        wearable.createConnection(
                new com.google.android.gms.wearable.ConnectionConfiguration(
                        "wear_companion_" + nodeId,
                        macAddress,
                        1, /* TYPE_BLUETOOTH */
                        2, // ROLE_SERVER
                        false,
                        nodeId
                )
        );
        wearable.enableConnection("wear_companion_" + nodeId);

        notifyDevicePaired(device);
        Log.i(TAG, "Device paired: " + deviceName + " (" + nodeId + ")");
    }

    /**
     * Unpairs a previously paired companion device.
     *
     * @param macAddress The Bluetooth MAC address to unpair
     */
    public void unpairDevice(String macAddress) {
        Log.d(TAG, "Unpairing device: " + macAddress);

        CompanionDevice device;
        synchronized (pairedDevices) {
            device = pairedDevices.remove(macAddress);
        }

        if (device != null) {
            // Remove the corresponding connection configuration
            wearable.deleteConnection("wear_companion_" + device.nodeId);
            savePairedDevices();
            notifyDeviceUnpaired(macAddress);
            Log.i(TAG, "Device unpaired: " + device.deviceName);
        }
    }

    /**
     * Cleans up resources. Should be called when the service is destroyed.
     */
    public void destroy() {
        stopScanning();
        if (bluetoothReceiver != null) {
            try {
                context.unregisterReceiver(bluetoothReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver already unregistered", e);
            }
        }
        Log.d(TAG, "CompanionPairingManager destroyed");
    }

    // === Internal Methods ===

    /**
     * Builds BLE ScanFilters for Wear OS device discovery.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();

        // Filter for Wear OS service UUID
        ScanFilter wearOsFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(WEAR_OS_SERVICE_UUID))
                .build();
        filters.add(wearOsFilter);

        // Filter for devices advertising Battery Service (common on watches)
        ScanFilter batteryFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(BATTERY_SERVICE_UUID))
                .build();
        filters.add(batteryFilter);

        return filters;
    }

    /**
     * Builds BLE ScanSettings optimized for Wear OS discovery.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
    }

    /**
     * Checks if a BluetoothDevice name indicates a Wear OS device.
     */
    private boolean isWearOsDevice(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) {
            return false;
        }

        // Direct "Wear" indicator
        if (deviceName.toLowerCase().contains("wear")) {
            return true;
        }

        // Known Wear OS prefixes
        for (String prefix : WEAR_OS_NAME_PREFIXES) {
            if (deviceName.toLowerCase().startsWith(prefix.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles a device connecting via Bluetooth ACL.
     */
    private void onDeviceConnected(BluetoothDevice device) {
        String mac = device.getAddress();
        synchronized (pairedDevices) {
            CompanionDevice companion = pairedDevices.get(mac);
            if (companion != null) {
                companion.isConnected = true;
                Log.d(TAG, "Companion device connected: " + companion.deviceName);
            }
        }
    }

    /**
     * Handles a device disconnecting via Bluetooth ACL.
     */
    private void onDeviceDisconnected(BluetoothDevice device) {
        String mac = device.getAddress();
        synchronized (pairedDevices) {
            CompanionDevice companion = pairedDevices.get(mac);
            if (companion != null) {
                companion.isConnected = false;
                Log.d(TAG, "Companion device disconnected: " + companion.deviceName);
            }
        }
    }

    // === BLE Scan Callback ===
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if (isWearOsDevice(deviceName)) {
                Log.d(TAG, "Wear OS device found: " + deviceName
                        + " (" + device.getAddress() + ") RSSI: " + result.getRssi());

                // Only notify if not already paired
                synchronized (pairedDevices) {
                    if (!pairedDevices.containsKey(device.getAddress())) {
                        CompanionDevice companionDevice = new CompanionDevice(
                                device.getAddress(),
                                deviceName != null ? deviceName : "Unknown Wear OS",
                                null, // No nodeId yet until paired
                                1,
                                0
                        );
                        notifyDeviceDiscovered(companionDevice);
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
            isScanning = false;
            notifyError("BLE scan failed with error code: " + errorCode);
        }
    };

    // === Persistence ===

    private void loadPairedDevices() {
        String json = preferences.getString(KEY_PAIRED_DEVICES, null);
        if (json == null || json.isEmpty()) return;

        try {
            // Simple delimiter-based format: mac|name|nodeId|type|timestamp;;mac|name|...
            String[] entries = json.split(";;");
            for (String entry : entries) {
                if (entry.isEmpty()) continue;
                String[] parts = entry.split("\\|");
                if (parts.length >= 5) {
                    CompanionDevice device = new CompanionDevice(
                            parts[0], parts[1], parts[2],
                            Integer.parseInt(parts[3]), Long.parseLong(parts[4])
                    );
                    pairedDevices.put(device.macAddress, device);
                }
            }
            Log.d(TAG, "Loaded " + pairedDevices.size() + " paired companion devices");
        } catch (Exception e) {
            Log.w(TAG, "Failed to load paired devices", e);
        }
    }

    private void savePairedDevices() {
        StringBuilder sb = new StringBuilder();
        synchronized (pairedDevices) {
            for (CompanionDevice device : pairedDevices.values()) {
                if (sb.length() > 0) sb.append(";;");
                sb.append(device.macAddress).append("|")
                        .append(device.deviceName != null ? device.deviceName : "").append("|")
                        .append(device.nodeId != null ? device.nodeId : "").append("|")
                        .append(device.deviceType).append("|")
                        .append(device.pairedTimestamp);
            }
        }
        preferences.edit().putString(KEY_PAIRED_DEVICES, sb.toString()).apply();
    }

    // === Listener Notifications ===

    private void notifyDeviceDiscovered(final CompanionDevice device) {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onDeviceDiscovered(device);
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying listener of device discovery", e);
                    }
                }
            }
        });
    }

    private void notifyDevicePaired(final CompanionDevice device) {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onDevicePaired(device);
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying listener of pairing", e);
                    }
                }
            }
        });
    }

    private void notifyDeviceUnpaired(final String macAddress) {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onDeviceUnpaired(macAddress);
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying listener of unpairing", e);
                    }
                }
            }
        });
    }

    private void notifyScanStarted() {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onScanStarted();
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying scan start", e);
                    }
                }
            }
        });
    }

    private void notifyScanStopped() {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onScanStopped();
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying scan stop", e);
                    }
                }
            }
        });
    }

    private void notifyError(final String message) {
        mainHandler.post(() -> {
            synchronized (listeners) {
                for (PairingListener listener : listeners) {
                    try {
                        listener.onError(message);
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying listener of error", e);
                    }
                }
            }
        });
    }
}
