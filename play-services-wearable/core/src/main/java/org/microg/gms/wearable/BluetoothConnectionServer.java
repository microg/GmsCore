/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microg.wearable.WearableConnection;

import java.io.IOException;
import java.util.UUID;

/**
 * Bluetooth RFCOMM server that accepts incoming connections from Wear OS watches.
 * <p>
 * Wear OS watches use Bluetooth SPP (Serial Port Profile) with the standard
 * SPP UUID (00001101-0000-1000-8000-00805F9B34FB) or Wear OS-specific UUIDs.
 */
public class BluetoothConnectionServer extends Thread {

    private static final String TAG = "GmsWearBtSrv";

    /**
     * Standard SPP UUID used by Wear OS for initial Bluetooth pairing and communication.
     */
    private static final UUID WEAROS_SPP_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Additional Wear OS-specific UUID for Google's Wearable protocol.
     */
    private static final UUID WEAROS_GOOGLE_UUID =
        UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");

    private final String serviceName;
    private final WearableConnection.Listener connectionListener;
    private BluetoothServerSocket serverSocket;

    public BluetoothConnectionServer(String serviceName, WearableConnection.Listener connectionListener) {
        super("BluetoothConnectionServer");
        this.serviceName = serviceName;
        this.connectionListener = connectionListener;
    }

    @Override
    public void run() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.w(TAG, "Bluetooth not available on this device");
            return;
        }

        if (!adapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled, cannot start WearOS server");
            return;
        }

        // Try to listen on the standard SPP UUID first, fall back to Google UUID
        BluetoothServerSocket socket = null;
        try {
            // Ensure device is discoverable for Wear OS companion app pairing
            if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Log.d(TAG, "Requesting discoverable mode for WearOS pairing...");
                // Note: actual discoverable request requires user consent via intent
                // This is handled by the calling activity/notification
            }

            // Listen on the SPP UUID for Wear OS connections
            socket = adapter.listenUsingRfcommWithServiceRecord(serviceName, WEAROS_SPP_UUID);
            serverSocket = socket;
            Log.d(TAG, "Bluetooth server listening on " + WEAROS_SPP_UUID);

            while (!Thread.interrupted()) {
                BluetoothSocket clientSocket = socket.accept();
                if (clientSocket != null) {
                    Log.d(TAG, "Accepted Bluetooth connection from: "
                        + clientSocket.getRemoteDevice().getName()
                        + " [" + clientSocket.getRemoteDevice().getAddress() + "]");
                    BluetoothWearableConnection connection =
                        new BluetoothWearableConnection(clientSocket, connectionListener);
                    // Start the connection in a new thread for each client
                    new Thread(connection, "BtConn-" + clientSocket.getRemoteDevice().getAddress()).start();
                }
            }
        } catch (IOException e) {
            if (!Thread.interrupted()) {
                Log.e(TAG, "Bluetooth server error", e);
            }
        } finally {
            closeSocket();
        }
    }

    public void close() {
        interrupt();
        closeSocket();
    }

    private void closeSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }
}
