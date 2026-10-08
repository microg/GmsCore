package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class BleConnectionManager extends BleStateMachine implements BleConnectionManagerInterface, GattEventListener {
    private static final String TAG = "BleConnectionManager";

    public static final int MSG_INIT = 1;
    public static final int MSG_BT_ADAPTER_STATE_CHANGED = 2;
    public static final int MSG_CONNECTION_CONFIG_UPDATE = 3;
    public static final int MSG_START_SCAN = 4;
    public static final int MSG_START_FORCED_SCAN = 5;
    public static final int MSG_SCAN_FAILED = 6;
    public static final int MSG_STOP_SCAN = 7;
    public static final int MSG_RESCHEDULE_SCAN = 8;
    public static final int MSG_RECONNECT_REQUESTED = 9;
    public static final int MSG_SERVICE_DISCOVERY_COMPLETE = 10;
    public static final int MSG_HANDLE_NOTIFICATION = 11;
    public static final int MSG_DECOMMISSION_WATCH = 12;
    public static final int MSG_RECONNECT_CHARACTERISTIC_CHANGED = 13;
    public static final int MSG_ERROR = 14;
    public static final int MSG_CONNECTION_THREAD_DONE = 15;
    public static final int MSG_GATT_CONNECTION_CLOSED = 16;
    public static final int MSG_READY_TO_SETUP_ANCS = 17;
    public static final int MSG_UPDATE_TIME = 18;
    public static final int MSG_ON_SERVICE_CHANGED = 19;
    public static final int MSG_RESET_CHARACTERICTIC_CHANGED = 20;
    public static final int MSG_RESET_CONNECTION = 21;
    private static final int MSG_GATT_CONNECTED = 22;

    private static final long SCAN_TIMEOUT_MS = 30000L;

    public final Context context;
    public final BluetoothAdapter btAdapter;

    public final BleServicesHandler gattHelper;
    final BluetoothGattHelper bleConnHelper;
    private final BleScanner bleScanner;

    public volatile AtomicReference<ConnectionConfiguration> config;

    final AtomicBoolean isReceiverRegistered = new AtomicBoolean(false);
    public final AtomicInteger scanAttemptCount = new AtomicInteger();
    public final AtomicInteger totalExceptionCount = new AtomicInteger();
    public final AtomicInteger disconnectExceptionCount = new AtomicInteger();

    final AtomicLong timeServiceNotFoundCounter = new AtomicLong();
    final AtomicLong invalidGattHandleCounter = new AtomicLong();
    final AtomicLong readNotPermittedCounter = new AtomicLong();
    final AtomicLong writeNotPermittedCounter = new AtomicLong();
    final AtomicLong invalidDecommissionCounter = new AtomicLong();
    final AtomicLong serviceNotFoundCounter = new AtomicLong();
    final AtomicLong timeCharInvalidCounter = new AtomicLong();
    final AtomicLong timezoneOffsetInvalidCounter = new AtomicLong();
    final AtomicLong missingClockworkCharCounter = new AtomicLong();

    private final AtomicLong errorSampler = new AtomicLong();
    private final AtomicBoolean isBtlePrioEnabled = new AtomicBoolean(true);

    final BroadcastReceiver btStateReceiver;
    public final BroadcastReceiver screenOnReceiver;

    final BleState idleState;
    public final BleState discoveredState;
    public final BleState connectedState;
    public final BleState disconnectingState;
    public final BleState errorDisconnectedState;
    private final BleState disconnectedState;
    private final BleState scanningState;
    private final BleState connectingState;

    public BleConnectionManager(
            Context context,
            BluetoothAdapter bluetoothAdapter,
            BleScanner bleScanner,
            BluetoothGattHelper bleConnHelper,
            BleServicesHandler gattHelper,
            Looper looper,
            ConnectionConfiguration connectionConfiguration) {
        super("BleConnectionManager", looper);

        this.context = context;
        this.btAdapter = bluetoothAdapter;
        this.bleScanner = bleScanner;
        this.bleConnHelper = bleConnHelper;
        this.gattHelper = gattHelper;
        this.config = new AtomicReference<>();

        bleConnHelper.setGattEventListener(this);

        this.btStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                ConnectionConfiguration cfg = BleConnectionManager.this.config.get();
                if (cfg == null || !cfg.enabled) return;

                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_OFF
                            || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        sendBtAdapterStateMsg(state);
                    }
                } else if ("android.gms.wearable.altReconnect".equals(action)) {
                    sendMessage(MSG_RECONNECT_REQUESTED);
                }
            }
        };

        this.screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                removeMessages(MSG_START_FORCED_SCAN);
                sendMessage(MSG_START_FORCED_SCAN);
                BleConnectionManager.this.context
                        .unregisterReceiver(BleConnectionManager.this.screenOnReceiver);
            }
        };

        BleState idleStateLocal = new IdleState(this);
        BleState disconnectedStateLocal = new ServiceOnState(this);
        BleState discoveredStateLocal = new DisconnectedState(this);
        BleState scanningStateLocal = new ScanningState(this);
        BleState connectingStateLocal = new ConnectingState(this);
        BleState connectedStateLocal = new ConnectedState(this);
        BleState disconnectingStateLocal = new DisconnectingState(this);
        BleState errorDisconnectedStateLocal = new ServiceOffState(this);

        this.idleState = idleStateLocal;
        this.disconnectedState = disconnectedStateLocal;
        this.discoveredState = discoveredStateLocal;
        this.scanningState = scanningStateLocal;
        this.connectingState = connectingStateLocal;
        this.connectedState = connectedStateLocal;
        this.disconnectingState = disconnectingStateLocal;
        this.errorDisconnectedState = errorDisconnectedStateLocal;

        this.isBtlePrioEnabled.set(
                connectionConfiguration == null || connectionConfiguration.btlePriority);

        this.config.set(connectionConfiguration);

        addState(disconnectedStateLocal);
        addState(discoveredStateLocal);
        addState(scanningStateLocal);
        addState(connectingStateLocal);
        addState(connectedStateLocal);
        addState(disconnectingStateLocal);
        addState(errorDisconnectedStateLocal);

        addTransition(disconnectedStateLocal, discoveredStateLocal);
        addTransition(discoveredStateLocal, scanningStateLocal);
        addTransition(scanningStateLocal, connectingStateLocal);
        addTransition(scanningStateLocal, disconnectingStateLocal);
        addTransition(connectingStateLocal, connectedStateLocal);
        addTransition(connectingStateLocal, disconnectingStateLocal);
        addTransition(connectedStateLocal, disconnectingStateLocal);
        addTransition(disconnectingStateLocal, discoveredStateLocal);
        addTransition(discoveredStateLocal, disconnectedStateLocal);
        addTransition(disconnectedStateLocal, errorDisconnectedStateLocal);
        addTransition(errorDisconnectedStateLocal, disconnectedStateLocal);

        setErrorState(errorDisconnectedStateLocal);
        start();
    }

    @Override
    protected String getMessageName(int what) {
        switch (what) {
            case MSG_INIT:                             return "MSG_INIT";
            case MSG_BT_ADAPTER_STATE_CHANGED:         return "MSG_BT_ADAPTER_STATE_CHANGED";
            case MSG_CONNECTION_CONFIG_UPDATE:         return "MSG_CONNECTION_CONFIG_UPDATE";
            case MSG_START_SCAN:                       return "MSG_START_SCAN";
            case MSG_START_FORCED_SCAN:                return "MSG_START_FORCED_SCAN";
            case MSG_SCAN_FAILED:                      return "MSG_SCAN_FAILED";
            case MSG_STOP_SCAN:                        return "MSG_STOP_SCAN";
            case MSG_RESCHEDULE_SCAN:                  return "MSG_RESCHEDULE_SCAN";
            case MSG_RECONNECT_REQUESTED:              return "MSG_RECONNECT_REQUESTED";
            case MSG_SERVICE_DISCOVERY_COMPLETE:       return "MSG_SERVICE_DISCOVERY_COMPLETE";
            case MSG_HANDLE_NOTIFICATION:              return "MSG_HANDLE_NOTIFICATION";
            case MSG_DECOMMISSION_WATCH:               return "MSG_DECOMMISSION_WATCH";
            case MSG_RECONNECT_CHARACTERISTIC_CHANGED: return "MSG_RECONNECT_CHARACTERISTIC_CHANGED";
            case MSG_ERROR:                            return "MSG_ERROR";
            case MSG_CONNECTION_THREAD_DONE:           return "MSG_CONNECTION_THREAD_DONE";
            case MSG_GATT_CONNECTION_CLOSED:           return "MSG_GATT_CONNECTION_CLOSED";
            case MSG_READY_TO_SETUP_ANCS:              return "MSG_READY_TO_SETUP_ANCS";
            case MSG_UPDATE_TIME:                      return "MSG_UPDATE_TIME";
            case MSG_ON_SERVICE_CHANGED:               return "MSG_ON_SERVICE_CHANGED";
            case MSG_RESET_CHARACTERICTIC_CHANGED:     return "MSG_RESET_CHARACTERICTIC_CHANGED";
            case MSG_RESET_CONNECTION:                 return "MSG_RESET_CONNECTION";
            case MSG_GATT_CONNECTED:                   return "MSG_GATT_CONNECTED";
            default:                                   return "UNKNOWN";
        }
    }

    @Override
    public void updateConfiguration(ConnectionConfiguration connectionConfiguration) {
        Log.d(TAG, "updateConfiguration: config is "
                + (connectionConfiguration.enabled ? "enabled" : "disabled"));
        this.config.set(connectionConfiguration);
        sendMessage(MSG_CONNECTION_CONFIG_UPDATE);
    }

    @Override
    public void quit() {

    }

    @Override
    public void quitSafely() {

    }

    @Override
    public void onGattConnected() {
        Log.d(TAG, "onGattConnected");
        sendMessage(MSG_GATT_CONNECTED);
    }

    @Override
    public void onServiceChanged() {
        Log.d(TAG, "onServiceChanged");
        sendMessage(MSG_ON_SERVICE_CHANGED);
    }

    @Override
    public void onCharacteristicWritten(BluetoothGattCharacteristic characteristic) {
        characteristic.getUuid();
    }

    @Override
    public void onServicesDiscovered() {
        Log.d(TAG, "onServicesDiscovered");
        sendMessage(MSG_SERVICE_DISCOVERY_COMPLETE);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {}

    @Override
    protected boolean shouldLogMessage(Message message) {
        return message.what != MSG_HANDLE_NOTIFICATION;
    }

    protected void onQuitting() {
        Log.d(TAG, "onQuitting");
        stopScan();
        doDisconnectGatt();
        doDisconnectGatt(); // twice in original GMS
        gattHelper.cleanup();
        if (isReceiverRegistered.compareAndSet(true, false)) {
            context.unregisterReceiver(btStateReceiver);
        }
    }

    void syncReceiverRegistration() {
        ConnectionConfiguration cfg = config.get();
        if (cfg != null && !cfg.enabled) {
            if (isReceiverRegistered.compareAndSet(true, false)) {
                context.unregisterReceiver(btStateReceiver);
            }
        } else if (isReceiverRegistered.compareAndSet(false, true)) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction("android.gms.wearable.altReconnect");
            context.registerReceiver(btStateReceiver, filter);
        }
    }

    boolean handleUnhandledMessage(Message message) {
        int what = message.what;
        if (what == MSG_RECONNECT_REQUESTED || what == MSG_CONNECTION_THREAD_DONE) {
            return true;
        }
        Log.d(TAG, "[" + currentState().getName() + "] Unhandled message: " + message.what);
        return false;
    }

    void stopScan() {
        if (!bleScanner.isScanning()) {
            Log.d(TAG, "Not scanning, returning.");
            return;
        }
        bleScanner.stopScan();
        removeMessages(MSG_START_SCAN);
        removeMessages(MSG_STOP_SCAN);
        removeMessages(MSG_START_FORCED_SCAN);
        Log.d(TAG, "Stopped scan.");
    }

    private void doDisconnectGatt() {
        try {
            try {
                if (bleConnHelper.isConnected()) {
                    Log.d(TAG, "Disconnecting");
                    bleConnHelper.disconnect();
                } else {
                    Log.d(TAG, "Not disconnecting; already disconnected");
                }
            } catch (BleException e) {
                Log.w(TAG, "Bluetooth exception caught while disconnecting");
            }
        } finally {
            sendMessageDelayed(MSG_GATT_CONNECTION_CLOSED, 0);
        }
    }

    void handleBleException(BleException e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Log.w(TAG, "Got exception: " + sw, e);
        totalExceptionCount.incrementAndGet();

        int code = e.statusCode;

        if (code == BleException.CODE_MISSING_CLOCKWORK_CHARS) {
            if (Build.VERSION.SDK_INT >= 28) {
                Log.d(TAG, "Clockwork service characteristics are missing.");
                return;
            } else {
                bleConnHelper.refreshGatt();
                missingClockworkCharCounter.incrementAndGet();
                return;
            }
        }

        disconnectExceptionCount.incrementAndGet();

        if (code == BleException.CODE_GATT_INVALID_HANDLE
                || code == BleException.CODE_GATT_READ_NOT_PERMITTED
                || code == BleException.CODE_GATT_WRITE_NOT_PERMITTED
                || code == BleException.CODE_INVALID_DECOMMISSION
                || code == BleException.CODE_TIMEZONE_OFFSET_INVALID
                || code == BleException.CODE_TIME_CHAR_INVALID) {
            bleConnHelper.refreshGatt();
            incrementExceptionCounter(code);
            return;
        }

        if (code == BleException.CODE_TIME_SERVICE_NOT_FOUND
                || code == BleException.CODE_SERVICE_NOT_FOUND) {
            if (Build.VERSION.SDK_INT >= 28) {
                Log.d(TAG, "Service is missing when OnServiceChanged enabled.");
            } else {
                bleConnHelper.refreshGatt();
                incrementExceptionCounter(code);
            }
            return;
        }

        if (e instanceof BleTimeoutException) {
            errorSampler.incrementAndGet();
            return;
        }

        if (code != BleException.CODE_UNKNOWN) {
            errorSampler.incrementAndGet();
        } else {
            Log.w(TAG, "Unable to log unhandled exception: " + e);
        }
    }


    private void incrementExceptionCounter(int code) {
        switch (code) {
            case BleException.CODE_GATT_INVALID_HANDLE:
                invalidGattHandleCounter.incrementAndGet(); break;
            case BleException.CODE_GATT_READ_NOT_PERMITTED:
                readNotPermittedCounter.incrementAndGet(); break;
            case BleException.CODE_GATT_WRITE_NOT_PERMITTED:
                writeNotPermittedCounter.incrementAndGet(); break;
            case BleException.CODE_TIME_SERVICE_NOT_FOUND:
                timeServiceNotFoundCounter.incrementAndGet(); break;
            case BleException.CODE_MISSING_CLOCKWORK_CHARS:
                missingClockworkCharCounter.incrementAndGet(); break;
            case BleException.CODE_INVALID_DECOMMISSION:
                invalidDecommissionCounter.incrementAndGet(); break;
            case BleException.CODE_SERVICE_NOT_FOUND:
                serviceNotFoundCounter.incrementAndGet(); break;
            case BleException.CODE_TIME_CHAR_INVALID:
                timeCharInvalidCounter.incrementAndGet(); break;
            case BleException.CODE_TIMEZONE_OFFSET_INVALID:
                timezoneOffsetInvalidCounter.incrementAndGet(); break;
            default:
                Log.w(TAG, "Failed to log exception with status code: " + code);
                break;
        }
    }

    static final class IdleState extends BleState {
        private final BleConnectionManager mgr;

        IdleState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() { return "IdleState"; }

        @Override
        public boolean handleMessage(Message msg) {
            return true;
        }
    }

    static final class ServiceOnState extends BleState {
        private final BleConnectionManager mgr;

        ServiceOnState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() { return "ServiceOnState"; }

        @Override
        public void onEnter() {
            mgr.syncReceiverRegistration();
            ConnectionConfiguration cfg = mgr.config.get();
            if (cfg == null || !cfg.enabled) {
                mgr.sendMessage(MSG_CONNECTION_CONFIG_UPDATE);
            } else if (mgr.btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                mgr.sendBtAdapterStateMsg(BluetoothAdapter.STATE_ON);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            ConnectionConfiguration cfg = mgr.config.get();
            boolean enabled = cfg != null && cfg.enabled;
            switch (msg.what) {
                case MSG_BT_ADAPTER_STATE_CHANGED:
                    if (msg.arg1 == BluetoothAdapter.STATE_ON && enabled) {
                        mgr.transitionTo(mgr.discoveredState);
                    }
                    return true;
                case MSG_CONNECTION_CONFIG_UPDATE:
                    mgr.syncReceiverRegistration();
                    if (!enabled) {
                        mgr.transitionTo(mgr.errorDisconnectedState);
                        return true;
                    }
                    if (mgr.btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        mgr.transitionTo(mgr.discoveredState);
                    }
                    return true;
                case MSG_DECOMMISSION_WATCH:
                    mgr.transitionTo(mgr.errorDisconnectedState);
                    return true;
                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class DisconnectedState extends BleState {
        private final BleConnectionManager mgr;

        DisconnectedState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() {
            return "DisconnectedState";
        }

        @Override
        public void onEnter() {
            ConnectionConfiguration cfg = mgr.config.get();
            if (cfg == null || !cfg.enabled) {
                mgr.sendMessage(MSG_CONNECTION_CONFIG_UPDATE);
            } else if (mgr.btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                mgr.sendMessage(MSG_INIT);
            } else if (mgr.btAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                mgr.sendBtAdapterStateMsg(BluetoothAdapter.STATE_OFF);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            ConnectionConfiguration cfg = mgr.config.get();
            boolean enabled = cfg != null && cfg.enabled;
            switch (msg.what) {
                case MSG_INIT:
                    mgr.transitionTo(mgr.scanningState);
                    return true;
                case MSG_BT_ADAPTER_STATE_CHANGED:
                    if (msg.arg1 == BluetoothAdapter.STATE_OFF) {
                        mgr.transitionTo(mgr.disconnectedState);
                    } else if (msg.arg1 == BluetoothAdapter.STATE_ON && enabled) {
                        mgr.transitionTo(mgr.scanningState);
                    }
                    return true;
                case MSG_CONNECTION_CONFIG_UPDATE:
                    if (!enabled) {
                        mgr.transitionTo(mgr.disconnectedState);
                        return true;
                    }
                    if (mgr.btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        mgr.transitionTo(mgr.scanningState);
                    }
                    return true;

                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class ScanningState extends BleState {
        private final BleConnectionManager mgr;

        ScanningState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() {
            return "ScanningState";
        }

        @Override
        public void onEnter() {
            mgr.sendMessage(MSG_START_SCAN);
        }

        @Override
        public void onExit() {
            mgr.stopScan();
        }

        @Override
        public boolean handleMessage(Message msg) {
            ConnectionConfiguration cfg = mgr.config.get();
            switch (msg.what) {
                case MSG_START_SCAN: {
                    if (cfg == null || !cfg.enabled) {
                        mgr.transitionTo(mgr.disconnectingState);
                        return true;
                    }
                    String addr = cfg.address;
                    if (addr == null) {
                        Log.w(TAG, "ScanningState: no bluetooth address in config, cannot scan");
                        mgr.transitionTo(mgr.disconnectingState);
                        return true;
                    }

                    Log.d(TAG, "ScanningState: starting scan for " + addr);
                    mgr.bleScanner.startScan(addr, new BleScanner.ScanListener() {
                        @Override
                        public void onDeviceFound(String address) {
                            Log.d(TAG, "BLE device found: " + address);
                            mgr.removeMessages(MSG_START_SCAN);
                            mgr.removeMessages(MSG_STOP_SCAN);
                            mgr.sendMessage(MSG_START_FORCED_SCAN);
                        }

                        @Override
                        public void onScanFailed(int errorCode) {
                            Log.w(TAG, "BLE scan failed, errorCode=" + errorCode);
                            mgr.sendMessage(MSG_SCAN_FAILED);
                        }
                    });

                    mgr.sendMessageDelayed(MSG_STOP_SCAN, SCAN_TIMEOUT_MS);
                    return true;
                }

                case MSG_START_FORCED_SCAN: {
                    mgr.stopScan();
                    if (cfg == null) {
                        mgr.transitionTo(mgr.disconnectingState);
                        return true;
                    }
                    String addr = cfg.address;
                    if (addr == null) {
                        Log.w(TAG, "ScanningState: device found but address is null");
                        mgr.transitionTo(mgr.disconnectingState);
                        return true;
                    }
                    BluetoothDevice device = mgr.btAdapter.getRemoteDevice(addr);
                    Log.d(TAG, "ScanningState: connecting GATT to " + addr);
                    mgr.bleConnHelper.connect(device);
                    return true;
                }

                case MSG_GATT_CONNECTED:
                    Log.d(TAG, "ScanningState: GATT connected, transitioning to ConnectingState");
                    mgr.transitionTo(mgr.connectingState);
                    return true;

                case MSG_STOP_SCAN:
                    Log.d(TAG, "ScanningState: scan timeout");
                    mgr.stopScan();
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_SCAN_FAILED:
                    Log.w(TAG, "ScanningState: scan failed");
                    mgr.stopScan();
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_RESCHEDULE_SCAN:
                    mgr.stopScan();
                    mgr.sendMessage(MSG_START_SCAN);
                    return true;

                case MSG_CONNECTION_CONFIG_UPDATE: {
                    boolean enabled = cfg != null && cfg.enabled;
                    if (!enabled) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;
                }

                case MSG_BT_ADAPTER_STATE_CHANGED:
                    if (msg.arg1 == BluetoothAdapter.STATE_OFF) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;

                case MSG_GATT_CONNECTION_CLOSED:
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class ConnectingState extends BleState {
        private final BleConnectionManager mgr;

        ConnectingState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() { return "ConnectingState"; }

        @Override
        public void onEnter() {
            mgr.sendMessage(MSG_INIT);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    try {
                        if (Build.VERSION.SDK_INT < 28) {
                            mgr.bleConnHelper.discoverServices();
                        } else {
                            Log.d(TAG, "onServiceChanged() Connection Model enabled,"
                                    + " transitioning to Connected State.");
                            mgr.transitionTo(mgr.connectedState);
                        }
                    } catch (BleException e) {
                        mgr.handleBleException(e);
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;

                case MSG_BT_ADAPTER_STATE_CHANGED:
                    if (msg.arg1 == BluetoothAdapter.STATE_OFF) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;

                case MSG_CONNECTION_CONFIG_UPDATE: {
                    ConnectionConfiguration cfg = mgr.config.get();
                    if (cfg == null || !cfg.enabled) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;
                }

                case MSG_SERVICE_DISCOVERY_COMPLETE:
                    if (Build.VERSION.SDK_INT >= 28) {
                        Log.e(TAG, "Unexpected Services Discovered in Connecting"
                                + " w/ OnServiceChangedModel. Disconnecting.");
                        mgr.transitionTo(mgr.disconnectingState);
                    } else {
                        try {
                            mgr.gattHelper.updateCurrentTime();
                        } catch (BleException e) {
                            Log.d(TAG, "ConnectingState: failed to update current time");
                            mgr.handleBleException(e);
                        }
                        Log.d(TAG, "ConnectingState: service discovery complete,"
                                + " transitioning to ConnectedState");
                        mgr.transitionTo(mgr.connectedState);
                    }
                    return true;

                case MSG_GATT_CONNECTION_CLOSED:
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_ON_SERVICE_CHANGED:
                    if (Build.VERSION.SDK_INT >= 28) {
                        mgr.transitionTo(mgr.connectedState);
                    }
                    return true;

                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class ConnectedState extends BleState {
        private final BleConnectionManager mgr;

        ConnectedState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() {
            return "ConnectedState";
        }

        @Override
        public void onEnter() {
            mgr.scanAttemptCount.set(0);
            mgr.sendMessage(MSG_INIT);
        }

        @Override
        public void onExit() {}

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    mgr.sendMessage(MSG_READY_TO_SETUP_ANCS);
                    return true;

                case MSG_UPDATE_TIME:
                    try {
                        mgr.gattHelper.updateCurrentTime();
                    } catch (BleException e) {
                        Log.w(TAG, "ConnectedState: failed to update time");
                        mgr.handleBleException(e);
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;

                case MSG_ON_SERVICE_CHANGED:
                    Log.d(TAG, "ConnectedState: onServiceChanged received");
                    try {
                        mgr.gattHelper.updateCurrentTime();
                    } catch (BleException e) {
                        Log.w(TAG, "ConnectedState: exception after service change");
                        mgr.handleBleException(e);
                        if (Build.VERSION.SDK_INT < 28) {
                            mgr.transitionTo(mgr.disconnectingState);
                        }
                    }
                    return true;

                case MSG_GATT_CONNECTION_CLOSED:
                    Log.d(TAG, "ConnectedState: GATT connection closed, disconnecting");
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_RESET_CONNECTION:
                    Log.d(TAG, "ConnectedState: reset connection requested");
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_DECOMMISSION_WATCH:
                    Log.d(TAG, "ConnectedState: decommissioning watch");
                    mgr.transitionTo(mgr.errorDisconnectedState);
                    return true;

                case MSG_BT_ADAPTER_STATE_CHANGED:
                    if (msg.arg1 == BluetoothAdapter.STATE_OFF) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;

                case MSG_CONNECTION_CONFIG_UPDATE: {
                    ConnectionConfiguration cfg = mgr.config.get();
                    if (cfg == null || !cfg.enabled) {
                        mgr.transitionTo(mgr.disconnectingState);
                    }
                    return true;
                }

                case MSG_RECONNECT_CHARACTERISTIC_CHANGED:
                case MSG_RESET_CHARACTERICTIC_CHANGED:
                    mgr.transitionTo(mgr.disconnectingState);
                    return true;

                case MSG_READY_TO_SETUP_ANCS:
                    return true;

                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class DisconnectingState extends BleState {
        private final BleConnectionManager mgr;

        DisconnectingState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() {
            return "DisconnectingState";
        }

        @Override
        public void onEnter() {
            mgr.sendMessage(MSG_INIT);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    try {
                        if (mgr.bleConnHelper.isConnected()) {
                            Log.d(TAG, "DisconnectingState: disconnecting GATT");
                            mgr.bleConnHelper.disconnect();
                        } else {
                            Log.d(TAG, "DisconnectingState: already disconnected");
                        }
                    } catch (BleException e) {
                        Log.w(TAG, "DisconnectingState: exception while disconnecting");
                    } finally {
                        mgr.transitionTo(mgr.discoveredState);
                    }
                    return true;

                case MSG_GATT_CONNECTION_CLOSED:
                    mgr.transitionTo(mgr.discoveredState);
                    return true;

                default:
                    return mgr.handleUnhandledMessage(msg);
            }
        }
    }

    static final class ServiceOffState extends BleState {
        private final BleConnectionManager mgr;

        ServiceOffState(BleConnectionManager mgr) {
            this.mgr = mgr;
        }

        @Override
        public String getName() {
            return "ServiceOffState";
        }

        @Override
        public void onEnter() {
            if (mgr.isReceiverRegistered.compareAndSet(true, false)) {
                mgr.context.unregisterReceiver(mgr.btStateReceiver);
            }
            ConnectionConfiguration cfg = mgr.config.get();
            if (cfg != null && cfg.enabled) {
                mgr.sendMessage(MSG_CONNECTION_CONFIG_UPDATE);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_CONNECTION_CONFIG_UPDATE) {
                ConnectionConfiguration cfg = mgr.config.get();
                if (cfg != null && cfg.enabled) {
                    mgr.transitionTo(mgr.disconnectedState);
                }
            }
            return true;
        }
    }

}
