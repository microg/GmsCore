package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleDeviceDiscoverer {
    private static final String TAG = "BleDeviceDiscoverer";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Map<BluetoothDevice, ScanFilter> deviceFilters = new HashMap<>();
    private final Map<BluetoothDevice, DeviceDiscoveryCallback> deviceCallbacks = new HashMap<>();
    private final Object lock = new Object();

    private BluetoothLeScanner scanner;
    private boolean isScanning = false;
    private ScanCallback scanCallback;

    public interface DeviceDiscoveryCallback {
        void onDeviceDiscovered(BluetoothDevice device);
    }

    public BleDeviceDiscoverer(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bluetoothAdapter != null) {
                this.scanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void addDevice(BluetoothDevice device, DeviceDiscoveryCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "BLE scanning not supported on Android < 5.0");
            return;
        }

        synchronized (lock) {
            if (deviceFilters.containsKey(device)) {
                Log.d(TAG, "Device already being watched: " + device.getAddress());
                return;
            }

            Log.d(TAG, "Adding device to watch: " + device.getAddress());

            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(device.getAddress())
                    .build();

            deviceFilters.put(device, filter);
            deviceCallbacks.put(device, callback);

            updateScanning();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void removeDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        synchronized (lock) {
            if (!deviceFilters.containsKey(device)) {
                Log.d(TAG, "Device not being watched: " + device.getAddress());
                return;
            }

            Log.d(TAG, "Removing device from watch: " + device.getAddress());

            deviceFilters.remove(device);
            deviceCallbacks.remove(device);

            updateScanning();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void clear() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        synchronized (lock) {
            Log.d(TAG, "Clearing all devices");
            deviceFilters.clear();
            deviceCallbacks.clear();
            stopScanning();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void updateScanning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (deviceFilters.isEmpty()) {
            stopScanning();
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available, cannot start scanning");
            return;
        }

        if (scanner == null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                Log.w(TAG, "BluetoothLeScanner not available");
                return;
            }
        }

        if (isScanning) {
            stopScanning();
        }

        startScanning();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanning() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        try {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    handleScanResult(result);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult result : results) {
                        handleScanResult(result);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "BLE scan failed with error: " + errorCode);
                    synchronized (lock) {
                        isScanning = false;
                    }
                }
            };

            List<ScanFilter> filters = new ArrayList<>(deviceFilters.values());

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();

            scanner.startScan(filters, settings, scanCallback);
            isScanning = true;

            Log.d(TAG, String.format("Started BLE scanning for %d devices", filters.size()));

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for BLE scanning", e);
        } catch (Exception e) {
            Log.e(TAG, "Error starting BLE scan", e);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopScanning() {
        if (!isScanning) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        try {
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback);
                Log.d(TAG, "Stopped BLE scanning");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error stopping BLE scan", e);
        } finally {
            isScanning = false;
            scanCallback = null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void handleScanResult(ScanResult result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (result == null || result.getDevice() == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();

        synchronized (lock) {
            DeviceDiscoveryCallback callback = deviceCallbacks.get(device);
            if (callback != null) {
                Log.d(TAG, String.format("Device discovered: %s (RSSI: %d)",
                        device.getAddress(), result.getRssi()));

                try {
                    callback.onDeviceDiscovered(device);
                } catch (Exception e) {
                    Log.e(TAG, "Error in discovery callback", e);
                }

                deviceFilters.remove(device);
                deviceCallbacks.remove(device);

                if (deviceFilters.isEmpty()) {
                    stopScanning();
                }
            }
        }
    }

    public boolean isScanning() {
        synchronized (lock) {
            return isScanning;
        }
    }

    public int getWatchedDeviceCount() {
        synchronized (lock) {
            return deviceFilters.size();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void shutdown() {
        clear();
    }
}