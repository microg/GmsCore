package org.microg.gms.wearable.transport;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class Multiplexer {
    private static final String TAG = "WearMultiplexer";
    
    private final BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;
    private boolean isRunning;

    public Multiplexer(BluetoothSocket socket) {
        this.socket = socket;
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get streams", e);
        }
    }

    public void start() {
        isRunning = true;
        new Thread(this::readLoop).start();
    }

    private void readLoop() {
        byte[] buffer = new byte[4096];
        while (isRunning && socket.isConnected()) {
            try {
                int read = in.read(buffer);
                if (read > 0) {
                    // Route packets to WearableImpl
                    Log.d(TAG, "Received " + read + " bytes from Wear device");
                }
            } catch (Exception e) {
                Log.e(TAG, "Read failed", e);
                isRunning = false;
            }
        }
    }

    public void sendPacket(byte[] data) {
        if (!isRunning || out == null) return;
        try {
            out.write(data);
            out.flush();
        } catch (Exception e) {
            Log.e(TAG, "Write failed", e);
        }
    }

    public void stop() {
        isRunning = false;
    }
}
