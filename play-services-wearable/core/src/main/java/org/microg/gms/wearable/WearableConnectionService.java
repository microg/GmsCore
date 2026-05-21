package org.microg.gms.wearable;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.microg.gms.wearable.transport.BluetoothConnectionManager;
import org.microg.gms.wearable.transport.Multiplexer;

public class WearableConnectionService extends Service implements BluetoothConnectionManager.ConnectionListener {
    private static final String TAG = "WearConnService";
    
    private BluetoothConnectionManager btManager;
    private Multiplexer multiplexer;
    private WearableImpl wearable;

    @Override
    public void onCreate() {
        super.onCreate();
        btManager = new BluetoothConnectionManager(this);
        // Initialize wearable implementation (stubbed)
        Log.d(TAG, "WearableConnectionService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("macAddress")) {
            String mac = intent.getStringExtra("macAddress");
            btManager.connectToDevice(mac);
        }
        return START_STICKY;
    }

    @Override
    public void onConnected(BluetoothSocket socket) {
        multiplexer = new Multiplexer(socket);
        multiplexer.start();
        // Here we would bind the multiplexer to WearableImpl to forward data map syncs
    }

    @Override
    public void onDisconnected() {
        if (multiplexer != null) {
            multiplexer.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        btManager.close();
        if (multiplexer != null) multiplexer.stop();
    }
}
