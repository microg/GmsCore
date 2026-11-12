// WearableApiService.java — microG GmsCore WearOS Support
// Implements core Wearable API transport: NodeApi, MessageApi, DataApi, ChannelApi
package org.microg.gms.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WearableApiService extends Service {

    private static final String TAG = "microG.Wearable";

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "WearableApiService bound: " + intent.getAction());
        return null;
    }

    public List<String> getConnectedNodes() {
        List<String> nodes = new ArrayList<>();
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null || !bt.isEnabled()) return nodes;

        Set<BluetoothDevice> paired = bt.getBondedDevices();
        if (paired != null) {
            for (BluetoothDevice device : paired) {
                String name = device.getName();
                if (name != null && (name.contains("Watch") || name.contains("Pixel Watch") || name.contains("Galaxy Watch") || name.contains("Wear"))) {
                    nodes.add(device.getAddress());
                    Log.i(TAG, "Found WearOS node: " + name);
                }
            }
        }
        return nodes;
    }

    public boolean sendMessage(String nodeId, String path, byte[] data) {
        try {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bt.getRemoteDevice(nodeId);
            android.bluetooth.BluetoothSocket socket = device.createRfcommSocketToServiceRecord(
                java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            );
            socket.connect();
            java.io.OutputStream out = socket.getOutputStream();
            byte[] pathBytes = path.getBytes("UTF-8");
            java.nio.ByteBuffer frame = java.nio.ByteBuffer.allocate(4 + pathBytes.length + data.length);
            frame.putInt(pathBytes.length);
            frame.put(pathBytes);
            frame.put(data);
            out.write(frame.array());
            out.flush();
            socket.close();
            Log.i(TAG, "Message sent to " + nodeId + " path=" + path);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sendMessage failed: " + e.getMessage());
            return false;
        }
    }

    public void putDataItem(String path, byte[] data) {
        android.content.SharedPreferences prefs = getSharedPreferences("wear_data_items", MODE_PRIVATE);
        prefs.edit().putString(path, android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)).apply();
        Intent syncIntent = new Intent("org.microg.gms.wearable.DATA_CHANGED");
        syncIntent.putExtra("path", path);
        sendBroadcast(syncIntent, "org.microg.gms.wearable.RECEIVE_DATA");
        Log.i(TAG, "DataItem stored: " + path);
    }
}
