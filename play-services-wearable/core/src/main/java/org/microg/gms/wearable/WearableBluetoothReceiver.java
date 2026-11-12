/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.Set;

/**
 * BroadcastReceiver listening for Bluetooth state changes, ACL device connections,
 * and bonding events to dynamically register and auto-connect WearOS smartwatches.
 */
public class WearableBluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "GmsWearBtReceiver";
    private final WearableImpl wearable;

    public WearableBluetoothReceiver(WearableImpl wearable) {
        this.wearable = wearable;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth turned on, discovering and connecting paired WearOS devices");
                    discoverAndConnectBondedWearables();
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned off, stopping Bluetooth Wearable connections");
                    wearable.stopBluetooth();
                }
                break;

            case BluetoothDevice.ACTION_ACL_CONNECTED:
                BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (connectedDevice != null && isWearableDevice(connectedDevice)) {
                    Log.i(TAG, "Wearable Bluetooth device connected to system: " + connectedDevice.getName() + " [" + connectedDevice.getAddress() + "]");
                    ensureConfigurationAndConnect(connectedDevice);
                }
                break;

            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bondState == BluetoothDevice.BOND_BONDED && bondedDevice != null && isWearableDevice(bondedDevice)) {
                    Log.i(TAG, "New WearOS device bonded: " + bondedDevice.getName() + " [" + bondedDevice.getAddress() + "]");
                    ensureConfigurationAndConnect(bondedDevice);
                }
                break;
        }
    }

    @SuppressLint("MissingPermission")
    public void discoverAndConnectBondedWearables() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) return;

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isWearableDevice(device)) {
                    Log.d(TAG, "Found bonded Wearable device: " + device.getName() + " [" + device.getAddress() + "]");
                    ensureConfigurationAndConnect(device);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isWearableDevice(BluetoothDevice device) {
        if (device == null) return false;
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null) {
            int majorClass = bluetoothClass.getMajorDeviceClass();
            // 0x0700 = BluetoothClass.Device.Major.WEARABLE
            if (majorClass == BluetoothClass.Device.Major.WEARABLE) {
                return true;
            }
        }

        // Also check if existing connection configuration already exists for this MAC
        for (ConnectionConfiguration config : wearable.getConfigurations()) {
            if (device.getAddress().equalsIgnoreCase(config.address)) {
                return true;
            }
        }

        return false;
    }

    @SuppressLint("MissingPermission")
    private void ensureConfigurationAndConnect(BluetoothDevice device) {
        String address = device.getAddress();
        String name = device.getName() != null ? device.getName() : "WearOS Watch";

        // Check if configuration already exists
        ConnectionConfiguration existing = null;
        for (ConnectionConfiguration config : wearable.getConfigurations()) {
            if (address.equalsIgnoreCase(config.address)) {
                existing = config;
                break;
            }
        }

        if (existing == null) {
            // Create configuration for new smartwatch: type 0 (Bluetooth), role 1 (Client), enabled true
            ConnectionConfiguration newConfig = new ConnectionConfiguration(name, address, 0, 1, true);
            wearable.createConnection(newConfig);
        } else {
            wearable.enableConnection(existing.name);
        }
    }
}
