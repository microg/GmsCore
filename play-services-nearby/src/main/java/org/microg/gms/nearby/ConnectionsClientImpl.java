/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BLE-based implementation of the {@link ConnectionsClient} discovery and advertising surface.
 *
 * <p>Service IDs are mapped onto a 128-bit BLE service UUID (the same scheme used by Google's
 * implementation: the service id is hashed into a stable UUID so that only endpoints of the same
 * service see each other). The local endpoint name is carried in the scan response of the
 * advertisement and read back from the scan record on the discovering side.
 */
public class ConnectionsClientImpl implements ConnectionsClient {
    private static final String TAG = "ConnectionsClientImpl";

    private final Context context;
    private final Map<String, String> endpointNames = new HashMap<>();

    private String advertisingServiceId;
    private String localEndpointName;
    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private AdvertiseCallback advertiseCallback;

    private String discoveryServiceId;
    private EndpointDiscoveryCallback endpointDiscoveryCallback;
    private ScanCallback scanCallback;

    public ConnectionsClientImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Maps a service id onto a stable 128-bit UUID, as required by the BLE advertisement format. */
    public static ParcelUuid serviceIdToUuid(@NonNull String serviceId) {
        return new ParcelUuid(UUID.nameUUIDFromBytes(("microg-nearby:" + serviceId).getBytes(StandardCharsets.UTF_8)));
    }

    private BluetoothAdapter getAdapter() {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager == null ? null : manager.getAdapter();
    }

    @NonNull
    @Override
    public Task<Void> startAdvertising(@NonNull String serviceId, @NonNull String localEndpointName,
                                       @NonNull ConnectionLifecycleCallback callback,
                                       @NonNull AdvertisingOptions options) {
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return Tasks.forException(new IllegalStateException("Bluetooth is not enabled"));
        }
        BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            return Tasks.forException(new IllegalStateException("BLE advertising is not supported"));
        }
        stopAdvertising();

        this.advertisingServiceId = serviceId;
        this.localEndpointName = localEndpointName;
        this.connectionLifecycleCallback = callback;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(serviceIdToUuid(serviceId))
                .build();
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addServiceData(serviceIdToUuid(serviceId), localEndpointName.getBytes(StandardCharsets.UTF_8))
                .build();

        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "Advertising " + serviceId + " as " + localEndpointName);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.w(TAG, "Advertising failed: " + errorCode);
            }
        };
        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    public Task<Void> startDiscovery(@NonNull String serviceId,
                                     @NonNull EndpointDiscoveryCallback callback,
                                     @NonNull DiscoveryOptions options) {
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return Tasks.forException(new IllegalStateException("Bluetooth is not enabled"));
        }
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            return Tasks.forException(new IllegalStateException("BLE scanning is not supported"));
        }
        stopDiscovery();

        this.discoveryServiceId = serviceId;
        this.endpointDiscoveryCallback = callback;

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(serviceIdToUuid(serviceId))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String endpointId = result.getDevice().getAddress();
                String endpointName = readEndpointName(result.getScanRecord(), serviceId);
                if (endpointName == null) return;
                if (endpointNames.containsKey(endpointId)) return;
                endpointNames.put(endpointId, endpointName);
                callback.onEndpointFound(endpointId, new DiscoveredEndpointInfo(endpointName, serviceId));
            }

            @Override
            public void onBatchScanResults(java.util.List<ScanResult> results) {
                for (ScanResult result : results) onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.w(TAG, "Scan failed: " + errorCode);
            }
        };
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        return Tasks.forResult(null);
    }

    private static String readEndpointName(ScanRecord record, String serviceId) {
        if (record == null) return null;
        byte[] data = record.getServiceData(serviceIdToUuid(serviceId));
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }

    @NonNull
    @Override
    public Task<Void> stopAdvertising() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null && advertiseCallback != null) {
            BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
            if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
        }
        advertiseCallback = null;
        advertisingServiceId = null;
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    public Task<Void> stopDiscovery() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null && scanCallback != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            if (scanner != null) scanner.stopScan(scanCallback);
        }
        scanCallback = null;
        discoveryServiceId = null;
        endpointNames.clear();
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    public Task<Void> stopAllEndpoints() {
        stopAdvertising();
        stopDiscovery();
        return Tasks.forResult(null);
    }
}
