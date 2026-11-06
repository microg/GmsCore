package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.Collections;
import java.util.List;

class BleScannerImpl implements BleScanner {
    private static final String TAG = "BleScannerImpl";

    private final BluetoothAdapter btAdapter;
    private volatile ScanCallback activeCb;
    private volatile boolean scanning;

    // For API 19
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private volatile ScanListener currentListener;
    private volatile String targetAddress;

    BleScannerImpl(BluetoothAdapter btAdapter) {
        this.btAdapter = btAdapter;
    }

    @Override
    public boolean isScanning() {
        return scanning;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScan() {
        if (!scanning) return;
        BluetoothLeScanner scanner = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = btAdapter.getBluetoothLeScanner();
            if (scanner != null && activeCb != null) {
                try {
                    scanner.stopScan(activeCb);
                } catch (Exception e) {
                    Log.w(TAG, "stopScan failed: " + e);
                }
            }
            activeCb = null;
            scanning = false;
        } else {
            if (leScanCallback != null) {
                btAdapter.stopLeScan(leScanCallback);
                leScanCallback = null;
            }
            currentListener = null;
            targetAddress = null;
        }
        Log.d(TAG, "scan stopped");
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startScan(String address, ScanListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startScanOld(address, listener);
        } else {
            BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
            if (scanner == null || !btAdapter.isEnabled()) {
                Log.w(TAG, "BLE scanner unavailable");
                listener.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
                return;
            }

            List<ScanFilter> filters = Collections.singletonList(
                    new ScanFilter.Builder().setDeviceAddress(address).build());

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            activeCb = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    String found = result.getDevice().getAddress();
                    Log.d(TAG, "device found: " + found);
                    listener.onDeviceFound(found);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "scan failed: " + errorCode);
                    scanning = false;
                    listener.onScanFailed(errorCode);
                }
            };

            try {
                scanner.startScan(filters, settings, activeCb);
                scanning = true;
                Log.d(TAG, "scan started for " + address);
            } catch (Exception e) {
                Log.e(TAG, "startScan threw: " + e);
                activeCb = null;
                listener.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanOld(final String address, final ScanListener listener) {
        if (scanning) stopScan();
        targetAddress = address;
        currentListener = listener;

        leScanCallback = (device, rssi, scanRecord) -> {
            if (device == null) return;
            final String found = device.getAddress();
            if (targetAddress == null || targetAddress.equalsIgnoreCase(found)) {
                Log.d(TAG, "startScanOld: " + found);
                ScanListener l = currentListener;
                if (l != null) l.onDeviceFound(found);
            }
        };

        boolean started = btAdapter.startLeScan(leScanCallback);
        if (started) {
            scanning = true;
        } else {
            leScanCallback = null;
            currentListener = null;
            Log.e(TAG, "startScanOld: returned false");
            listener.onScanFailed(2);
        }
    }
}
