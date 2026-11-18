package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.lang.reflect.Method;
import java.util.UUID;

class BluetoothGattHelperImpl implements BluetoothGattHelper {
    private static final String TAG = "BleGattHelperImpl";

    static final UUID UUID_CTS = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    static final UUID UUID_CURRENT_TIME = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    static final UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_SERVICE_CHANGED = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final BluetoothManager btManager;
    private volatile boolean connected;

    private final Object gattLock = new Object();

    private volatile BluetoothGatt gatt;
    private volatile GattEventListener listener;

    BluetoothGattHelperImpl(Context context, BluetoothManager btManager) {
        this.context = context;
        this.btManager = btManager;
    }

    @Override
    public void setGattEventListener(GattEventListener l) {
        this.listener = l;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(BluetoothDevice device) {
        synchronized (gattLock) {
            if (gatt != null) {
                Log.w(TAG, "connect: called while GATT exists — closing previous");
                gatt.close();
                gatt = null;
                connected = false;
            }
            gatt = createGatt(device);
            Log.d(TAG, "connectGatt: issued for " + device.getAddress());
        }
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() throws BleException {
        BluetoothGatt g = gatt;
        if (g == null) throw new BleException("not connected");
        g.disconnect();
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void discoverServices() throws BleException {
        BluetoothGatt g = gatt;
        if (g == null) throw new BleException("not connected");
        if (!g.discoverServices()) {
            throw new BleException("discoverServices: returned false");
        }
    }

    @Override
    public void refreshGatt() {
        BluetoothGatt g = gatt;
        if (g == null) { Log.w(TAG, "refreshGatt: not connected"); return; }
        try {
            Method refresh = g.getClass().getMethod("refresh");
            refresh.invoke(g);
            Log.d(TAG, "refreshGatt: OK");
        } catch (Exception e) {
            Log.w(TAG, "refreshGatt failed: " + e);
        }
    }

    @Override
    public BluetoothGatt getGatt() {
        return gatt;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void close() {
        synchronized (gattLock) {
            if (gatt != null) {
                gatt.close();
                gatt = null;
            }
            connected = false;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private BluetoothGatt createGatt(BluetoothDevice device) {
        try {
            Method m = BluetoothDevice.class.getMethod("connectGatt",
                    Context.class, boolean.class, BluetoothGattCallback.class, int.class);
            m.setAccessible(true);
            BluetoothGatt g = (BluetoothGatt) m.invoke(device, context, false,
                    callback, 2);
            Log.d(TAG, "connectGatt via reflection (TRANSPORT_LE)");
            return g;
        } catch (Exception e) {
            Log.w(TAG, "connectGatt reflection failed ("
                    + e.getMessage() + "); falling back to 3-arg");
            return device.connectGatt(context, false, callback);
        }
    }


    private final BluetoothGattCallback callback = new BluetoothGattCallback() {

        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);

            GattEventListener l = listener;
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                connected = true;
                if (l != null) l.onGattConnected();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                connected = false;
                g.close();
                if (gatt == g) gatt = null;
                if (l != null) l.onGattDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            Log.d(TAG, "onServicesDiscovered status=" + status);
            GattEventListener l = listener;
            if (l == null) return;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                l.onServicesDiscovered();
            }
        }

        @Override
        public void onServiceChanged(BluetoothGatt g) {
            Log.d(TAG, "onServiceChanged");
            GattEventListener l = listener;
            if (l != null) l.onServiceChanged();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g,
                                            BluetoothGattCharacteristic c, byte[] value) {
            GattEventListener l = listener;
            if (l == null) return;

            if (UUID_SERVICE_CHANGED.equals(c.getUuid())) {
                l.onServiceChanged();
            } else {
                l.onCharacteristicChanged(c);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g,
                                            BluetoothGattCharacteristic c) {
            GattEventListener l = listener;
            if (l == null) return;

            if (UUID_SERVICE_CHANGED.equals(c.getUuid())) {
                l.onServiceChanged();
            } else {
                l.onCharacteristicChanged(c);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g,
                                          BluetoothGattCharacteristic c, int status) {
            GattEventListener l = listener;
            if (l != null) l.onCharacteristicWritten(c);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g,
                                      BluetoothGattDescriptor d, int status) {
        }
    };
}

