package org.microg.gms.wearable.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnMgr";
    private static final UUID WEAR_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP for mock

    private BluetoothAdapter adapter;
    private BluetoothSocket currentSocket;
    private ConnectionListener listener;

    public interface ConnectionListener {
        void onConnected(BluetoothSocket socket);
        void onDisconnected();
    }

    public BluetoothConnectionManager(ConnectionListener listener) {
        this.listener = listener;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connectToDevice(String macAddress) {
        if (adapter == null || !adapter.isEnabled()) return;
        
        BluetoothDevice device = adapter.getRemoteDevice(macAddress);
        new Thread(() -> {
            try {
                currentSocket = device.createRfcommSocketToServiceRecord(WEAR_UUID);
                currentSocket.connect();
                Log.d(TAG, "Connected to Wear device: " + macAddress);
                if (listener != null) listener.onConnected(currentSocket);
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect", e);
                close();
            }
        }).start();
    }

    public void close() {
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {}
            currentSocket = null;
        }
        if (listener != null) listener.onDisconnected();
    }
}
