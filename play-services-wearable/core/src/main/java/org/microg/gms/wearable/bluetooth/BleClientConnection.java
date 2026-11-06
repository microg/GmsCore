package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.ConnectHandshake;
import org.microg.gms.wearable.TransportConnectionHandler;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.WearableImpl;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public class BleClientConnection implements Closeable {
    private static final String TAG = "BleClientConnection";
    static final UUID UUID_PSM_SERVICE = UUID.fromString("C9C92447-8F77-4C63-9C79-25E71B4CA9B3");
    static final UUID UUID_PSM_CHAR = UUID.fromString("ABDD3056-28FA-441D-A470-55A75A52553A");
    static final UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final long RECONNECT_DELAY_MS = 5000;
    private static final long REDISCOVER_DELAY_MS = 1000;
    private static final long PSM_READ_TIMEOUT_MS = 35000;
    private static final long L2CAP_RETRY_DELAY_MS = 5000;

    private final Context context;
    private final WearableImpl wearable;
    private final ConnectionConfiguration config;
    private final BluetoothDevice device;

    private final HandlerThread handlerThread;
    private final Handler handler;
    private BluetoothGatt gatt;
    private L2capClientThread l2capThread;
    private boolean closed = false;

    private final Runnable connectRunnable = this::doConnectGatt;
    private final Runnable discoverRunnable = this::doDiscoverServices;
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private final Runnable psmTimeoutRunnable = () -> {
        Log.w(TAG, "PSM characteristic read timed out, disconnecting");
        if (gatt != null) gatt.disconnect();
    };

    public BleClientConnection(Context context, WearableImpl wearable,
                               BluetoothAdapter adapter, ConnectionConfiguration config) {
        this.context = context;
        this.wearable = wearable;
        this.config = config;
        this.device = adapter.getRemoteDevice(config.address);
        this.handlerThread = new HandlerThread("BleClient-" + config.address);
        this.handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
    }

    public void start() {
        Log.d(TAG, "Starting for " + config.address);
        handler.postDelayed(connectRunnable, 0);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void close() {
        Log.d(TAG, "Closing for " + config.address);
        handler.post(() -> {
            closed = true;
            handler.removeCallbacksAndMessages(null);
            closeL2cap();
            if (gatt != null) {
                gatt.close();
                gatt = null;
            }
            handlerThread.quitSafely();
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void doConnectGatt() {
        if (closed) return;
        if (gatt == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gatt = device.connectGatt(context, true, callback, BluetoothDevice.TRANSPORT_LE);
            } else {
                gatt = device.connectGatt(context, true, callback);
            }
        }
        if (gatt == null) {
            Log.w(TAG, "connectGatt returned null, retrying");
            handler.postDelayed(connectRunnable, RECONNECT_DELAY_MS);
            return;
        }
        Log.d(TAG, "gatt.connect() -> " + gatt.connect());
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void doDiscoverServices() {
        if (closed || gatt == null) return;
        Log.d(TAG, "discoverServices() -> " + gatt.discoverServices());
    }

    private void onGattConnected() {
        handler.removeCallbacks(discoverRunnable);
        handler.post(discoverRunnable);
    }

    private void onGattDisconnected() {
        handler.removeCallbacks(psmTimeoutRunnable);
        handler.removeCallbacks(discoverRunnable);
        handler.removeCallbacks(connectRunnable);
        handler.postDelayed(connectRunnable, RECONNECT_DELAY_MS);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void onServicesDiscovered() {
        BluetoothGattService service = gatt.getService(UUID_PSM_SERVICE);
        if (service == null) {
            Log.w(TAG, "PSM service not found, retrying discovery in 1s");
            handler.postDelayed(discoverRunnable, REDISCOVER_DELAY_MS);
            return;
        }
        BluetoothGattCharacteristic psmChar = service.getCharacteristic(UUID_PSM_CHAR);
        if (psmChar == null) {
            Log.w(TAG, "PSM characteristic not found, retrying discovery in 1s");
            handler.postDelayed(discoverRunnable, REDISCOVER_DELAY_MS);
            return;
        }

        enableNotifications(psmChar);

        Log.d(TAG, "Reading PSM characteristic");
        gatt.readCharacteristic(psmChar);
        handler.removeCallbacks(psmTimeoutRunnable);
        handler.postDelayed(psmTimeoutRunnable, PSM_READ_TIMEOUT_MS);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void enableNotifications(BluetoothGattCharacteristic c) {
        try {
            gatt.setCharacteristicNotification(c, true);
            BluetoothGattDescriptor cccd = c.getDescriptor(UUID_CCCD);
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(cccd);
            }
        } catch (Exception e) {
            Log.w(TAG, "enableNotifications failed: " + e);
        }
    }

    private void onPsmValue(byte[] value) {
        if (value == null || value.length < 2) {
            Log.w(TAG, "Bad PSM value length: " + (value == null ? -1 : value.length));
            return;
        }
        int psm = (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
        Log.d(TAG, "PSM value: " + psm);

        if (l2capThread != null && l2capThread.psm == psm) return;

        closeL2cap();
        if (psm == 0) {
            Log.d(TAG, "PSM 0, server not listening");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "L2CAP CoC requires API 29+");
            return;
        }
        l2capThread = new L2capClientThread(psm);
        l2capThread.start();
    }

    private void closeL2cap() {
        if (l2capThread != null) {
            l2capThread.close();
            l2capThread = null;
        }
    }

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status=" + status + " state=" + newState);
            handler.post(() -> {
                if (closed || g != gatt) return;
                if (newState == BluetoothGatt.STATE_CONNECTED) onGattConnected();
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) onGattDisconnected();
            });
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            Log.d(TAG, "onServicesDiscovered status=" + status);
            handler.post(() -> {
                if (closed || g != gatt) return;
                BleClientConnection.this.onServicesDiscovered();
            });
        }

        @Override
        public void onServiceChanged(BluetoothGatt g) {
            handler.post(() -> {
                if (closed || g != gatt) return;
                handler.removeCallbacks(discoverRunnable);
                handler.post(discoverRunnable);
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            Log.d(TAG, "onCharacteristicRead status=" + status);
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            final byte[] value = c.getValue();
            handler.post(() -> {
                if (closed || g != gatt) return;
                handler.removeCallbacks(psmTimeoutRunnable);
                if (UUID_PSM_CHAR.equals(c.getUuid())) onPsmValue(value);
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            final byte[] value = c.getValue();
            handler.post(() -> {
                if (closed || g != gatt) return;
                if (UUID_PSM_CHAR.equals(c.getUuid())) onPsmValue(value);
                else Log.d(TAG, "Unhandled notification for " + c.getUuid());
            });
        }
    };

    private class L2capClientThread extends Thread implements Closeable {
        final int psm;
        private volatile boolean stop = false;
        private volatile BluetoothSocket socket;

        L2capClientThread(int psm) {
            super("BleL2cap-" + config.address + "-" + psm);
            this.psm = psm;
        }

        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void run() {
            while (!stop) {
                try {
                    runOnce();
                } catch (IOException e) {
                    Log.w(TAG, "L2CAP connection failed: " + e.getMessage());
                } catch (RuntimeException e) {
                    Log.e(TAG, "Unexpected error in L2CAP thread", e);
                } finally {
                    closeSocket();
                }
                if (stop) break;
                try {
                    Thread.sleep(L2CAP_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.d(TAG, "L2CAP thread finished for psm " + psm);
        }

        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        private void runOnce() throws IOException {
            Log.d(TAG, "Connecting L2CAP, psm=" + psm);
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isDiscovering()) adapter.cancelDiscovery();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new IOException("L2CAP CoC requires API 29+");
            }
            BluetoothSocket s;
            try {
                s = device.createL2capChannel(psm);
            } catch (RuntimeException e) {
                throw new IOException("createL2capChannel failed", e);
            }
            socket = s;
            s.connect();
            Log.d(TAG, "L2CAP socket connected");

            ConnectHandshake.LocalIdentity local = new ConnectHandshake.LocalIdentity(
                    wearable.getLocalNodeId(),
                    Build.MODEL,
                    getAndroidId(),
                    wearable.getClockworkNodePreferences().getNetworkId(),
                    null,
                    config.migrating,
                    config.migrating ? wearable.getClockworkNodePreferences().getPeerNodeId() : null
            );

            BluetoothWearableConnection conn =
                    new BluetoothWearableConnection(s, local, WearableConnection.NOOP);
            if (!conn.handshake()) {
                throw new IOException("L2CAP handshake failed");
            }
            new TransportConnectionHandler(wearable, config).handle(conn);
            Log.d(TAG, "L2CAP connection ended");
        }

        private void closeSocket() {
            BluetoothSocket s = socket;
            socket = null;
            if (s != null) {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public void close() {
            stop = true;
            closeSocket();
            interrupt();
        }
    }

    private long getAndroidId() {
        try {
            String s = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return s != null ? Long.parseUnsignedLong(s, 16) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}