package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.proto.Connect;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class BleManager implements Closeable {
    private static final String TAG = "BleManager";

    private final Context context;
    private final WearableImpl wearable;
    private final BluetoothAdapter btAdapter;
    private final BluetoothManager btManager;

    private final Map<String, Entry> entries = new HashMap<>();

    public BleManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
        btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager != null ? btManager.getAdapter() : BluetoothAdapter.getDefaultAdapter();
    }



    public synchronized void enable(ConnectionConfiguration config) {
        if (config.address == null) {
            Log.w(TAG, "enable: address is null, ignoring");
            return;
        }

        Entry e = entries.get(config.address);
        if (e != null) {
            Log.d(TAG, "enable: updating existing manager for " + config.address);
            e.mgr.updateConfiguration(config);
            return;
        }

        Log.d(TAG, "enable: creating BleConnectionManager for " + config.address);
        e = createEntry(config);
        entries.put(config.address, e);
    }

    public synchronized void disable(ConnectionConfiguration config) {
        if (config.address == null) return;

        Entry e = entries.remove(config.address);
        if (e != null) {
            Log.d(TAG, "disable: stopping manager for " + config.address);
            stopEntry(e);
        }
    }

    @Override
    public synchronized void close() {
        Log.d(TAG, "close: shutting down all BLE managers");
        for (Entry e : entries.values()) {
            stopEntry(e);
        }
        entries.clear();
    }

    private Entry createEntry(ConnectionConfiguration config) {
        HandlerThread thread = new HandlerThread(
                "BleConnMgr-" + config.address);
        thread.start();

        BluetoothGattHelperImpl gattHelper =
                new BluetoothGattHelperImpl(context, btManager);

        BleScannerImpl scanner = new BleScannerImpl(btAdapter);
        BleServicesHandlerImpl svc  = new BleServicesHandlerImpl(gattHelper);

        BleConnectionManager mgr = new BleConnectionManager(
                context,
                btAdapter,
                scanner,
                gattHelper,
                svc,
                thread.getLooper(),
                config);

        return new Entry(mgr, thread);
    }

    private static void stopEntry(Entry e) {
        try {
            e.mgr.quit();
        } catch (Exception ex) {
            Log.w(TAG, "quit error: " + ex);
        }
        try {
            e.thread.quitSafely();
        } catch (Exception ex) {
            Log.w(TAG, "thread quit error: " + ex);
        }
    }

    private static final class Entry {
        final BleConnectionManager mgr;
        final HandlerThread thread;

        Entry(BleConnectionManager mgr, HandlerThread thread) {
            this.mgr = mgr;
            this.thread = thread;
        }
    }

}
