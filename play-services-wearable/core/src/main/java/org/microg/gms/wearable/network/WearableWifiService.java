package org.microg.gms.wearable.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.WearableStatusCodes;

import org.microg.gms.wearable.DataItemInternal;
import org.microg.gms.wearable.DataItemRecord;
import org.microg.gms.wearable.RpcMessageTransport;
import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.proto.ProtoDuration;
import org.microg.gms.wearable.proto.WifiConfigurationInfo;
import org.microg.gms.wearable.proto.WifiConfigurationList;
import org.microg.gms.wearable.proto.WifiConnectRequest;
import org.microg.gms.wearable.proto.WifiConnectResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class WearableWifiService {
    private static final String TAG = "WearableWifiService";

    public static final String GMS_PACKAGE = "com.google.android.gms";
    public static final String GMS_SIGNATURE = "38918a453d07199354f8b19af05ec6562ced5788";

    public static final String PATH_WIFI_SYNC_PROTO = "/wifi_sync_proto";
    public static final String PATH_SYNC_WIFI_CREDENTIALS = "/sync_wifi_credentials";
    public static final String PATH_WIFI_CONNECT_IMMEDIATE = "/wifi_connect_immediate";

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_INVALID_REQUEST = 2;
    public static final int RESULT_NETWORK_UNAVAILABLE = 3;
    public static final int RESULT_UNSUPPORTED_SDK = 4;

    private static final int STATUS_WIFI_CONNECT_UNSUPPORTED = 4009;
    private static final int STATUS_WIFI_CONNECT_FAILED = 4015;

    private static final long CONNECT_REQUEST_TIMEOUT_MS = 60_000L;
    private static final long CONNECT_RESPONSE_GRACE_MS = 5_000L;
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 300_000L;

    private static final String PREFS_NAME = "wearable.wifi_service";
    private static final String PREF_LAST_SYNC_WRITTEN = "last_sync_dataitem_written";
    private static final String SETUP_WIZARD_SETTING = "setup_wizard_has_run";

    private static final String ACTION_CONFIGURED_NETWORKS_CHANGE = "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";
    private static final String EXTRA_CHANGE_REASON = "changeReason";
    private static final String EXTRA_WIFI_CONFIGURATION = "wifiConfiguration";
    private static final int CHANGE_REASON_ADDED = 0;
    private static final int CHANGE_REASON_CONFIG_CHANGE = 2;
    private static final int WIFI_STATE_ENABLED = 3;
    private static final String FEATURE_WATCH = "android.hardware.type.watch";
    private static final String UNKNOWN_SSID = "<unknown ssid>";

    private static final int SECURITY_NONE = WifiConfiguration.KeyMgmt.NONE;
    private static final int SECURITY_WPA_PSK = WifiConfiguration.KeyMgmt.WPA_PSK;
    private static final int SECURITY_WPA_EAP = WifiConfiguration.KeyMgmt.WPA_EAP;

    private static final int SECURITY_SAE = WifiConfiguration.KeyMgmt.SAE;

    private static final int SECURITY_OWE = WifiConfiguration.KeyMgmt.OWE;

    private static final int LEGACY_OPEN = 0;
    private static final int LEGACY_WEP = 1;
    private static final int LEGACY_PSK = 2;
    private static final int LEGACY_EAP = 3;

    private static final int MSG_INIT = 1;
    private static final int MSG_DATA_ITEM = 2;
    private static final int MSG_WIFI_ENABLED = 3;
    private static final int MSG_CREDENTIAL_CHANGED = 4;
    private static final int MSG_CONNECT = 5;
    private static final int MSG_CONNECT_AVAILABLE = 6;
    private static final int MSG_CONNECT_UNAVAILABLE = 7;

    public interface StatusCallback {
        void onStatus(Status status);
    }

    private static final class ConnectRequest {
        final boolean requiresResponse;
        final int requestId;
        final String sourceNodeId;
        final WifiConnectRequest request;

        ConnectRequest(boolean requiresResponse, int requestId, String sourceNodeId, WifiConnectRequest request) {
            this.requiresResponse = requiresResponse;
            this.requestId = requestId;
            this.sourceNodeId = sourceNodeId;
            this.request = request;
        }
    }

    private final Context context;
    private final WearableImpl wearable;
    private final boolean isWatch;
    @Nullable
    private final WifiManager wifiManager;
    @Nullable
    private final ConnectivityManager connectivityManager;
    private final SharedPreferences prefs;
    private final HandlerThread handlerThread;
    private final Handler handler;

    private final AtomicBoolean setupComplete = new AtomicBoolean(false);
    private final AtomicBoolean sawProtoItem = new AtomicBoolean(false);
    private final AtomicBoolean knownNetworksLoaded = new AtomicBoolean(false);
    private volatile boolean initialSyncDone = false;

    private final Map<String, WifiConfigurationInfo> knownNetworks = new ConcurrentHashMap<>();

    private final Object pendingLock = new Object();
    private DataItemRecord pendingRecord;
    private ConnectivityManager.NetworkCallback syncNetworkCallback;

    private ConnectivityManager.NetworkCallback connectCallback;
    private ConnectRequest activeConnect;

    private BroadcastReceiver savedNetworkReceiver;
    private BroadcastReceiver wifiStateReceiver;
    private ContentObserver setupObserver;

    public WearableWifiService(Context context, WearableImpl wearable) {
        Context app = context.getApplicationContext();
        this.context = app != null ? app : context;
        this.wearable = wearable;
        this.isWatch = this.context.getPackageManager().hasSystemFeature(FEATURE_WATCH);
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.handlerThread = new HandlerThread("WearWifiServiceHandler", Process.THREAD_PRIORITY_BACKGROUND);
        this.handlerThread.start();
        this.handler = new WifiHandler(handlerThread.getLooper());
        start();
    }

    @Nullable
    public WifiManager getWifiManager() {
        return wifiManager;
    }

    private void start() {
        if (!isWatch) {
            savedNetworkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    onSavedNetworksChanged(intent);
                }
            };
            registerReceiverSafely(savedNetworkReceiver, new IntentFilter(ACTION_CONFIGURED_NETWORKS_CHANGE));
            return;
        }

        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                        && intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WIFI_STATE_ENABLED) {
                    handler.sendEmptyMessage(MSG_WIFI_ENABLED);
                }
            }
        };
        registerReceiverSafely(wifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        final ContentResolver resolver = context.getContentResolver();
        if (Settings.System.getInt(resolver, SETUP_WIZARD_SETTING, 0) != 0) {
            setupComplete.set(true);
            handler.sendEmptyMessage(MSG_INIT);
        } else {
            Log.d(TAG, "Waiting for setup wizard to complete before syncing wifi credentials");
            setupObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    if (Settings.System.getInt(resolver, SETUP_WIZARD_SETTING, 0) == 1) {
                        Log.d(TAG, "Setup wizard completed, performing initial wifi sync");
                        setupComplete.set(true);
                        handler.sendEmptyMessage(MSG_INIT);
                    }
                }
            };
            resolver.registerContentObserver(Settings.System.getUriFor(SETUP_WIZARD_SETTING), false, setupObserver);
        }
    }

    public void stop() {
        unregisterReceiverSafely(savedNetworkReceiver);
        unregisterReceiverSafely(wifiStateReceiver);
        savedNetworkReceiver = null;
        wifiStateReceiver = null;
        if (setupObserver != null) {
            context.getContentResolver().unregisterContentObserver(setupObserver);
            setupObserver = null;
        }
        synchronized (pendingLock) {
            unregisterCallbackSafely(syncNetworkCallback);
            syncNetworkCallback = null;
            pendingRecord = null;
        }
        handler.post(() -> {
            unregisterCallbackSafely(connectCallback);
            connectCallback = null;
            activeConnect = null;
        });
        handlerThread.quitSafely();
    }

    private void registerReceiverSafely(BroadcastReceiver receiver, IntentFilter filter) {
        try {
            context.registerReceiver(receiver, filter);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not register receiver for " + filter.getAction(0), e);
        }
    }

    private void unregisterReceiverSafely(@Nullable BroadcastReceiver receiver) {
        if (receiver == null) return;
        try {
            context.unregisterReceiver(receiver);
        } catch (RuntimeException ignored) {
        }
    }

    private void unregisterCallbackSafely(@Nullable ConnectivityManager.NetworkCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        if (callback == null || connectivityManager == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(callback);
        } catch (RuntimeException ignored) {
        }
    }

    private final class WifiHandler extends Handler {
        WifiHandler(Looper looper) {
            super(looper);
        }

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_INIT:
                        performInitialSync();
                        break;
                    case MSG_DATA_ITEM:
                        considerDataItem((DataItemRecord) msg.obj);
                        break;
                    case MSG_WIFI_ENABLED:
                        applyPendingDataItem("WifiOnReceiver");
                        break;
                    case MSG_CREDENTIAL_CHANGED:
                        syncNewCredential();
                        break;
                    case MSG_CONNECT:
                        connectImmediately((ConnectRequest) msg.obj);
                        break;
                    case MSG_CONNECT_AVAILABLE:
                        finishConnect(RESULT_SUCCESS);
                        break;
                    case MSG_CONNECT_UNAVAILABLE:
                        finishConnect(RESULT_NETWORK_UNAVAILABLE);
                        break;
                    default:
                        break;
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Failure handling message " + msg.what, e);
            }
        }
    }

    private void onSavedNetworksChanged(Intent intent) {
        if (isWatch || !ACTION_CONFIGURED_NETWORKS_CHANGE.equals(intent.getAction())) return;
        int reason = intent.getIntExtra(EXTRA_CHANGE_REASON, CHANGE_REASON_ADDED);
        if (reason != CHANGE_REASON_ADDED && reason != CHANGE_REASON_CONFIG_CHANGE) {
            Log.d(TAG, "Not syncing credentials for change reason: " + reason);
            return;
        }
        Log.d(TAG, "Wifi credentials " + (reason == CHANGE_REASON_ADDED ? "ADDED" : "CHANGED") + ", syncing...");
        WifiConfiguration changed = null;
        try {
            changed = intent.getParcelableExtra(EXTRA_WIFI_CONFIGURATION);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read changed configuration", e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && changed != null && Hidden.isEphemeral(changed)) {
            Log.d(TAG, "Not syncing change to ephemeral credential: " + changed.SSID);
            return;
        }
        handler.sendEmptyMessage(MSG_CREDENTIAL_CHANGED);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void syncNewCredential() {
        try {
            if (!syncCredentials(false)) {
                Log.w(TAG, "Failed to sync new wifi credential.");
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Error trying to sync new wifi credentials", e);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public boolean syncCredentials(boolean enableWifiTemporarily) {
        if (isWatch) return false;
        Log.i(TAG, "Syncing Wifi Credentials");
        if (wifiManager == null) {
            Log.w(TAG, "No WiFi service. Exit...");
            return false;
        }
        List<WifiConfiguration> networks = getNetworks(enableWifiTemporarily);
        if (networks.isEmpty()) return false;
        String localNodeId = wearable.getLocalNodeId();
        if (TextUtils.isEmpty(localNodeId)) {
            Log.w(TAG, "No local node id, cannot publish credentials");
            return false;
        }
        Log.d(TAG, "Number of wifi credentials: " + networks.size());

        List<WifiConfigurationInfo> infos = new ArrayList<>(networks.size());
        for (WifiConfiguration config : networks) infos.add(toInfo(config));
        publish(localNodeId, PATH_WIFI_SYNC_PROTO,
                new WifiConfigurationList.Builder().networks(infos).build().encode());
        publish(localNodeId, PATH_SYNC_WIFI_CREDENTIALS, buildLegacyPayload(networks));
        return true;
    }

    private void publish(String localNodeId, String path, byte[] data) {
        DataItemInternal item = new DataItemInternal(localNodeId, path);
        item.data = data;
        DataItemRecord record = wearable.putDataItem(GMS_PACKAGE, GMS_SIGNATURE, localNodeId, item);
        wearable.syncRecordToAll(record);
    }

    private static byte[] buildLegacyPayload(List<WifiConfiguration> networks) {
        ArrayList<DataMap> list = new ArrayList<>(networks.size());
        for (WifiConfiguration config : networks) {
            DataMap map = new DataMap();
            map.putString("ssid", unquote(config.SSID));
            if (config.hiddenSSID) map.putInt("hiddenSsid", 1);
            if (config.allowedKeyManagement.get(SECURITY_WPA_PSK)) {
                map.putInt("key_mgmt", LEGACY_PSK);
                map.putString("key", unquote(config.preSharedKey));
            } else if (config.allowedKeyManagement.get(SECURITY_WPA_EAP)
                    || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
                map.putInt("key_mgmt", LEGACY_EAP);
            } else if (config.wepKeys != null && config.wepKeys[0] != null) {
                map.putInt("key_mgmt", LEGACY_WEP);
                map.putString("key", unquote(config.wepKeys[0]));
            } else {
                map.putInt("key_mgmt", LEGACY_OPEN);
            }
            list.add(map);
        }
        DataMap root = new DataMap();
        root.putDataMapArrayList("list", list);
        root.putInt("source", 1);
        return root.toByteArray();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void syncCredentialToNode(final String nodeId, final String ssid, final StatusCallback callback) {
        handler.post(() -> {
            try {
                sendConnectRequest(nodeId, ssid, callback);
            } catch (RuntimeException e) {
                Log.w(TAG, "syncCredentialToNode failed", e);
                callback.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void syncConnectedCredentialToNode(final String nodeId, final StatusCallback callback) {
        handler.post(() -> {
            String ssid = null;
            try {
                WifiInfo info = wifiManager != null ? wifiManager.getConnectionInfo() : null;
                if (info != null) ssid = info.getSSID();
            } catch (RuntimeException e) {
                Log.w(TAG, "Could not read connection info", e);
            }
            if (TextUtils.isEmpty(ssid) || UNKNOWN_SSID.equals(ssid)) {
                callback.onStatus(new Status(WearableStatusCodes.WIFI_CREDENTIAL_SYNC_NO_CREDENTIAL_FETCHED));
                return;
            }
            try {
                sendConnectRequest(nodeId, ssid, callback);
            } catch (RuntimeException e) {
                Log.w(TAG, "syncConnectedCredentialToNode failed", e);
                callback.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void sendConnectRequest(String nodeId, String ssid, final StatusCallback callback) {
        String quoted = quote(ssid);
        WifiConfiguration found = null;
        for (WifiConfiguration config : Hidden.privilegedNetworks(wifiManager)) {
            if (config.SSID != null && config.SSID.equals(quoted)) {
                found = config;
                break;
            }
        }
        if (found == null) {
            callback.onStatus(new Status(WearableStatusCodes.WIFI_CREDENTIAL_SYNC_NO_CREDENTIAL_FETCHED));
            return;
        }

        byte[] payload = new WifiConnectRequest.Builder()
                .config(toInfo(found))
                .timeout(toDuration(CONNECT_REQUEST_TIMEOUT_MS))
                .build().encode();

        final String target = wearable.resolveToWearableNodeId(nodeId);
        int requestId = wearable.getRpcTransport().sendRequest(GMS_PACKAGE, GMS_SIGNATURE, target,
                PATH_WIFI_CONNECT_IMMEDIATE, payload, new MessageOptions(RpcMessageTransport.API_PRIO_LOW));
        if (requestId == -1) {
            callback.onStatus(new Status(WearableStatusCodes.TARGET_NODE_NOT_CONNECTED));
            return;
        }
        wearable.getRpcHelper().addResponseListener(target, requestId,
                CONNECT_REQUEST_TIMEOUT_MS + CONNECT_RESPONSE_GRACE_MS,
                data -> {
                    int status = statusFromResponse(data);
                    if (status == STATUS_WIFI_CONNECT_FAILED || status == STATUS_WIFI_CONNECT_UNSUPPORTED) {
                        Log.w(TAG, "Immediate connect failed (" + status + "); credentials still synced via data items");
                        status = CommonStatusCodes.SUCCESS;
                    }
                    callback.onStatus(new Status(status));
                },
                () -> {
                    Log.w(TAG, "Immediate connect timed out; relying on data item sync");
                    callback.onStatus(new Status(CommonStatusCodes.SUCCESS));
                });
    }

    private static int statusFromResponse(@Nullable byte[] data) {
        if (data == null) return CommonStatusCodes.INTERNAL_ERROR;
        try {
            Integer result = WifiConnectResponse.ADAPTER.decode(data).result;
            switch (result != null ? result : 0) {
                case RESULT_SUCCESS:
                    return CommonStatusCodes.SUCCESS;
                case RESULT_NETWORK_UNAVAILABLE:
                    return STATUS_WIFI_CONNECT_FAILED;
                case RESULT_UNSUPPORTED_SDK:
                    return STATUS_WIFI_CONNECT_UNSUPPORTED;
                default:
                    return CommonStatusCodes.INTERNAL_ERROR;
            }
        } catch (IOException e) {
            return CommonStatusCodes.INTERNAL_ERROR;
        }
    }

    public void onDataItemsChanged(List<DataItemRecord> records) {
        if (!isWatch || wifiManager == null) return;
        for (DataItemRecord record : records) {
            if (!GMS_PACKAGE.equals(record.packageName) || !GMS_SIGNATURE.equals(record.signatureDigest)) {
                continue;
            }
            String path = record.dataItem.path;
            if (PATH_WIFI_SYNC_PROTO.equals(path)) {
                sawProtoItem.set(true);
            } else if (!PATH_SYNC_WIFI_CREDENTIALS.equals(path) || sawProtoItem.get()) {
                continue;
            }
            if (record.deleted) {
                Log.d(TAG, "Wifi Credentials data item was deleted.");
                continue;
            }
            if (setupComplete.get()) {
                handler.sendMessage(handler.obtainMessage(MSG_DATA_ITEM, record));
                return;
            }
            Log.d(TAG, "Received wifi creds but setup has not completed, delaying until completion");
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void performInitialSync() {
        if (wifiManager == null) return;
        if (!knownNetworksLoaded.get()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                for (WifiConfiguration config : getNetworks(false)) {
                    knownNetworks.put(Hidden.key(config), toInfo(config));
                }
            }
            knownNetworksLoaded.set(true);
        }
        DataItemRecord proto = queryDataItem(PATH_WIFI_SYNC_PROTO);
        if (proto != null) {
            sawProtoItem.set(true);
            considerDataItem(proto);
        } else if (!sawProtoItem.get()) {
            considerDataItem(queryDataItem(PATH_SYNC_WIFI_CREDENTIALS));
        }
        initialSyncDone = true;
    }

    @Nullable
    private DataItemRecord queryDataItem(String path) {
        Cursor cursor = wearable.getNodeDatabase().getDataItemsByHostAndPath(GMS_PACKAGE, GMS_SIGNATURE, null, path);
        if (cursor == null) return null;
        try {
            return cursor.moveToFirst() ? DataItemRecord.fromCursor(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private void considerDataItem(@Nullable DataItemRecord record) {
        if (record == null) {
            Log.d(TAG, "Wifi sync skipped - no data item present.");
            return;
        }
        if (prefs.getLong(PREF_LAST_SYNC_WRITTEN, -1L) >= record.lastModified) {
            Log.d(TAG, "Wifi sync skipped - data item has not changed since last sync.");
            return;
        }
        synchronized (pendingLock) {
            pendingRecord = record;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && syncNetworkCallback == null && connectivityManager != null) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
                syncNetworkCallback = new ConnectivityManager.NetworkCallback();
                connectivityManager.requestNetwork(request, syncNetworkCallback);
            }
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.d(TAG, "Syncing credentials from handler.");
                applyPendingDataItem("SyncHandler");
            }
        }
    }

    private void applyPendingDataItem(String reason) {
        synchronized (pendingLock) {
            DataItemRecord record = pendingRecord;
            if (record == null || wifiManager == null) return;
            Log.d(TAG, "Syncing credentials from " + reason);

            String path = record.dataItem.path;
            byte[] data = record.dataItem.data;
            if (data == null) {
                Log.w(TAG, "Wifi data item has no payload");
                return;
            }
            if (PATH_WIFI_SYNC_PROTO.equals(path)) {
                try {
                    WifiConfigurationList list = WifiConfigurationList.ADAPTER.decode(data);
                    Log.w(TAG, "Adding " + list.networks.size() + " networks from proto.");
                    int added = 0;
                    for (WifiConfigurationInfo info : list.networks) {
                        if (applyConfiguration(info, false)) added++;
                    }
                    if (added > 0) wifiManager.saveConfiguration();
                } catch (IOException e) {
                    Log.w(TAG, "Received invalid Wifi Sync proto", e);
                    return;
                }
            } else if (PATH_SYNC_WIFI_CREDENTIALS.equals(path)) {
                if (!applyLegacy(DataMap.fromByteArray(data))) return;
            } else {
                Log.w(TAG, "Sync initialized with a non-wifi data item");
                return;
            }

            unregisterCallbackSafely(syncNetworkCallback);
            syncNetworkCallback = null;
            prefs.edit().putLong(PREF_LAST_SYNC_WRITTEN, record.lastModified).commit();
            pendingRecord = null;
        }
    }

    private boolean applyLegacy(DataMap map) {
        Log.d(TAG, "Credential source is from: " + map.getInt("source"));
        ArrayList<DataMap> list = map.getDataMapArrayList("list");
        if (list == null) {
            Log.w(TAG, "Received an empty wifi credentials data item.");
            return false;
        }
        Log.d(TAG, "Adding " + list.size() + " networks.");
        for (DataMap entry : list) {
            String ssid = entry.getString("ssid");
            int keyMgmt = entry.getInt("key_mgmt");
            String key = entry.getString("key");
            if (ssid == null || keyMgmt == LEGACY_EAP) continue;
            boolean hidden = entry.getInt("hiddenSsid") != 0;

            WifiConfiguration config = new WifiConfiguration();
            config.SSID = quote(ssid);
            config.hiddenSSID = hidden;
            if (keyMgmt == LEGACY_OPEN) {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                config.priority = 1;
                config.status = WifiConfiguration.Status.ENABLED;
                if (keyMgmt == LEGACY_WEP) {
                    configureWep(config, key);
                } else if (keyMgmt == LEGACY_PSK) {
                    configureWpaPsk(config, key);
                } else {
                    Log.w(TAG, "Unrecognized key management scheme: " + keyMgmt);
                    continue;
                }
            }
            enableNetwork(addNetwork(config), false);
        }
        Log.d(TAG, "Saving configurations to disk...");
        wifiManager.saveConfiguration();
        return true;
    }

    public boolean onRpcRequest(int requestId, @Nullable String path, @Nullable byte[] data,
                                String sourceNodeId, boolean requiresResponse) {
        if (!isWatch || !PATH_WIFI_CONNECT_IMMEDIATE.equals(path)) return false;
        Log.i(TAG, "Received immediate wifi connection request.");
        ConnectRequest pending = new ConnectRequest(requiresResponse, requestId, sourceNodeId, null);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.i(TAG, "SDK version on this device is too low (" + Build.VERSION.SDK_INT
                    + ") for immediate wifi connect; declining.");
            reply(pending, RESULT_UNSUPPORTED_SDK);
            return true;
        }
        try {
            WifiConnectRequest request = WifiConnectRequest.ADAPTER.decode(data != null ? data : new byte[0]);
            handler.sendMessage(handler.obtainMessage(MSG_CONNECT,
                    new ConnectRequest(requiresResponse, requestId, sourceNodeId, request)));
        } catch (IOException e) {
            Log.w(TAG, "Received invalid Wifi Sync proto", e);
            reply(pending, RESULT_INVALID_REQUEST);
        }
        return true;
    }

    private void connectImmediately(ConnectRequest connect) {
        if (wifiManager == null || connectivityManager == null) {
            reply(connect, RESULT_NETWORK_UNAVAILABLE);
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            reply(connect, RESULT_UNSUPPORTED_SDK);
            return;
        }
        activeConnect = connect;
        unregisterCallbackSafely(connectCallback);
        connectCallback = null;

        wifiManager.setWifiEnabled(true);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        connectCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                handler.sendEmptyMessage(MSG_CONNECT_AVAILABLE);
            }

            @Override
            public void onUnavailable() {
                handler.sendEmptyMessage(MSG_CONNECT_UNAVAILABLE);
            }
        };

        long timeoutMs = durationToMillis(connect.request.timeout);
        if (timeoutMs <= 0) timeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        connectivityManager.requestNetwork(request, connectCallback, (int) Math.min(timeoutMs, Integer.MAX_VALUE));

        WifiConfigurationInfo info = connect.request.config != null
                ? connect.request.config : new WifiConfigurationInfo.Builder().build();
        applyConfiguration(info, true);
    }

    private void finishConnect(int result) {
        unregisterCallbackSafely(connectCallback);
        connectCallback = null;
        reply(activeConnect, result);
        activeConnect = null;
    }

    private void reply(@Nullable ConnectRequest connect, int result) {
        if (connect == null || !connect.requiresResponse) return;
        byte[] payload = new WifiConnectResponse.Builder().result(result).build().encode();
        wearable.getRpcTransport().sendResponse(GMS_PACKAGE, GMS_SIGNATURE, connect.sourceNodeId,
                PATH_WIFI_CONNECT_IMMEDIATE, payload, connect.requestId);
    }

    static WifiConfigurationInfo toInfo(WifiConfiguration config) {
        WifiConfigurationInfo.Builder b = new WifiConfigurationInfo.Builder()
                .ssid(unquote(config.SSID))
                .hiddenSsid(config.hiddenSSID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Boolean autoJoin = Hidden.getBoolean(config, "allowAutojoin");
            Integer macRandomization = Hidden.getInt(config, "macRandomizationSetting");
            if (autoJoin != null) b.allowAutoJoin(autoJoin);
            if (macRandomization != null) b.macRandomizationSetting(macRandomization);
        }
        if (config.allowedKeyManagement.get(SECURITY_WPA_PSK)) {
            b.securityType(WifiConfigurationInfo.WifiSecurityType.fromValue(SECURITY_WPA_PSK)).preSharedKey(unquote(config.preSharedKey));
        } else if (config.allowedKeyManagement.get(SECURITY_WPA_EAP)
                || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            b.securityType(WifiConfigurationInfo.WifiSecurityType.fromValue(SECURITY_WPA_EAP));
        } else if (config.allowedKeyManagement.get(SECURITY_SAE)) {
            b.securityType(WifiConfigurationInfo.WifiSecurityType.fromValue(SECURITY_SAE)).preSharedKey(unquote(config.preSharedKey));
        } else if (config.allowedKeyManagement.get(SECURITY_OWE)) {
            b.securityType(WifiConfigurationInfo.WifiSecurityType.fromValue(SECURITY_OWE));
        } else {
            b.securityType(WifiConfigurationInfo.WifiSecurityType.fromValue(SECURITY_NONE));
            if (config.wepKeys != null && config.wepKeys[0] != null) b.wepKey(unquote(config.wepKeys[0]));
        }
        return b.build();
    }

    @Nullable
    private static WifiConfiguration toConfiguration(WifiConfigurationInfo info) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = quote(info.ssid);
        config.hiddenSSID = Boolean.TRUE.equals(info.hiddenSsid);
        config.status = WifiConfiguration.Status.ENABLED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (info.allowAutoJoin != null) Hidden.setBoolean(config, "allowAutojoin", info.allowAutoJoin);
            if (info.macRandomizationSetting != null) {
                Hidden.setInt(config, "macRandomizationSetting", info.macRandomizationSetting);
            }
        }

        int type = info.securityType != null ? info.securityType.getValue() : SECURITY_NONE;
        if (type == SECURITY_NONE) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            if (info.wepKey != null) {
                configureWep(config, info.wepKey);
            } else {
                config.priority = 0;
            }
        } else if (type == SECURITY_WPA_PSK) {
            configureWpaPsk(config, info.preSharedKey);
        } else if (type == SECURITY_SAE || type == SECURITY_OWE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Log.w(TAG, "Current android platform is not supporting WPA3. Network skipped: " + info.ssid);
                return null;
            }
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.GCMP_256);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.GCMP_256);
            Hidden.setBoolean(config, "requirePmf", true);
            if (type == SECURITY_OWE) {
                config.allowedKeyManagement.set(SECURITY_OWE);
            } else {
                config.allowedKeyManagement.set(SECURITY_SAE);
                setPassphrase(config, info.preSharedKey);
            }
        } else {
            Log.e(TAG, "Security type " + type + " of " + info.ssid + " is unsupported; skipping.");
            return null;
        }
        return config;
    }

    private static void configureWep(WifiConfiguration config, @Nullable String key) {
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.wepKeys = new String[4];
        int length = key == null ? 0 : key.length();
        if (length > 0) {
            boolean hex = (length == 10 || length == 26 || length == 58) && key.matches("[0-9A-Fa-f]*");
            config.wepKeys[0] = hex ? key : quote(key);
            config.wepTxKeyIndex = 0;
        }
    }

    private static void configureWpaPsk(WifiConfiguration config, @Nullable String key) {
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        setPassphrase(config, key);
    }

    private static void setPassphrase(WifiConfiguration config, @Nullable String key) {
        if (key == null || key.isEmpty()) return;

        config.preSharedKey = key.matches("[0-9A-Fa-f]{64}") ? key : quote(key);
    }

    private boolean applyConfiguration(WifiConfigurationInfo info, boolean immediate) {
        WifiConfiguration config = toConfiguration(info);
        if (config == null) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || immediate) {
            config.networkId = addNetwork(config);
            enableNetwork(config.networkId, immediate);
            return true;
        }

        String key = Hidden.key(config);
        WifiConfigurationInfo known = knownNetworks.get(key);
        if (known == null) {
            config.networkId = addNetwork(config);
            enableNetwork(config.networkId, false);
            remember(config, key, info);
            return true;
        }
        if (sameNetwork(known, info)) {
            return true;
        }

        config.networkId = addNetwork(config);
        remember(config, key, info);
        return true;
    }

    private static boolean sameNetwork(WifiConfigurationInfo a, WifiConfigurationInfo b) {
        return Objects.equals(a.ssid, b.ssid)
                && Boolean.TRUE.equals(a.hiddenSsid) == Boolean.TRUE.equals(b.hiddenSsid)
                && Objects.equals(a.allowAutoJoin, b.allowAutoJoin)
                && Objects.equals(a.macRandomizationSetting, b.macRandomizationSetting)
                && Objects.equals(a.securityType, b.securityType)
                && Objects.equals(a.preSharedKey, b.preSharedKey)
                && Objects.equals(a.wepKey, b.wepKey);
    }

    private void remember(WifiConfiguration config, String key, WifiConfigurationInfo info) {
        if (config.networkId == -1) {
            Log.w(TAG, "Network id is invalid. Not adding to store.");
            return;
        }
        knownNetworks.put(key, info);
        Log.d(TAG, "Network <" + config.SSID + "> added to store.");
    }

    private int addNetwork(WifiConfiguration config) {
        int id = wifiManager.addNetwork(config);
        if (id == -1) {
            Log.w(TAG, "Add network <" + config.SSID + "> failed.");
        } else {
            Log.i(TAG, "Network <" + config.SSID + "> added.");
        }
        return id;
    }

    private void enableNetwork(int networkId, boolean disableOthers) {
        if (networkId == -1) {
            Log.w(TAG, "Network id is invalid. Enable network failed.");
            return;
        }
        wifiManager.enableNetwork(networkId, disableOthers);
        Log.i(TAG, "Network with id <" + networkId + "> enabled.");
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private List<WifiConfiguration> getNetworks(boolean enableWifiTemporarily) {
        if (wifiManager == null) return Collections.emptyList();
        boolean enabledByUs = false;
        if (enableWifiTemporarily && !wifiManager.isWifiEnabled() && !isAirplaneModeOn()) {
            enabledByUs = true;
            wifiManager.setWifiEnabled(true);
        }
        List<WifiConfiguration> networks = Hidden.privilegedNetworks(wifiManager);
        if (enabledByUs && !isAirplaneModeOn()) {
            wifiManager.setWifiEnabled(false);
        }
        return networks;
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    static String quote(@Nullable String s) {
        if (TextUtils.isEmpty(s)) return "";
        return (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') ? s : "\"" + s + "\"";
    }

    static String unquote(@Nullable String s) {
        if (TextUtils.isEmpty(s)) return "";
        int length = s.length();
        if (length <= 1 || s.charAt(0) != '"') return s;
        return s.charAt(length - 1) == '"' ? s.substring(1, length - 1) : s;
    }

    private static ProtoDuration toDuration(long millis) {
        return new ProtoDuration.Builder()
                .seconds(millis / 1000L)
                .nanos((int) ((millis % 1000L) * 1_000_000L))
                .build();
    }

    private static long durationToMillis(@Nullable ProtoDuration duration) {
        if (duration == null) return 0;
        long seconds = duration.seconds != null ? duration.seconds : 0;
        int nanos = duration.nanos != null ? duration.nanos : 0;
        return seconds * 1000L + nanos / 1_000_000L;
    }

    private static final class Hidden {

        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        static List<WifiConfiguration> privilegedNetworks(@Nullable WifiManager manager) {
            if (manager == null) return Collections.emptyList();
            try {
                Method method = WifiManager.class.getMethod("getPrivilegedConfiguredNetworks");
                Object result = method.invoke(manager);
                if (result instanceof List) return (List<WifiConfiguration>) result;
            } catch (ReflectiveOperationException | RuntimeException e) {
                Log.d(TAG, "getPrivilegedConfiguredNetworks unavailable, falling back: " + e);
            }
            try {
                List<WifiConfiguration> networks = manager.getConfiguredNetworks();
                if (networks != null) return networks;
            } catch (SecurityException e) {
                Log.w(TAG, "getConfiguredNetworks permission denied", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "getConfiguredNetworks failed", e);
            }
            return Collections.emptyList();
        }

        static String key(WifiConfiguration config) {
            try {
                Object key = WifiConfiguration.class.getMethod("getKey").invoke(config);
                if (key instanceof String) return (String) key;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            return config.SSID + "|" + config.allowedKeyManagement;
        }

        static boolean isEphemeral(WifiConfiguration config) {
            try {
                Object result = WifiConfiguration.class.getMethod("isEphemeral").invoke(config);
                return Boolean.TRUE.equals(result);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }

        @Nullable
        static Boolean getBoolean(Object target, String field) {
            try {
                Field f = target.getClass().getField(field);
                return f.getBoolean(target);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }

        @Nullable
        static Integer getInt(Object target, String field) {
            try {
                Field f = target.getClass().getField(field);
                return f.getInt(target);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }

        static void setBoolean(Object target, String field, boolean value) {
            try {
                target.getClass().getField(field).setBoolean(target, value);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Log.d(TAG, "Could not set " + field + ": " + e);
            }
        }

        static void setInt(Object target, String field, int value) {
            try {
                target.getClass().getField(field).setInt(target, value);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Log.d(TAG, "Could not set " + field + ": " + e);
            }
        }
    }
}