/*
 * Copyright (C) 2013-2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.AppRecommendationsRequest;
import com.google.android.gms.wearable.AppTheme;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.ConnectionDelayConfig;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.Term;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.google.android.gms.wearable.internal.*;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.channel.ChannelManager;
import org.microg.gms.wearable.channel.ChannelStateMachine;
import org.microg.gms.wearable.channel.ChannelStatusCodes;
import org.microg.gms.wearable.channel.ChannelToken;
import org.microg.gms.wearable.channel.InvalidChannelTokenException;
import org.microg.gms.wearable.channel.OpenChannelCallback;
import org.microg.gms.wearable.network.WearableWifiService;
import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.BackupBoolResponse;
import org.microg.gms.wearable.proto.DataSyncTrackingMessage;
import org.microg.gms.wearable.proto.PrivacySettings;
import org.microg.gms.wearable.proto.ProtoDuration;
import org.microg.gms.wearable.proto.AccountConsentRecord;
import org.microg.gms.wearable.proto.BackupErrorResponse;
import org.microg.gms.wearable.proto.EnableBackupRequest;
import org.microg.gms.wearable.proto.EnableBackupSkippedRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "GmsWearSvcImpl";

    private final Context context;
    private final String packageName;
    private final WearableImpl wearable;
    private final Handler mainHandler;
    private final CapabilityManager capabilities;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<String, Integer> restoreStateByNode = new ConcurrentHashMap<>();
    private final Map<String, byte[]> restoreDataByNode = new ConcurrentHashMap<>();
    private static final String CAPABILITY_BACKUP_SETTINGS = "com.google.android.gms.wearable.companion_backup_settings_wear_app";
    private static final long BACKUP_RPC_TIMEOUT_MS = 5_000L;
    private static final long SEND_REQUEST_TIMEOUT_MS = 60_000;

    public static final String DATA_SYNC_PROGRESS_PATH = "DATA_SYNC_PROGRESS";
    private static final long DATA_SYNC_TRACKING_TIMEOUT_MS = 30_000L;

    private static final int WEAR_FEATURE_DISABLED = 4014;

    private static final String CONSENT_PACKAGE = "com.google.android.gms";
    private static final String PATH_PRIVACY_SETTINGS = "/privacy_settings";

    private static final Object CONSENT_LOCK = new Object();

    private static final int TERMS_CONTEXT_UNSUPERVISED = 0;
    private static final int TERMS_CONTEXT_SUPERVISED = 1;

    private static final int TERM_TOS = 0;
    private static final int TERM_LOGGING = 1;
    private static final int TERM_CLOUDSYNC = 2;
    private static final int TERM_LOCATION = 3;
    private static final int TERM_UPDATES = 4;
    private static final int TERM_BACKUP = 5;
    private static final int OPT_IN_TYPE_BACKUP = 4;

    private static final String PATH_ENABLE_BACKUP = "/backup_settings/enable_backup";
    private static final String PATH_ENABLE_BACKUP_SKIPPED = "/backup_settings/enable_backup_skipped";
    private static final int FRAGMENT_COMPANION_TERMS_OF_SERVICE = 16;
    private static final long ENABLE_BACKUP_RPC_TIMEOUT_MS = 15_000L;

    private static final String PREFS_APP_THEMES = "wearable_app_themes";

    private interface StatusSink {
        void onStatus(int statusCode);
    }

    private static final class BackupAction {
        final String nodeId;
        final String accountName;
        final boolean enable;

        BackupAction(String nodeId, String accountName, boolean enable) {
            this.nodeId = nodeId;
            this.accountName = accountName;
            this.enable = enable;
        }
    }

    private interface ConsentOperation {
        void run() throws Exception;
    }

    private WearFastPairManager fastPairManager;

    private synchronized WearFastPairManager getFastPairManager() {
        if (fastPairManager == null) fastPairManager = new WearFastPairManager(context);
        return fastPairManager;
    }

    public WearableServiceImpl(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
        this.capabilities = new CapabilityManager(context, wearable, packageName);
        this.mainHandler = new Handler(context.getMainLooper());
    }

    private AppKey getAppKey() {
        return wearable.getAppKey(packageName);
    }

    private ChannelManager getChannelManager() throws RemoteException {
        ChannelManager cm = wearable.getChannelManager();
        if (cm == null) throw new RemoteException("ChannelManager not yet initialized");
        return cm;
    }

    private void postMain(IWearableCallbacks callbacks, RemoteExceptionRunnable runnable) {
        mainHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                runnable.run();
            }
        });
    }

    private void postNetwork(IWearableCallbacks callbacks, RemoteExceptionRunnable runnable) {
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                runnable.run();
            }
        });
    }

    /*
     * Config
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void putConfig(IWearableCallbacks callbacks, final ConnectionConfiguration config) throws RemoteException {
        config.packageName = this.packageName;
        postMain(callbacks, () -> {
            wearable.createConnection(config);
            wearable.enableConnection(config.name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void deleteConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        postMain(callbacks, () -> {
            wearable.deleteConnection(name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void getConfigs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfigs");
        postMain(callbacks, () -> {
            try {
                callbacks.onGetConfigsResponse(new GetConfigsResponse(0, wearable.getConfigurations()));
            } catch (Exception e) {
                callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
            }
        });
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void enableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        Log.d(TAG, "enableConfig: " + name);
        postMain(callbacks, () -> {
            wearable.enableConnection(name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void disableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        Log.d(TAG, "disableConfig: " + name);
        postMain(callbacks, () -> {
            wearable.disableConnection(name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void updateConnectionStrategy(IWearableCallbacks callbacks, String name, int strategy) throws RemoteException {
        Log.d(TAG, "updateConnectionStrategy: name=" + name + ", strategy=" + strategy);
        
        postMain(callbacks, () -> {
            try {
                ConnectionConfiguration config = wearable.getConfigurationByName(name);
                if (config == null) {
                    Log.w(TAG, "updateConnectionStrategy: no config found with name: " + name);
                    callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                    return;
                }
                wearable.updateConnectionStrategy(config, strategy);
                callbacks.onStatus(Status.SUCCESS);
            } catch (Exception e) {
                Log.e(TAG, "updateConnectionStrategy: exception during processing", e);
                callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
            }
        });
    }

    @Override
    public void getRelatedConfigs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getRelatedConfigs");
        postMain(callbacks, () -> {
            try {
                ConnectionConfiguration[] allConfigs = wearable.getConfigurations();
                List<ConnectionConfiguration> relatedConfigs = new ArrayList<>();
                for (ConnectionConfiguration config : allConfigs) {
                    if (config.packageName == null || config.packageName.equals(packageName)) {
                        relatedConfigs.add(config);
                    }
                }

                callbacks.onGetConfigsResponse(new GetConfigsResponse(0,
                        relatedConfigs.toArray(new ConnectionConfiguration[0])));
            } catch (Exception e) {
                Log.e(TAG, "getRelatedConfigs failed", e);
                callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
            }
        });

    }

    @Override
    public void updateConfig(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        Log.d(TAG, "updateConfig: " + config);

        postMain(callbacks, () -> {
            try {
                if (config == null || config.address == null) {
                    Log.w(TAG, "updateConfig: invalid config");
                    callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                    return;
                }

                ConnectionConfiguration existing = wearable.getConfigurationByAddress(config.address);

                if (existing == null) {
                    Log.w(TAG, "updateConfig: no existing config for address " + config.address);
                    callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                    return;
                }

                ConnectionConfiguration toUpdate = config;
                if (existing.dataItemSyncEnabled && !config.dataItemSyncEnabled) {
                    Log.w(TAG, "updateConfig: disabling dataItemSync not allowed, keeping existing value dataItemSyncEnabled=" + existing.dataItemSyncEnabled);
                    toUpdate = new ConnectionConfiguration(
                            config.name, config.address, config.type, config.role, config.enabled,
                            config.connected, config.peerNodeId, config.btlePriority,
                            config.nodeId, config.packageName, config.connectionRetryStrategy,
                            config.allowedConfigPackages, config.migrating,
                            existing.dataItemSyncEnabled,
                            config.connectionRestrictions,
                            config.removeConnectionWhenBondRemovedByUser,
                            config.connectionDelayFilters,
                            config.maxSupportedRemoteAndroidSdkVersion, config.runtimeType);

                }

                boolean syncNowEnabled = !existing.dataItemSyncEnabled && toUpdate.dataItemSyncEnabled;

                wearable.updateConfiguration(toUpdate);

                if (syncNowEnabled) {
                    String peerId = toUpdate.peerNodeId != null ? toUpdate.peerNodeId : existing.peerNodeId;
                    if (peerId != null) {
                        DataTransport dt = wearable.getDataTransport(peerId);
                        if (dt != null) {
                            Log.d(TAG, "updateConfig: triggering deferred sync for " + peerId);
                            dt.onDataItemSyncEnabled();
                        }
                    }
                }

                callbacks.onStatus(Status.SUCCESS);

            } catch (Exception e) {
                Log.e(TAG, "updateConfig: exception during processing", e);
                try {
                    callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                } catch (RemoteException re) {
                    Log.w(TAG, "Failed to send error status", re);
                }
            }
        });
    }

    /*
     * DataItems
     */

    @Override
    public void putData(IWearableCallbacks callbacks, final PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putData: " + request.toString(true));
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                DataItemRecord record = wearable.putData(request, packageName);
                callbacks.onPutDataResponse(new PutDataResponse(0, record.toParcelable()));
            }
        });
    }

    @Override
    public void getDataItem(IWearableCallbacks callbacks, final Uri uri) throws RemoteException {
        Log.d(TAG, "getDataItem: " + uri);
        postMain(callbacks, () -> {
            DataItemRecord record = wearable.getDataItemByUri(uri, packageName);
            if (record != null) {
                callbacks.onGetDataItemResponse(new GetDataItemResponse(0, record.toParcelable()));
            } else {
                callbacks.onGetDataItemResponse(new GetDataItemResponse(0, null));
            }
        });
    }

    @Override
    public void getDataItems(final IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getDataItems: " + callbacks);
        postMain(callbacks, () -> {
            callbacks.onDataItemChanged(wearable.getDataItemsAsHolder(packageName));
        });
    }

    @Override
    public void getDataItemsByUri(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        getDataItemsByUriWithFilter(callbacks, uri, 0);
    }

    @Override
    public void getDataItemsByUriWithFilter(IWearableCallbacks callbacks, final Uri uri, int typeFilter) throws RemoteException {
        Log.d(TAG, "getDataItemsByUri: " + uri);
        postMain(callbacks, () -> {
            callbacks.onDataItemChanged(wearable.getDataItemsByUriAsHolder(uri, packageName));
        });
    }

    @Override
    public void deleteDataItems(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        deleteDataItemsWithFilter(callbacks, uri, 0);
    }

    @Override
    public void deleteDataItemsWithFilter(IWearableCallbacks callbacks, final Uri uri, int typeFilter) throws RemoteException {
        Log.d(TAG, "deleteDataItems: " + uri);
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                callbacks.onDeleteDataItemsResponse(new DeleteDataItemsResponse(0, wearable.deleteDataItems(uri, packageName)));
            }
        });
    }

    @Override
    public void sendMessage(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data) throws RemoteException {
        sendMessageWithOptions(callbacks, targetNodeId, path, data, new MessageOptions(0));
    }

    @Override
    public void sendMessageWithOptions(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data, MessageOptions options) throws RemoteException {
        Log.d(TAG, "sendMessage: " + targetNodeId + " / " + path + ": " + (data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP)));
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                SendMessageResponse sendMessageResponse = new SendMessageResponse();
                try {
                    sendMessageResponse.requestId = wearable.sendMessage(packageName, targetNodeId, path, data, options);
                    if (sendMessageResponse.requestId == -1) {
                        sendMessageResponse.statusCode = 4000;
                    }
                } catch (Exception e) {
                    sendMessageResponse.statusCode = 8;
                }
                mainHandler.post(() -> {
                    try {
                        callbacks.onSendMessageResponse(sendMessageResponse);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void sendRequest(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data) throws RemoteException {
        sendRequestWithOptions(callbacks, targetNodeId, path, data, new MessageOptions(0));
    }

    @Override
    public void sendRequestWithOptions(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data, MessageOptions options) throws RemoteException {
        Log.d(TAG, "sendRequest: " + targetNodeId + " / " + path + ": " + (data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP)));
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                final int messageId = wearable.sendRequest(packageName, targetNodeId, path, data, options);

                if (messageId == -1) {
                    Log.w(TAG, "sendRequest: no route to " + targetNodeId + " for " + path);
                    mainHandler.post(() -> {
                        try {
                            callbacks.onRpcResponse(new RpcResponse(4004, -1, new byte[0]));
                        } catch (RemoteException e) { e.printStackTrace(); }
                    });
                    return;
                }

                final String routedNode = wearable.resolveToWearableNodeId(targetNodeId);
                wearable.getRpcHelper().addResponseListener(
                        routedNode,
                        messageId,
                        SEND_REQUEST_TIMEOUT_MS,
                        responseData -> mainHandler.post(() -> {
                            try {
                                callbacks.onRpcResponse(new RpcResponse(
                                        0, messageId,
                                        responseData != null ? responseData : new byte[0]));
                            } catch (RemoteException e) { e.printStackTrace(); }
                        }),
                        () -> {
                            Log.w(TAG, "sendRequest timeout: " + path + " node=" + targetNodeId);
                            mainHandler.post(() -> {
                                try {
                                    callbacks.onRpcResponse(new RpcResponse(15, -1, new byte[0]));
                                } catch (RemoteException e) { e.printStackTrace(); }
                            });
                        });
            }
        });
    }

    @Override
    public void getCompanionPackageForNode(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "getCompanionPackageForNode: " + nodeId);

        postMain(callbacks, () -> {
            try {
                if (TextUtils.isEmpty(nodeId)) {
                    Log.e(TAG, "getCompanionPackageForNode: empty nodeId");
                    callbacks.onGetCompanionPackageForNodeResponse(
                            new GetCompanionPackageForNodeResponse(CommonStatusCodes.ERROR, ""));
                    return;
                }

                if ("cloud".equals(nodeId)) {
                    Log.d(TAG, "getCompanionPackageForNode: cloud node has no package");
                    callbacks.onGetCompanionPackageForNodeResponse(
                            new GetCompanionPackageForNodeResponse(CommonStatusCodes.ERROR, ""));
                    return;
                }

                ConnectionConfiguration[] configurations = wearable.getConfigurations();
                if (configurations != null) {
                    for (ConnectionConfiguration config : configurations) {
                        if (nodeId.equals(config.nodeId) || nodeId.equals(config.peerNodeId)) {
                            String packageName = config.packageName != null ? config.packageName : "";
                            Log.d(TAG, "getCompanionPackageForNode: found package " + packageName + " for node " + nodeId);
                            callbacks.onGetCompanionPackageForNodeResponse(
                                    new GetCompanionPackageForNodeResponse(CommonStatusCodes.SUCCESS, packageName));
                            return;
                        }
                    }
                }

                Log.w(TAG, "getCompanionPackageForNode: node " + nodeId + " not found");
                callbacks.onGetCompanionPackageForNodeResponse(
                        new GetCompanionPackageForNodeResponse(CommonStatusCodes.ERROR, ""));

            } catch (Exception e) {
                Log.e(TAG, "getCompanionPackageForNode: exception during processing", e);
                try {
                    callbacks.onGetCompanionPackageForNodeResponse(
                            new GetCompanionPackageForNodeResponse(CommonStatusCodes.INTERNAL_ERROR, ""));
                } catch (RemoteException re) {
                    Log.w(TAG, "Failed to send error response", re);
                }
            }
        });
    }

    @Override
    public void setCloudSyncSettingByNode(IWearableCallbacks callbacks, String s, boolean b) throws RemoteException {
        Log.d(TAG, "unimplemented Method setCloudSyncSettingByNode");

        // dummy
        postMain(callbacks, () -> {
            try {
                callbacks.onStatus(Status.SUCCESS);
            } catch (Exception e) {
                Log.e(TAG, "setCloudSyncSettingByNode: exception during processing", e);
                callbacks.onStatus(Status.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public void getFdForAsset(IWearableCallbacks callbacks, final Asset asset) throws RemoteException {
        Log.d(TAG, "getFdForAsset " + asset);
        postMain(callbacks, () -> {
            // TODO: Access control
            ParcelFileDescriptor pfd = null;
            try {
                String digest = asset != null ? asset.getDigest() : null;
                if (TextUtils.isEmpty(digest)) {
                    callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(4005, null));
                    return;
                }

                File file = wearable.createAssetFile(digest);
                if (!file.isFile() || !file.canRead()) {
                    callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(4005, null));
                    return;
                }

                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(0, pfd));
            } catch (FileNotFoundException e) {
                callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(4005, null));
            } catch (Exception e) {
                Log.e(TAG, "getFdForAsset: exception during processing", e);
                callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(8, null));
            } finally {
                if (pfd != null) {
                    try { pfd.close(); } catch (IOException ignored) {}
                }
            }
        });
    }

    @Override
    public void optInCloudSync(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        Log.d(TAG, "unimplemented Method optInCloudSync");

        // dummy
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    @Deprecated
    public void getCloudSyncOptInDone(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInDone");
        callbacks.onGetCloudSyncOptInOutDoneResponse(new GetCloudSyncOptInOutDoneResponse(0, false));
    }

    @Override
    public void setCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCloudSyncSetting");

        postMain(callbacks, () -> {
            // dummy
            callbacks.onStatus(new Status(0));
        });
    }

    @Override
    public void getCloudSyncSetting(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncSetting");
        // disabled by default
        callbacks.onGetCloudSyncSettingResponse(new GetCloudSyncSettingResponse(0, false));
    }

    @Override
    public void getCloudSyncOptInStatus(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInStatus");
        // opt out by default
        callbacks.onGetCloudSyncOptInStatusResponse(new GetCloudSyncOptInStatusResponse(0, false, true));
    }

    @Override
    public void sendAmsRemoteCommand(IWearableCallbacks callbacks, byte command) throws RemoteException {
        Log.d(TAG, "unimplemented Method sendAmsRemoteCommand: " + command);

        postMain(callbacks, () -> {
            // return error, because we dont have AMS handling
            callbacks.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
        });
    }

    private void postConsentOperation(IWearableCallbacks callbacks, String name, ConsentOperation operation) {
        wearable.networkHandler.post(() -> {
            int code = CommonStatusCodes.SUCCESS;
            try {
                synchronized (CONSENT_LOCK) {
                    operation.run();
                }
            } catch (Exception e) {
                Log.w(TAG, name + " failed", e);
                code = CommonStatusCodes.ERROR;
            }
            try {
                callbacks.onStatus(new Status(code));
            } catch (RemoteException e) {
                Log.w(TAG, name + ": onStatus failed", e);
            }
        });
    }

    private void postConsentResponse(IWearableCallbacks callbacks, String nodeId) {
        wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                ConsentResponse response;
                try {
                    synchronized (CONSENT_LOCK) {
                        response = toConsentResponse(getPrivacySettings(nodeId));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getConsentStatus exception", e);
                    response = new ConsentResponse(CommonStatusCodes.ERROR, false, false, false, false, null, null, null);
                }
                callbacks.onConsentResponse(response);
            }
        });
    }

    private static String consentPath(String nodeId) {
        return TextUtils.isEmpty(nodeId) ? PATH_PRIVACY_SETTINGS : PATH_PRIVACY_SETTINGS + "/" + nodeId;
    }

    private PrivacySettings readPrivacySettings(String nodeId) {
        Uri uri = new Uri.Builder().scheme("wear").authority("").path(consentPath(nodeId)).build();
        DataItemRecord record = wearable.getDataItemByUri(uri, CONSENT_PACKAGE);
        if (record == null || record.deleted || record.dataItem == null || record.dataItem.data == null) {
            Log.d(TAG, "Consent data item does not exist for " + (nodeId == null ? "global" : nodeId));
            return null;
        }
        try {
            return PrivacySettings.ADAPTER.decode(record.dataItem.data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse consent record from dataitem", e);
        }
    }

    private PrivacySettings getPrivacySettings(String nodeId) {
        if (!TextUtils.isEmpty(nodeId)) {
            PrivacySettings perWatch = readPrivacySettings(nodeId);
            if (perWatch != null) return perWatch;
        }
        return readPrivacySettings(null);
    }

    private void writePrivacySettings(PrivacySettings.Builder builder, String nodeId) {
        PrivacySettings partial = builder.build();
        builder.loggingConsent(Boolean.TRUE.equals(partial.loggingConsent));
        builder.cloudSyncConsent(Boolean.TRUE.equals(partial.cloudSyncConsent));
        builder.locationConsent(Boolean.TRUE.equals(partial.locationConsent));

        long now = System.currentTimeMillis();
        builder.lastUpdateRequested(new ProtoDuration.Builder()
                .seconds(now / 1000)
                .nanos((int) ((now % 1000) * 1_000_000))
                .build());
        if (!TextUtils.isEmpty(nodeId)) builder.nodeId(nodeId);
        putPrivacySettings(builder.build(), nodeId);
    }
    private void putPrivacySettings(PrivacySettings settings, String nodeId) {
        Log.d(TAG, "Saving wearable consent to record " + consentPath(nodeId));
        wearable.putData(PutDataRequest.create(consentPath(nodeId))
                .setData(PrivacySettings.ADAPTER.encode(settings)), CONSENT_PACKAGE);
    }

    private static ConsentResponse toConsentResponse(PrivacySettings settings) {
        if (settings == null) {
            return new ConsentResponse(0, false, false, false, false, null, null, null);
        }
        Long lastUpdate = null;
        if (settings.lastUpdateRequested != null) {
            long seconds = settings.lastUpdateRequested.seconds != null ? settings.lastUpdateRequested.seconds : 0L;
            int nanos = settings.lastUpdateRequested.nanos != null ? settings.lastUpdateRequested.nanos : 0;
            lastUpdate = seconds * 1000L + nanos / 1_000_000;
        }
        List<AccountConsentRecordParcelable> accounts = null;
        if (!settings.accountConsents.isEmpty()) {
            accounts = new ArrayList<>();
            for (AccountConsentRecord record : settings.accountConsents) {
                accounts.add(new AccountConsentRecordParcelable(
                        record.accountName, Boolean.TRUE.equals(record.consentGranted)));
            }
        }
        return new ConsentResponse(
                0,
                true,
                Boolean.TRUE.equals(settings.loggingConsent),
                Boolean.TRUE.equals(settings.cloudSyncConsent),
                Boolean.TRUE.equals(settings.locationConsent),
                accounts,
                TextUtils.isEmpty(settings.nodeId) ? null : settings.nodeId,
                lastUpdate);
    }

    private static PrivacySettings.Builder builderFrom(PrivacySettings base) {
        return base != null ? base.newBuilder() : new PrivacySettings.Builder();
    }

    private static boolean isOptInTermType(int termType) {
        return termType == TERM_LOGGING || termType == TERM_CLOUDSYNC || termType == TERM_LOCATION;
    }

    private static void setOptIn(PrivacySettings.Builder builder, int termType, boolean value) {
        switch (termType) {
            case TERM_LOGGING:
                builder.loggingConsent(value);
                break;
            case TERM_CLOUDSYNC:
                builder.cloudSyncConsent(value);
                break;
            case TERM_LOCATION:
                builder.locationConsent(value);
                break;
            default:
                throw new IllegalArgumentException("Not an opt-in term type: " + termType);
        }
    }

    private static int[] getTermTypes(int termsContext) {
        switch (termsContext) {
            case TERMS_CONTEXT_UNSUPERVISED:
                return new int[]{TERM_TOS, TERM_LOCATION, TERM_LOGGING, TERM_BACKUP, TERM_UPDATES, TERM_CLOUDSYNC};
            case TERMS_CONTEXT_SUPERVISED:
                return new int[]{TERM_TOS, TERM_LOCATION, TERM_LOGGING, TERM_UPDATES};
            default:
                return null;
        }
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }

    private static int[] requireTermTypes(int termsContext, String parentGaiaId, String childGaiaId) {
        if (termsContext == TERMS_CONTEXT_SUPERVISED
                && (TextUtils.isEmpty(parentGaiaId) || TextUtils.isEmpty(childGaiaId))) {
            throw new IllegalStateException("Consent requires parent and child Gaia Id: " + termsContext);
        }
        int[] termTypes = getTermTypes(termsContext);
        if (termTypes == null) {
            throw new IllegalArgumentException("Invalid TermsContext " + termsContext);
        }
        return termTypes;
    }

    private BackupAction doAcceptTerms(AcceptTermsRequest request) {
        int[] valid = requireTermTypes(request.termsContext, request.parentGaiaId, request.childGaiaId);
        List<?> accepted = request.acceptedTermTypes;
        if (accepted == null) accepted = Collections.emptyList();
        for (Object type : accepted) {
            if (!(type instanceof Integer) || !contains(valid, (Integer) type)) {
                throw new IllegalStateException("Accepted terms contains invalid term type for termsContext " + request.termsContext);
            }
        }

        String consentNodeId = null;
        if (request.perWatchConsents) {
            if (TextUtils.isEmpty(request.nodeId)) {
                throw new IllegalStateException("Node ID not provided for consents per watch.");
            }
            consentNodeId = request.nodeId;
        }

        PrivacySettings.Builder builder = builderFrom(getPrivacySettings(consentNodeId));
        for (int type : new int[]{TERM_LOGGING, TERM_LOCATION, TERM_CLOUDSYNC}) {
            if (contains(valid, type)) setOptIn(builder, type, accepted.contains(type));
        }
        writePrivacySettings(builder, consentNodeId);

        if (accepted.contains(TERM_BACKUP)) {
            if (TextUtils.isEmpty(request.nodeId) || TextUtils.isEmpty(request.accountName)) {
                Log.w(TAG, "acceptTerms called with TermType.BACKUP but missing nodeId or accountName.");
                return null;
            }
            return new BackupAction(request.nodeId, request.accountName, true);
        }
        List<?> skipped = request.skippedTermTypes;
        if (skipped != null && skipped.contains(TERM_BACKUP) && !TextUtils.isEmpty(request.nodeId)) {
            return new BackupAction(request.nodeId, null, false);
        }
        return null;
    }

    private void doRecordTermConsent(RecordTermConsentRequest request) {
        int[] valid = requireTermTypes(request.termsContext, request.parentGaiaId, request.childGaiaId);
        if (!contains(valid, request.termType)) {
            throw new IllegalStateException("Invalid termType " + request.termType + " for termsContext " + request.termsContext);
        }
        if (!isOptInTermType(request.termType)) {
            throw new IllegalStateException("Invalid term type for recordTermConsent");
        }
        updateOptIn(request.accountId, request.termType, request.consentGranted);
    }

    private void updateOptIn(String nodeId, int termType, boolean value) {
        PrivacySettings current = getPrivacySettings(nodeId);
        if (current == null) {
            Log.w(TAG, "updateOptIn: no existing consent record, creating one");
        }
        PrivacySettings.Builder builder = builderFrom(current);
        setOptIn(builder, termType, value);
        writePrivacySettings(builder, nodeId);
    }

    @Override
    public void getConsentStatus(IWearableCallbacks callbacks) throws RemoteException {
        postConsentResponse(callbacks, null);
    }

    @Override
    public void addAccountToConsent(IWearableCallbacks callbacks, AddAccountToConsentRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method addAccountToConsent: "
                + "account=" + request.accountName
                + ", consent=" + request.consentGranted);
        postMain(callbacks, () -> callbacks.onStatus(Status.SUCCESS));
    }

    @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
    private void doRecordSwaadlOptIn() {
        PrivacySettings current = readPrivacySettings(null);
        if (current == null) throw new IllegalStateException("Consent record not available");
        List<AccountConsentRecord> records = new ArrayList<>();
        for (Account account : AccountManager.get(context).getAccountsByType("com.google")) {
            records.add(new AccountConsentRecord.Builder()
                    .accountName(account.name)
                    .consentGranted(false)
                    .build());
        }
        Log.d(TAG, "Writing sWAADL consent for " + records.size() + " account(s)");
        putPrivacySettings(current.newBuilder().accountConsents(records).build(), null);
    }

    private void sendBackupRequest(BackupAction action, StatusSink sink) {
        String path;
        byte[] payload;
        if (action.enable) {
            path = PATH_ENABLE_BACKUP;
            payload = EnableBackupRequest.ADAPTER.encode(new EnableBackupRequest.Builder()
                    .accountName(action.accountName).inSetup(true).flag(false).build());
        } else {
            path = PATH_ENABLE_BACKUP_SKIPPED;
            payload = EnableBackupSkippedRequest.ADAPTER.encode(new EnableBackupSkippedRequest.Builder()
                    .fragmentType(FRAGMENT_COMPANION_TERMS_OF_SERVICE).flag(false).build());
        }
        Log.d(TAG, "sendBackupRequest: " + path + " node=" + action.nodeId);
        int messageId = wearable.sendRequest(packageName, action.nodeId, path, payload, new MessageOptions(0));
        if (messageId < 0) {
            Log.w(TAG, "sendBackupRequest: sendRequest failed for " + path);
            sink.onStatus(CommonStatusCodes.ERROR);
            return;
        }
        wearable.getRpcHelper().addResponseListener(action.nodeId, messageId, ENABLE_BACKUP_RPC_TIMEOUT_MS,
                data -> {
                    int code = CommonStatusCodes.SUCCESS;
                    if (data != null && data.length > 0) {
                        try {
                            BackupErrorResponse response = BackupErrorResponse.ADAPTER.decode(data);
                            if (response.errorCode != null) {
                                Log.w(TAG, path + " failed on the watch, error=" + response.errorCode);
                                code = CommonStatusCodes.ERROR;
                            }
                        } catch (IOException e) {
                            Log.w(TAG, path + ": failed to decode response", e);
                            code = CommonStatusCodes.ERROR;
                        }
                    }
                    sink.onStatus(code);
                },
                () -> {
                    Log.w(TAG, path + ": RPC timeout for node " + action.nodeId);
                    sink.onStatus(CommonStatusCodes.ERROR);
                });
    }

    private void postStatus(IWearableCallbacks callbacks, String name, int statusCode) {
        mainHandler.post(() -> {
            try {
                callbacks.onStatus(new Status(statusCode));
            } catch (RemoteException e) {
                Log.w(TAG, name + ": onStatus failed", e);
            }
        });
    }

    private GetTermsResponse buildTermsResponse(int termsContext) {
        List<WearableTerms.TermDef> defs = WearableTerms.forContext(termsContext);
        if (defs == null) {
            Log.w(TAG, "No terms available for " + termsContext);
            return new GetTermsResponse(CommonStatusCodes.INTERNAL_ERROR, Collections.<Term>emptyList());
        }
        List<Term> terms = new ArrayList<>();
        for (WearableTerms.TermDef def : defs) {
            terms.add(new Term(def.termType, def.getDescription(context), def.explicit,
                    def.getTitle(context), null, def.getOptInType()));
        }
        return new GetTermsResponse(CommonStatusCodes.SUCCESS, terms);
    }

    @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
    @Override
    public void acceptTerms(IWearableCallbacks callbacks, AcceptTermsRequest request) throws RemoteException {
        Log.d(TAG, "acceptTerms: context=" + request.termsContext + ", accepted=" + request.acceptedTermTypes
                + ", skipped=" + request.skippedTermTypes + ", node=" + request.nodeId
                + ", perWatch=" + request.perWatchConsents);
        wearable.networkHandler.post(() -> {
            BackupAction backup;
            try {
                synchronized (CONSENT_LOCK) {
                    backup = doAcceptTerms(request);
                }
            } catch (Exception e) {
                Log.w(TAG, "acceptTerms failed", e);
                postStatus(callbacks, "acceptTerms", CommonStatusCodes.ERROR);
                return;
            }
            try {
                synchronized (CONSENT_LOCK) {
                    doRecordSwaadlOptIn();
                }
            } catch (Exception e) {
                Log.w(TAG, "acceptTerms: recordSwaadlOptIn failed", e);
            }
            if (backup == null) {
                postStatus(callbacks, "acceptTerms", CommonStatusCodes.SUCCESS);
            } else {
                sendBackupRequest(backup, code -> postStatus(callbacks, "acceptTerms", code));
            }
        });
    }

    @Override
    public void recordTermConsent(IWearableCallbacks callbacks, RecordTermConsentRequest request) throws RemoteException {
        Log.d(TAG, "recordTermConsent: context=" + request.termsContext + ", type=" + request.termType
                + ", granted=" + request.consentGranted);
        postConsentOperation(callbacks, "recordTermConsent", () -> doRecordTermConsent(request));
    }

    @Override
    public void getTerms(IWearableCallbacks callbacks, int termsContext) throws RemoteException {
        Log.d(TAG, "getTerms: " + termsContext);
        postMain(callbacks, () -> callbacks.onGetTermsResponse(buildTermsResponse(termsContext)));
    }

    @Override
    public void getConsentStatusForRequest(IWearableCallbacks callbacks, ConsentStatusRequest request) throws RemoteException {
        Log.d(TAG, "getConsentStatusForRequest: node=" + request.status);
        postConsentResponse(callbacks, request.status);
    }

    @Override
    public void recordSwaadlOptIn(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "recordSwaadlOptIn");
        postConsentOperation(callbacks, "recordSwaadlOptIn", this::doRecordSwaadlOptIn);
    }

    @Override
    public void someBoolUnknown(IWearableCallbacks callbacks) throws RemoteException {
        // not sure what it is, no-op in gms
        postMain(callbacks, () -> {
            try {
                callbacks.onBooleanResponse(new BooleanResponse(0, true));
            } catch (Exception e) {
                callbacks.onBooleanResponse(new BooleanResponse(8, false));
            }
        });
    }

    @Override
    public void logCounter(IWearableCallbacks callbacks, LogCounterRequest request) throws RemoteException {
        Log.d(TAG, "logCounter: " + (request == null ? "null"
                : request.counterName + ", value=" + request.value + ", increment=" + request.increment));
        postMain(callbacks, () -> {
            if (request == null || TextUtils.isEmpty(request.counterName)) {
                callbacks.onStatus(new Status(CommonStatusCodes.DEVELOPER_ERROR));
                return;
            }
            WearableLogStore.INSTANCE.counter(request.counterName, request.value, request.increment);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void logEvent(IWearableCallbacks callbacks, LogEventRequest request) throws RemoteException {
        Log.d(TAG, "logEvent: data length=" + (request != null && request.eventData != null ? request.eventData.length : 0));
        postMain(callbacks, () -> {
            if (request == null) {
                callbacks.onStatus(new Status(CommonStatusCodes.DEVELOPER_ERROR));
                return;
            }
            WearableLogStore.INSTANCE.event(request.eventData != null ? request.eventData.length : 0);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void logTimer(IWearableCallbacks callbacks, LogTimerRequest request) throws RemoteException {
        Log.d(TAG, "logTimer: " + (request == null ? "null" : request.timerName + ", timestamp=" + request.timestamp));
        postMain(callbacks, () -> {
            if (request == null || TextUtils.isEmpty(request.timerName)) {
                callbacks.onStatus(new Status(CommonStatusCodes.DEVELOPER_ERROR));
                return;
            }
            WearableLogStore.INSTANCE.timer(request.timerName, request.timestamp);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void clearLogs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "clearLogs");
        postMain(callbacks, () -> {
            WearableLogStore.INSTANCE.clear();
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void getBackupSettingsSupported(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "getBackupSettingsSupported: nodeId=" + nodeId);
        postMain(callbacks, () -> {
            Set<String> nodes = capabilities.getNodesForCapability(
                    CAPABILITY_BACKUP_SETTINGS);
            if (nodes.contains(nodeId)) {
                Log.d(TAG, "getBackupSettingsSupported: capability found for " + nodeId);
                callbacks.onGetBackupSettingsSupportedResponse(
                        new GetBackupSettingsSupportedResponse(CommonStatusCodes.SUCCESS, true));
                return;
            }

            Log.d(TAG, "getBackupSettingsSupported: no capability, trying RPC for " + nodeId);
            wearable.networkHandler.post(new CallbackRunnable(callbacks) {
                @Override
                public void run(IWearableCallbacks cb) throws RemoteException {
                    int messageId = wearable.sendRequest(packageName, nodeId,
                            "/backup_settings/backup_supported", null, new MessageOptions(0));
                    if (messageId < 0) {
                        Log.w(TAG, "getBackupSettingsSupported: sendRequest failed");
                        mainHandler.post(() -> {
                            try {
                                cb.onGetBackupSettingsSupportedResponse(
                                        new GetBackupSettingsSupportedResponse(
                                                CommonStatusCodes.ERROR, false));
                            } catch (RemoteException ignored) {
                            }
                        });
                        return;
                    }
                    wearable.getRpcHelper().addResponseListener(nodeId, messageId,
                            BACKUP_RPC_TIMEOUT_MS, responseData -> {
                                boolean supported = false;
                                try {
                                    supported = Boolean.TRUE.equals(
                                            BackupBoolResponse.ADAPTER.decode(responseData).value);
                                } catch (IOException e) {
                                    Log.w(TAG, "parseProtoBool: failed to decode response", e);
                                }
                                boolean finalSupported = supported;
                                mainHandler.post(() -> {
                                    try {
                                        cb.onGetBackupSettingsSupportedResponse(
                                                new GetBackupSettingsSupportedResponse(
                                                        CommonStatusCodes.SUCCESS, finalSupported));
                                    } catch (RemoteException ignored) {
                                    }
                                });
                    }, () -> {
                        Log.w(TAG, "getBackupSettingsSupported: RPC timeout for " + nodeId);
                        mainHandler.post(() -> {
                            try {
                                cb.onGetBackupSettingsSupportedResponse(
                                        new GetBackupSettingsSupportedResponse(
                                                CommonStatusCodes.ERROR, false));
                            } catch (RemoteException ignored) {
                            }
                        });
                    });
                }
            });
        });
    }

    @Override
    public void getRestoreSupported(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getRestoreSupported");
        postMain(callbacks, () ->
                callbacks.onGetRestoreSupportedResponse(
                        new GetRestoreSupportedResponse(CommonStatusCodes.SUCCESS, true)));
    }

    @Override
    public void startRestoreSession(IWearableCallbacks callbacks, StartRestoreSessionRequest request) throws RemoteException {
        Log.d(TAG, "startRestoreSession: nodeId=" + request.nodeId);
        postMain(callbacks, () -> {
            int reqId = wearable.sendMessage(packageName, request.nodeId,
                    "/restore/restore_finished", null, new MessageOptions(0));
            if (reqId < 0) {
                Log.w(TAG, "startRestoreSession: sendMessage failed for node " + request.nodeId);
            }
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void saveRestoreState(IWearableCallbacks callbacks, SaveRestoreStateRequest request) throws RemoteException {
        Log.d(TAG, "saveRestoreState: nodeId=" + request.nodeId + " state=" + request.state);
        postMain(callbacks, () -> {
            if (request.nodeId == null || request.data == null) {
                Log.w(TAG, "saveRestoreState: null nodeId or data");
                callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                return;
            }
            restoreStateByNode.put(request.nodeId, request.state);
            restoreDataByNode.put(request.nodeId, request.data);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void getRestoreState(IWearableCallbacks callbacks, GetRestoreStateRequest request) throws RemoteException {
        Log.d(TAG, "getRestoreState: nodeId=" + request.nodeId);
        postMain(callbacks, () -> {
            Integer state = restoreStateByNode.get(request.nodeId);
            byte[] data = restoreDataByNode.get(request.nodeId);
            if (state == null || data == null) {
                Log.w(TAG, "getRestoreState: no saved state for node " + request.nodeId);
                callbacks.onGetRestoreStateResponse(
                        new GetRestoreStateResponse(CommonStatusCodes.ERROR, 0, new byte[0]));
                return;
            }
            callbacks.onGetRestoreStateResponse(
                    new GetRestoreStateResponse(CommonStatusCodes.SUCCESS, state, data));
        });
    }

    @Override
    public void getBackupEnabled(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "getBackupEnabled: nodeId=" + nodeId);
        wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks cb) throws RemoteException {
                int messageId = wearable.sendRequest(packageName, nodeId,
                        "/backup_settings/backup_enabled",
                        null, new MessageOptions(0));
                if (messageId < 0) {
                    Log.w(TAG, "getBackupSettingsSupported: sendRequest failed");
                    mainHandler.post(() -> {
                        try {
                            cb.onGetBackupSettingsSupportedResponse(
                                    new GetBackupSettingsSupportedResponse(
                                            CommonStatusCodes.ERROR, false));
                        } catch (RemoteException ignored) {
                        }
                    });
                    return;
                }
                wearable.getRpcHelper().addResponseListener(nodeId, messageId,
                        BACKUP_RPC_TIMEOUT_MS, responseData -> {
                            boolean enabled = false;
                            try {
                                enabled = Boolean.TRUE.equals(
                                        BackupBoolResponse.ADAPTER.decode(responseData).value);
                            } catch (IOException e) {
                                Log.w(TAG, "parseProtoBool: failed to decode response", e);
                            }
                            boolean finalEnabled = enabled;
                            mainHandler.post(() -> {
                                try {
                                    cb.onBooleanResponse(
                                            new BooleanResponse(CommonStatusCodes.SUCCESS, finalEnabled));
                                } catch (RemoteException ignored) {
                                }
                            });
                }, () -> {
                    Log.w(TAG, "getBackupEnabled: RPC timeout for node " + nodeId);
                    mainHandler.post(() -> {
                        try {
                            cb.onBooleanResponse(
                                    new BooleanResponse(CommonStatusCodes.ERROR, false));
                        } catch (RemoteException ignored) {
                        }
                    });
                });
            }
        });
    }

    @Override
    public void dataSynchronizationProgressTracking(IWearableCallbacks callbacks, String peerNodeId) throws RemoteException {
        Log.d(TAG, "dataSynchronizationProgressTracking: peer=" + peerNodeId);

        if (TextUtils.isEmpty(peerNodeId)) {
            callbacks.onStatus(new Status(CommonStatusCodes.ERROR, "peerNodeId cannot be empty"));
            return;
        }

        wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                String trackerId = UUID.randomUUID().toString();
                long targetSeqId = wearable.getNodeDatabase().getCurrentSeqId(wearable.getLocalNodeId());
                Log.d(TAG, "dataSynchronizationProgressTracking: trackerId=" + trackerId
                        + " targetSeqId=" + targetSeqId + " peer=" + peerNodeId);
                DataSyncTrackingMessage request = new DataSyncTrackingMessage.Builder()
                        .trackerId(trackerId).sequenceId(targetSeqId).build();
                byte[] payload = DataSyncTrackingMessage.ADAPTER.encode(request);

                wearable.getRpcHelper().addDataSyncListener(peerNodeId, trackerId, DATA_SYNC_TRACKING_TIMEOUT_MS,
                        reachedSeqId -> {
                            Log.d(TAG, "dataSynchronizationProgressTracking: peer=" + peerNodeId
                                    + " confirmed seqId=" + reachedSeqId);
                            mainHandler.post(() -> {
                                try {
                                    callbacks.onStatus(Status.SUCCESS);
                                } catch (RemoteException ignored) {}
                            });
                        }, () -> {
                            Log.w(TAG, "dataSynchronizationProgressTracking: timeout for peer=" + peerNodeId);
                            mainHandler.post(() -> {
                                try {
                                    callbacks.onStatus(new Status(CommonStatusCodes.TIMEOUT));
                                } catch (RemoteException ignored) {}
                            });
                        });
                int reqId = wearable.sendMessage(packageName, peerNodeId, DATA_SYNC_PROGRESS_PATH,
                        payload, new MessageOptions(0));
                if (reqId < 0) {
                    wearable.getRpcHelper().cancelDataSyncListener(peerNodeId, trackerId);
                    Log.w(TAG, "dataSynchronizationProgressTracking: sendMessage failed");
                    mainHandler.post(() -> {
                        try {
                            callbacks.onStatus(new Status(CommonStatusCodes.ERROR));
                        } catch (RemoteException ignored) {}
                    });
                }
            }
        });
    }

    private static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final int APPTYPE_USIM = 2;
    private static final int AUTHTYPE_EAP_AKA = 129;
    private static final String EAP_IDENTITY_PREFIX = "0";

    private TelephonyManager getTelephonyManager(int subscriptionId) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            return tm.createForSubscriptionId(subscriptionId);
        }
        return tm;
    }

    private static String iccAuthentication(TelephonyManager tm, String challenge) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            return tm.getIccAuthentication(APPTYPE_USIM, TelephonyManager.AUTHTYPE_EAP_AKA, challenge);
        }
        try {
            Method m = TelephonyManager.class.getMethod("getIccAuthentication", int.class, int.class, String.class);
            return (String) m.invoke(tm, APPTYPE_USIM, AUTHTYPE_EAP_AKA, challenge);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SecurityException) throw (SecurityException) e.getCause();
            return null;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    private static String buildEapIdentity(String imsi, String simOperator) {
        if (TextUtils.isEmpty(imsi) || TextUtils.isEmpty(simOperator) || simOperator.length() < 5) {
            return null;
        }
        String mcc = simOperator.substring(0, 3);
        String mnc = simOperator.substring(3);
        if (mnc.length() == 2) mnc = "0" + mnc;
        return EAP_IDENTITY_PREFIX + imsi + "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
    }

    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @Override
    public void getEapId(IWearableCallbacks callbacks, int subscriptionId) throws RemoteException {
        Log.d(TAG, "getEapId: subscriptionId=" + subscriptionId);
        postMain(callbacks, () -> {
            if (subscriptionId == INVALID_SUBSCRIPTION_ID) {
                callbacks.onGetEapIdResponse(new GetEapIdResponse(CommonStatusCodes.DEVELOPER_ERROR, ""));
                return;
            }
            String eapId = null;
            try {
                TelephonyManager tm = getTelephonyManager(subscriptionId);
                if (tm != null) eapId = buildEapIdentity(tm.getSubscriberId(), tm.getSimOperator());
            } catch (SecurityException e) {
                Log.w(TAG, "getEapId: missing phone state permission", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "getEapId failed", e);
            }
            callbacks.onGetEapIdResponse(TextUtils.isEmpty(eapId)
                    ? new GetEapIdResponse(CommonStatusCodes.ERROR, "")
                    : new GetEapIdResponse(CommonStatusCodes.SUCCESS, eapId));
        });
    }

    @Override
    public void performEapAka(IWearableCallbacks callbacks, int subscriptionId, String challenge) throws RemoteException {
        Log.d(TAG, "performEapAka: subscriptionId=" + subscriptionId);
        postMain(callbacks, () -> {
            if (subscriptionId == INVALID_SUBSCRIPTION_ID || TextUtils.isEmpty(challenge)) {
                callbacks.onPerformEapAkaResponse(new PerformEapAkaResponse(CommonStatusCodes.DEVELOPER_ERROR, ""));
                return;
            }
            try {
                Base64.decode(challenge, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                callbacks.onPerformEapAkaResponse(new PerformEapAkaResponse(CommonStatusCodes.DEVELOPER_ERROR, ""));
                return;
            }

            String result = null;
            try {
                TelephonyManager tm = getTelephonyManager(subscriptionId);
                if (tm != null) result = iccAuthentication(tm, challenge);
            } catch (SecurityException e) {
                Log.w(TAG, "performEapAka: missing privileged phone state permission", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "performEapAka failed", e);
            }

            callbacks.onPerformEapAkaResponse(TextUtils.isEmpty(result)
                    ? new PerformEapAkaResponse(CommonStatusCodes.ERROR, "")
                    : new PerformEapAkaResponse(CommonStatusCodes.SUCCESS, result));
        });
    }

    @Override
    public void associateDeviceAndAccountWithFastPair(IWearableCallbacks callbacks, String s1, Account account, String s2, String s3) throws RemoteException {
        Log.d(TAG, "associateDeviceAndAccountWithFastPair: " + packageName + " " + s3 + " " + s2 + " " + s1);
        postMain(callbacks, () -> {
            if (account == null || TextUtils.isEmpty(account.name)) {
                callbacks.onStatus(new Status(CommonStatusCodes.DEVELOPER_ERROR));
                return;
            }
            try {
                getFastPairManager().getOrCreateAccountKey(account.name);
                callbacks.onStatus(Status.SUCCESS);
            } catch (Exception e) {
                Log.e(TAG, "associateDeviceAndAccountWithFastPair failed", e);
                callbacks.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });

    }

    @Override
    public void getFastpairAccountKeys(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getFastpairAccountKeys");
        postMain(callbacks, () -> {
            try {
                List<FastPairAccountKeyParcelable> keys = new ArrayList<>();
                for (WearFastPairManager.AccountKeyRecord r : getFastPairManager().getAccountKeys()) {
                    keys.add(new FastPairAccountKeyParcelable(r.accountKey));
                }
                callbacks.onGetFastpairAccountKeysResponse(
                        new GetFastpairAccountKeysResponse(CommonStatusCodes.SUCCESS, keys));
            } catch (Exception e) {
                Log.e(TAG, "getFastpairAccountKeys failed", e);
                callbacks.onGetFastpairAccountKeysResponse(
                        new GetFastpairAccountKeysResponse(CommonStatusCodes.INTERNAL_ERROR, null));
            }
        });
    }

    @Override
    public void getFastpairAccountKeyByAccount(IWearableCallbacks callbacks, Account account) throws RemoteException {
        Log.d(TAG, "getFastpairAccountKeyByAccount: " + (account == null ? "null" : account.name));

        String[] pkgs = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (pkgs == null || !java.util.Arrays.asList(pkgs).contains(packageName)) {
            throw new SecurityException(String.format("Package [%s] is not authorized", packageName));
        }

        postMain(callbacks, () -> {
            if (account == null || TextUtils.isEmpty(account.name)) {
                callbacks.onGetFastpairAccountKeyByAccountResponse(
                        new GetFastpairAccountKeyByAccountResponse(CommonStatusCodes.DEVELOPER_ERROR, null));
                return;
            }
            try {
                WearFastPairManager.AccountKeyRecord r = getFastPairManager().getAccountKey(account.name);
                callbacks.onGetFastpairAccountKeyByAccountResponse(new GetFastpairAccountKeyByAccountResponse(
                        CommonStatusCodes.SUCCESS,
                        r == null ? null : new FastPairAccountKeyParcelable(r.accountKey)));
            } catch (Exception e) {
                Log.e(TAG, "getFastpairAccountKeyByAccount failed", e);
                callbacks.onGetFastpairAccountKeyByAccountResponse(
                        new GetFastpairAccountKeyByAccountResponse(CommonStatusCodes.INTERNAL_ERROR, null));
            }
        });

    }

    @Override
    public void getAppRecommendations(IWearableCallbacks callbacks, AppRecommendationsRequest request) throws RemoteException {
        Log.d(TAG, "getAppRecommendations: " + request);
        postMain(callbacks, () -> callbacks.onAppRecommendationsResponse(
                new AppRecommendationsResponse(CommonStatusCodes.CANCELED)));
    }

    @Override
    public void setThemeForApp(IWearableCallbacks callbacks, AppTheme theme) throws RemoteException {
        Log.d(TAG, "setThemeForApp: " + theme);
        postNetwork(callbacks, () -> {
            int status;
            try {
                if (theme == null) throw new IllegalArgumentException("theme is null");
                int color = inRange(theme.colorTheme, 3);
                int dynamic = inRange(theme.dynamicColor, 2);
                int align = inRange(theme.screenAlignment, 2);
                int size = inRange(theme.screenItemsSize, 3);
                context.getSharedPreferences(PREFS_APP_THEMES, Context.MODE_PRIVATE).edit()
                        .putString(this.packageName, color + "," + dynamic + "," + align + "," + size)
                        .commit();
                status = CommonStatusCodes.SUCCESS;
            } catch (RuntimeException e) {
                Log.w(TAG, "setThemeForApp failed", e);
                status = CommonStatusCodes.INTERNAL_ERROR;
            }
            callbacks.onStatus(new Status(status));
        });
    }

    @Override
    public void getThemeForApp(IWearableCallbacks callbacks, String targetPackage) throws RemoteException {
        Log.d(TAG, "getThemeForApp: " + targetPackage);
        postNetwork(callbacks, () -> {
            int status;
            AppTheme result = null;
            try {
                if (TextUtils.isEmpty(targetPackage)) throw new IllegalArgumentException("empty package");
                result = loadAppTheme(targetPackage);
                status = CommonStatusCodes.SUCCESS;
            } catch (RuntimeException e) {
                Log.w(TAG, "getThemeForApp failed", e);
                status = CommonStatusCodes.INTERNAL_ERROR;
            }
            callbacks.onGetAppThemeResponse(new GetAppThemeResponse(status, result));
        });
    }

    private static int inRange(int v, int max) { return (v < 0 || v > max) ? 0 : v; }

    private AppTheme loadAppTheme(String pkg) {
        int[] v = new int[4];
        String raw = context.getSharedPreferences(PREFS_APP_THEMES, Context.MODE_PRIVATE).getString(pkg, null);
        if (raw != null) {
            String[] p = raw.split(",");
            try {
                if (p.length == 4) for (int i = 0; i < 4; i++) v[i] = Integer.parseInt(p[i]);
            } catch (NumberFormatException e) {
                v = new int[4];
            }
        }
        // defaults from gms: SYSTEM, dynamic color enabled, START alignment, LARGE items
        return new AppTheme(v[0] == 0 ? 1 : v[0], v[1] == 0 ? 1 : v[1], v[2] == 0 ? 1 : v[2], v[3] == 0 ? 3 : v[3]);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void retryConnection(IWearableCallbacks callbacks, String nodeId, boolean force) throws RemoteException {
        Log.d(TAG, "retryConnection: nodeId=" + nodeId + ", force=" + force);
        postMain(callbacks, () -> {
            if (TextUtils.isEmpty(nodeId)) {
                Log.w(TAG, "retryConnection: empty nodeId");
                callbacks.onStatus(new Status(WearableStatusCodes.INVALID_TARGET_NODE));
                return;
            }
            boolean ok = wearable.retryConnection(nodeId, force);
            callbacks.onStatus(ok ? Status.SUCCESS
                    : new Status(WearableStatusCodes.TARGET_NODE_NOT_CONNECTED));
        });
    }

    @Override
    public void cancelMigration(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        Log.d(TAG, "cancelMigration: " + config);
        postMain(callbacks, () -> {
            boolean ok = wearable.cancelNodeMigration(config);
            callbacks.onStatus(ok ? Status.SUCCESS : new Status(CommonStatusCodes.ERROR));
        });
    }

    @Override
    public void isNodeConnectionMetered(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "isNodeConnectionMetered: " + nodeId);
        postMain(callbacks, () -> {
            callbacks.onBooleanResponse(new BooleanResponse(CommonStatusCodes.SUCCESS, false));
        });
    }

    @Override
    public void setConnectionDelayConfig(IWearableCallbacks callbacks, ConnectionDelayConfig config) throws RemoteException {
        Log.d(TAG, "setConnectionDelayConfig: " + config);
        postMain(callbacks, () -> callbacks.onStatus(new Status(WEAR_FEATURE_DISABLED)));
    }

    @Override
    public void connectionDelayUnknown101(IWearableCallbacks callbacks, String s) throws RemoteException {
        Log.d(TAG, "connectionDelayUnknown101: " + s);
        postMain(callbacks, () -> callbacks.onStatus(new Status(WEAR_FEATURE_DISABLED)));
    }

    @Override
    public void clearConnectionDelayConfig(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "clearConnectionDelayConfig: " + nodeId);
        postMain(callbacks, () -> callbacks.onStatus(new Status(WEAR_FEATURE_DISABLED)));
    }

    @Override
    public void addSupervisedAccount(IWearableCallbacks callbacks, AddSupervisedAccountRequest request) throws RemoteException {
        Log.d(TAG, "addSupervisedAccount");
        postMain(callbacks, () -> callbacks.onStatus(new Status(WEAR_FEATURE_DISABLED)));
    }

    @Override
    public void recordUntetheredSupervisedAccountTransfer(IWearableCallbacks callbacks, RecordUntetheredSupervisedAccountTransferRequest request) throws RemoteException {
        Log.d(TAG, "recordUntetheredSupervisedAccountTransfer");
        postMain(callbacks, () -> callbacks.onStatus(new Status(WEAR_FEATURE_DISABLED)));
    }

    @Override
    public void getLocalNode(IWearableCallbacks callbacks) throws RemoteException {
        postMain(callbacks, () -> {
            try {
                callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(0, new NodeParcelable(wearable.getLocalNodeId(), Build.MODEL)));
            } catch (Exception e) {
                callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(8, null));
            }
        });
    }

    @Override
    public void getNodeId(IWearableCallbacks callbacks, String address) throws RemoteException {
        postNetwork(callbacks, () -> {
            String resultNode;
            ConnectionConfiguration configuration = wearable.getConfigurationByAddress(address);
            try {
                if (address == null || configuration == null || configuration.type == 4 || !address.equals(configuration.address)) {
                    resultNode = null;
                } else {
                    resultNode = configuration.peerNodeId;
                }

                if (resultNode != null)
                    callbacks.onGetNodeIdResponse(new GetNodeIdResponse(0, resultNode));
                else
                    callbacks.onGetNodeIdResponse(new GetNodeIdResponse(13, null));

            } catch (Exception e) {
                callbacks.onGetNodeIdResponse(new GetNodeIdResponse(8, null));
            }
        });
    }

    @Override
    public void getConnectedNodes(IWearableCallbacks callbacks) throws RemoteException {
        postMain(callbacks, () -> {
            callbacks.onGetConnectedNodesResponse(new GetConnectedNodesResponse(0, wearable.getConnectedNodesParcelableList()));
        });
    }

    /*
     * Capability
     */

    @Override
    public void getConnectedCapability(IWearableCallbacks callbacks, String capability, int nodeFilter) throws RemoteException {
        Log.d(TAG, "getConnectedCapability: " + capability + ", nodeFilter=" + nodeFilter);
        postMain(callbacks, () -> {
            try {
                List<NodeParcelable> nodes = new ArrayList<>();
                Set<String> nodeIds = capabilities.getNodesForCapability(capability);
                final Set<String> reachable = reachableNodeIds();

                for (String nodeId : nodeIds) {
                    if (shouldIncludeNode(nodeId, nodeFilter, reachable)) {
                        ConnectionConfiguration cc = wearable.getConfigurationByNodeId(nodeId);
                        if (cc == null) cc = wearable.getConfigurationByPeerNodeId(nodeId);
                        String dispName = (cc != null && cc.name != null) ? cc.name : nodeId;
                        nodes.add(new NodeParcelable(nodeId, dispName));
                    }
                }

                CapabilityInfoParcelable capabilityInfo = new CapabilityInfoParcelable(capability, nodes);
                callbacks.onGetCapabilityResponse(new GetCapabilityResponse(0, capabilityInfo));
            } catch (Exception e) {
                Log.e(TAG, "getConnectedCapability failed", e);
                callbacks.onGetCapabilityResponse(new GetCapabilityResponse(13, null));
            }
        });
    }

    @Override
    public void getAllCapabilities(IWearableCallbacks callbacks, int nodeFilter) throws RemoteException {
        Log.d(TAG, "getAllCapabilities: nodeFilter=" + nodeFilter);
        postMain(callbacks, () -> {
            try {
                Map<String, CapabilityInfoParcelable> capabilitiesMap = new HashMap<>();
                final Set<String> reachable = reachableNodeIds();

                DataHolder dataHolder = wearable.getDataItemsByUriAsHolder(
                        Uri.parse("wear:/capabilities/"), packageName
                );

                try {
                    Set<String> processedCapabilities = new HashSet<>();

                    for (int i = 0; i < dataHolder.getCount(); i++) {
                        String uri = dataHolder.getString("path", i, 0);
                        if (uri != null && uri.startsWith("/capabilities/")) {
                            String[] segments = uri.split("/");
                            if (segments.length >= 4) {
                                String capabilityName = Uri.decode(segments[segments.length - 1]);
                                if (!processedCapabilities.contains(capabilityName)) {
                                    processedCapabilities.add(capabilityName);

                                    List<NodeParcelable> nodes = new ArrayList<>();
                                    Set<String> nodeIds = capabilities.getNodesForCapability(capabilityName);

                                    for (String nodeId: nodeIds) {
                                        if (shouldIncludeNode(nodeId, nodeFilter, reachable)){
                                            ConnectionConfiguration cc = wearable.getConfigurationByNodeId(nodeId);
                                            if (cc == null) cc = wearable.getConfigurationByPeerNodeId(nodeId);
                                            String dispName = (cc != null && cc.name != null) ? cc.name : nodeId;
                                            nodes.add(new NodeParcelable(nodeId, dispName));
                                        }
                                    }

                                    if (!nodes.isEmpty() || nodeFilter == 0) {
                                        capabilitiesMap.put(capabilityName, new CapabilityInfoParcelable(capabilityName, nodes));
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    dataHolder.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "getAllCapabilities failed", e);
                callbacks.onGetAllCapabilitiesResponse(new GetAllCapabilitiesResponse(13, new ArrayList<>()));
            }
        });
    }

    private Set<String> reachableNodeIds() {
        Set<String> ids = new HashSet<>();
        String local = wearable.getLocalNodeId();
        if (local != null) ids.add(local);
        for (NodeParcelable n : wearable.getConnectedNodesParcelableList())
            ids.add(n.getId());

        return ids;
    }

    private boolean shouldIncludeNode(String nodeId, int nodeFilter, Set<String> reachable) {
        switch (nodeFilter) {
            case 0:
                return true;
            case 1:
            case 2:
                return reachable.contains(nodeId);
            default:
                Log.w(TAG, "Unknown node filter: " + nodeFilter + ", including all nodes");
                return true;
        }
    }

    @Override
    public void addLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        Log.d(TAG, "addLocalCapability: " + capability);

        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                try {
                    int statusCode = capabilities.add(capability);
                    callbacks.onAddLocalCapabilityResponse(new AddLocalCapabilityResponse(statusCode));

                    if (statusCode == 0) {
                        Log.d(TAG, "Successfully added local capability: " + capability);
                    } else {
                        Log.w(TAG, "Failed to add local capability: " + capability + ", status=" + statusCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "addLocalCapability exception", e);
                    callbacks.onAddLocalCapabilityResponse(new AddLocalCapabilityResponse(8));
                }
            }
        });
    }

    @Override
    public void removeLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        Log.d(TAG, "removeLocalCapability: " + capability);

        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                try {
                    int statusCode = capabilities.remove(capability);
                    callbacks.onRemoveLocalCapabilityResponse(new RemoveLocalCapabilityResponse(statusCode));

                    if (statusCode == 0) {
                        Log.d(TAG, "Successfully removed local capability: " + capability);
                    } else {
                        Log.w(TAG, "Failed to remove local capability: " + capability + ", status=" + statusCode);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "removeLocalCapability exception", e);
                    callbacks.onRemoveLocalCapabilityResponse(new RemoveLocalCapabilityResponse(8));
                }
            }
        });
    }

    @Override
    public void addListener(IWearableCallbacks callbacks, AddListenerRequest request) throws RemoteException {
        if (request.listener != null) {
            wearable.addListener(packageName, request.listener, request.intentFilters);
        }
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void removeListener(IWearableCallbacks callbacks, RemoveListenerRequest request) throws RemoteException {
        wearable.removeListener(request.listener);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getStorageInformation(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getStorageInformation");
        postMain(callbacks, () -> {
            try {
                NodeDatabaseHelper nodeDatabase = wearable.getNodeDatabase();
                PackageManager packageManager = context.getPackageManager();
                SQLiteDatabase db = nodeDatabase.getReadableDatabase();

                File databasePath = context.getDatabasePath("node.db");
                long totalDatabaseSize = databasePath != null ? databasePath.length() : 0L;

                Map<String, PackageStorageInfo> packageInfoMap = new HashMap<>();

                Map<String, String> packageIdToName = new HashMap<>();
                Cursor appKeysCursor = db.query("appkeys", new String[]{"_id", "packageName"},
                        null, null, null, null, null);

                while (appKeysCursor.moveToNext()) {
                    String id = appKeysCursor.getString(0);
                    String packageName = appKeysCursor.getString(1);
                    packageIdToName.put(id, packageName);
                }
                appKeysCursor.close();

                for (Map.Entry<String, String> entry : packageIdToName.entrySet()) {
                    String appKeyId = entry.getKey();
                    String packageName = entry.getValue();

                    long dataItemsSize = getTableSizeForAppKey(db, "dataitems", "appkeys_id", appKeyId);

                    long assetsSize = getAssetsSizeForPackage(db, nodeDatabase, appKeyId);

                    String appLabel = packageName;
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                        CharSequence label = packageManager.getApplicationLabel(appInfo);
                        if (label != null) {
                            appLabel = label.toString();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Package not found: " + packageName);
                    }

                    long totalSize = dataItemsSize + assetsSize;

                    if (totalSize > 0) {
                        PackageStorageInfo info = new PackageStorageInfo(
                                packageName,
                                appLabel,
                                totalSize
                        );
                        packageInfoMap.put(packageName, info);
                    }
                }

                List<PackageStorageInfo> packageInfoList = new ArrayList<>(packageInfoMap.values());
                PackageStorageInfo[] packageInfoArray = packageInfoList.toArray(
                        new PackageStorageInfo[packageInfoList.size()]);

                StorageInfoResponse response = new StorageInfoResponse(
                        CommonStatusCodes.SUCCESS,
                        totalDatabaseSize,
                        packageInfoArray
                );

                Log.d(TAG, "getStorageInformation: total db size=" + totalDatabaseSize +
                        ", packages=" + packageInfoArray.length);
                callbacks.onStorageInfoResponse(response);

            } catch (Exception e) {
                Log.e(TAG, "getStorageInformation: exception during processing", e);
                try {
                    callbacks.onStorageInfoResponse(new StorageInfoResponse(
                            CommonStatusCodes.INTERNAL_ERROR, 0L, new PackageStorageInfo[0]));
                } catch (RemoteException re) {
                    Log.w(TAG, "Failed to send error response", re);
                }
            }
        });
    }

    @Override
    public void privacyRecordOptinRequest(IWearableCallbacks callbacks,
                                            PrivacyRecordOptinRequest request)
            throws RemoteException {
        Log.d(TAG, "PrivacyRecordOptinRequest: type=" + request.optInType
                + " optedIn=" + request.optedIn + " node=" + request.nodeId);
        // types: 1=LOGGING, 2=CLOUDSYNC, 3=LOCATION, 4=BACKUP
        if (request.optInType < 1 || request.optInType > 4) {
            Log.e(TAG, "onPrivacyRecordOptinRequest: invalid optInType " + request.optInType);
            postMain(callbacks, () -> callbacks.onStatus(new Status(CommonStatusCodes.ERROR)));
            return;
        }
        if (request.optInType == OPT_IN_TYPE_BACKUP) {
            postMain(callbacks, () -> callbacks.onStatus(Status.SUCCESS));
            return;
        }
        postConsentOperation(callbacks, "recordOptIn",
                () -> updateOptIn(request.nodeId, request.optInType, request.optedIn));
    }

    private long getTableSizeForAppKey(SQLiteDatabase db, String tableName,
                                       String keyColumn, String appKeyId) {
        long totalSize = 0;

        Cursor cursor = db.query(tableName, null, keyColumn + "=?",
                new String[]{appKeyId}, null, null, null);

        int rowCount = cursor.getCount();
        cursor.close();

        totalSize = rowCount * 1024L;

        return totalSize;
    }

    private long getAssetsSizeForPackage(SQLiteDatabase db, NodeDatabaseHelper nodeDatabase,
                                         String appKeyId) {
        long totalSize = 0;

        try {
            Set<String> assetDigests = new HashSet<>();
            Cursor aclCursor = db.query("assetsacls", new String[]{"assets_digest"},
                    "appkeys_id=?", new String[]{appKeyId}, null, null, null);

            while (aclCursor.moveToNext()) {
                String digest = aclCursor.getString(0);
                assetDigests.add(digest);
            }
            aclCursor.close();

            for (String digest : assetDigests) {
                File assetFile = new File(context.getFilesDir(), "assets/" + digest);
                if (assetFile.exists()) {
                    totalSize += assetFile.length();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error calculating assets size", e);
        }

        return totalSize;
    }

    @Override
    public void clearStorage(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "clearStorage");

        postMain(callbacks, () -> {
            try {
                Log.d(TAG, "clearStorage: starting storage clear");

                NodeDatabaseHelper nodeDatabase = wearable.getNodeDatabase();
                SQLiteDatabase db = nodeDatabase.getWritableDatabase();

                db.execSQL("DELETE FROM dataitems");
                db.execSQL("DELETE FROM assets");
                db.execSQL("DELETE FROM assetrefs");
                db.execSQL("DELETE FROM assetsacls");
                db.execSQL("DELETE FROM nodeinfo");
                db.execSQL("DELETE FROM appkeys");
                db.execSQL("DELETE FROM archiveDataItems");
                db.execSQL("DELETE FROM archiveAssetRefs");

                Log.d(TAG, "clearStorage: database tables cleared");

                File assetsDir = new File(context.getFilesDir(), "assets");
                if (assetsDir.exists() && assetsDir.isDirectory()) {
                    File[] assetFiles = assetsDir.listFiles();
                    if (assetFiles != null) {
                        for (File file : assetFiles) {
                            if (file.isFile()) {
                                file.delete();
                            }
                        }
                    }
                }
                Log.d(TAG, "clearStorage: asset files cleared");

                ClockworkNodePreferences prefs = wearable.getClockworkNodePreferences();
                if (prefs != null) {
                    prefs.clear();
                }
                Log.d(TAG, "clearStorage: preferences cleared");

                callbacks.onStatus(Status.SUCCESS);

                Log.d(TAG, "clearStorage: complete");

            } catch (Exception e) {
                Log.e(TAG, "clearStorage: exception during clearing storage", e);
                try {
                    callbacks.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
                } catch (RemoteException re) {
                    Log.w(TAG, "Failed to send error response", re);
                }
            }
        });

    }

    @Override
    public void endCall(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: endCall");
    }

    @Override
    public void acceptRingingCall(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: acceptRingingCall");
    }

    @Override
    public void silenceRinger(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: silenceRinger");
    }

    /*
     * Apple Notification Center Service
     */

    @Override
    public void injectAncsNotificationForTesting(IWearableCallbacks callbacks, AncsNotificationParcelable notification) throws RemoteException {
        Log.d(TAG, "unimplemented Method: injectAncsNotificationForTesting: " + notification);
    }

    @Override
    public void doAncsPositiveAction(IWearableCallbacks callbacks, int i) throws RemoteException {
        Log.d(TAG, "unimplemented Method: doAncsPositiveAction: " + i);
    }

    @Override
    public void doAncsNegativeAction(IWearableCallbacks callbacks, int i) throws RemoteException {
        Log.d(TAG, "unimplemented Method: doAncsNegativeAction: " + i);
    }

    /*
     * Channels
     */

    @Override
    public void openChannel(IWearableCallbacks callbacks, String nodeId, String path) throws RemoteException {
        Log.d(TAG, "openChannel; " + nodeId + ", " + path);
        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            Log.w(TAG, "openChannel: ChannelManager not initialized");
            callbacks.onOpenChannelResponse(new OpenChannelResponse(ChannelStatusCodes.INTERNAL_ERROR, null));
            return;
        }

        try {
            if (nodeId == null || nodeId.isEmpty()) {
                Log.w(TAG, "openChannel: nodeId is null or empty");
                callbacks.onOpenChannelResponse(new OpenChannelResponse(ChannelStatusCodes.INVALID_ARGUMENT, null));
                return;
            }

            if (path == null || path.isEmpty()) {
                Log.w(TAG, "openChannel: path is null or empty");
                callbacks.onOpenChannelResponse(new OpenChannelResponse(ChannelStatusCodes.INVALID_ARGUMENT, null));
                return;
            }

            AppKey appKey = getAppKey();
            boolean isReliable = true;
            OpenChannelCallback openCallback = (statusCode, token, path1) -> {
                try {
                    if (statusCode == ChannelStatusCodes.SUCCESS && token != null) {
                        callbacks.onOpenChannelResponse(new OpenChannelResponse(statusCode, token.toParcelable(path1)));
                    } else {
                        callbacks.onOpenChannelResponse(new OpenChannelResponse(statusCode, null));
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to send openChannel result", e);
                }
            };

            channelManager.openChannel(appKey, nodeId, path, isReliable, openCallback);
        } catch (Exception e) {
            Log.w(TAG, "openChannel: exception during processing", e);
            callbacks.onOpenChannelResponse(new OpenChannelResponse(ChannelStatusCodes.INTERNAL_ERROR, null));
        }
    }

    @Override
    public void closeChannel(IWearableCallbacks callbacks, String s) throws RemoteException {
        closeChannelWithError(callbacks, s, 0);
    }

    @Override
    public void closeChannelWithError(IWearableCallbacks callbacks, String channelToken, int errorCode) throws RemoteException {
        Log.d(TAG, "closeChannelWithError:" + channelToken + ", " + errorCode);

        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            callbacks.onCloseChannelResponse(new CloseChannelResponse(ChannelStatusCodes.INTERNAL_ERROR));
            return;
        }

        try {
            ChannelToken token = ChannelToken.fromString(getAppKey(), channelToken);
            ChannelStateMachine channel = channelManager.getChannel(token);

            if (channel == null) {
                callbacks.onCloseChannelResponse(new CloseChannelResponse(ChannelStatusCodes.CHANNEL_NOT_FOUND));
                return;
            }

            channelManager.closeChannel(token, errorCode);
            callbacks.onCloseChannelResponse(new CloseChannelResponse(ChannelStatusCodes.SUCCESS));

        } catch (InvalidChannelTokenException e) {
            Log.w(TAG, "closeChannelWithError: invalid token", e);
            callbacks.onCloseChannelResponse(new CloseChannelResponse(ChannelStatusCodes.INVALID_ARGUMENT));
        } catch (Exception e) {
            Log.w(TAG, "closeChannelWithError: exception", e);
            callbacks.onCloseChannelResponse(new CloseChannelResponse(ChannelStatusCodes.INTERNAL_ERROR));
        }

    }

    @Override
    public void getChannelInputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String channelToken) throws RemoteException {
        Log.d(TAG, "getChannelInputStream: " + channelToken);

        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
            return;
        }

        try {
            ChannelToken token = ChannelToken.fromString(getAppKey(), channelToken);
            ChannelStateMachine channel = channelManager.getChannel(token);

            if (channel == null) {
                ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.CHANNEL_NOT_FOUND);
                return;
            }

            if (channel.hasInputStream()) {
                ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.ALREADY_IN_PROGRESS);
                return;
            }

            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor readEnd = pipe[0];
            ParcelFileDescriptor writeEnd = pipe[1];

            channel.setInputStream(writeEnd, channelCallbacks);

            callbacks.onGetChannelInputStreamResponse(
                    new GetChannelInputStreamResponse(ChannelStatusCodes.SUCCESS, readEnd));

            readEnd.close();
        } catch (InvalidChannelTokenException e) {
            Log.w(TAG, "getChannelInputStream: invalid token", e);
            ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.INVALID_ARGUMENT);
        } catch (IOException e) {
            Log.w(TAG, "getChannelInputStream: IO exception", e);
            ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.w(TAG, "getChannelInputStream: exception", e);
            ChannelManager.getInputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void getChannelOutputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String channelToken) throws RemoteException {
        Log.d(TAG, "getChannelOutputStream: " + channelToken);

        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
            return;
        }

        try {
            ChannelToken token = ChannelToken.fromString(getAppKey(), channelToken);
            ChannelStateMachine channel = channelManager.getChannel(token);

            if (channel == null) {
                ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.CHANNEL_NOT_FOUND);
                return;
            }

            if (channel.hasOutputStream()) {
                ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.ALREADY_IN_PROGRESS);
                return;
            }

            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor readEnd = pipe[0];
            ParcelFileDescriptor writeEnd = pipe[1];

            channel.setOutputStream(readEnd, channelCallbacks, 0, -1);

            callbacks.onGetChannelOutputStreamResponse(
                    new GetChannelOutputStreamResponse(ChannelStatusCodes.SUCCESS, writeEnd));

            writeEnd.close();

        } catch (InvalidChannelTokenException e) {
            Log.w(TAG, "getChannelOutputStream: invalid token", e);
            ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.INVALID_ARGUMENT);
        } catch (IOException e) {
            Log.w(TAG, "getChannelOutputStream: IO exception", e);
            ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.w(TAG, "getChannelOutputStream: exception", e);
            ChannelManager.getOutputStreamError(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        }

    }

    @Override
    public void writeChannelInputToFd(IWearableCallbacks callbacks, String channelToken, ParcelFileDescriptor fd) throws RemoteException {
        Log.d(TAG, "writeChannelInputToFd: " + channelToken);

        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            ChannelManager.receiveFileResult(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
            return;
        }

        try {
            ChannelToken token = ChannelToken.fromString(getAppKey(), channelToken);
            ChannelStateMachine channel = channelManager.getChannel(token);

            if (channel == null) {
                ChannelManager.receiveFileResult(callbacks, ChannelStatusCodes.CHANNEL_NOT_FOUND);
                return;
            }

            if (channel.hasInputStream()) {
                ChannelManager.receiveFileResult(callbacks, ChannelStatusCodes.ALREADY_IN_PROGRESS);
                return;
            }

            channel.setInputStream(fd, new ReceiveFileStreamCallback(callbacks));

        } catch (InvalidChannelTokenException e) {
            Log.w(TAG, "writeChannelInputToFd: invalid token", e);
            ChannelManager.receiveFileResult(callbacks, ChannelStatusCodes.INVALID_ARGUMENT);
        } catch (Exception e) {
            Log.w(TAG, "writeChannelInputToFd: exception", e);
            ChannelManager.receiveFileResult(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        }

    }

    @Override
    public void readChannelOutputFromFd(IWearableCallbacks callbacks, String channelToken, ParcelFileDescriptor fd, long startOffset, long length) throws RemoteException {
        Log.d(TAG, "unimplemented Method: readChannelOutputFromFd: " + channelToken + ", " + startOffset + ", " + length);

        ChannelManager channelManager = getChannelManager();
        if (channelManager == null) {
            ChannelManager.sendFileResult(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
            return;
        }

        try {
            ChannelToken token = ChannelToken.fromString(getAppKey(), channelToken);
            ChannelStateMachine channel = channelManager.getChannel(token);

            if (channel == null) {
                ChannelManager.sendFileResult(callbacks, ChannelStatusCodes.CHANNEL_NOT_FOUND);
                return;
            }

            if (channel.hasOutputStream()) {
                ChannelManager.sendFileResult(callbacks, ChannelStatusCodes.ALREADY_IN_PROGRESS);
                return;
            }

            channel.setOutputStream(fd, new SendFileStreamCallback(callbacks), startOffset, length);

        } catch (InvalidChannelTokenException e) {
            Log.w(TAG, "readChannelOutputFromFd: invalid token", e);
            ChannelManager.sendFileResult(callbacks, ChannelStatusCodes.INVALID_ARGUMENT);
        } catch (Exception e) {
            Log.w(TAG, "readChannelOutputFromFd: exception", e);
            ChannelManager.sendFileResult(callbacks, ChannelStatusCodes.INTERNAL_ERROR);
        }

    }

    private static class ReceiveFileStreamCallback extends IChannelStreamCallbacks.Stub {
        private final IWearableCallbacks callbacks;

        ReceiveFileStreamCallback(IWearableCallbacks callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public void onChannelClosed(int closeReason, int errorCode) throws RemoteException {
            int statusCode = (closeReason == ChannelStatusCodes.CLOSE_REASON_NORMAL)
                    ? ChannelStatusCodes.SUCCESS : closeReason;
            ChannelManager.receiveFileResult(callbacks, statusCode);
        }
    }

    private static class SendFileStreamCallback extends IChannelStreamCallbacks.Stub {
        private final IWearableCallbacks callbacks;

        SendFileStreamCallback(IWearableCallbacks callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public void onChannelClosed(int closeReason, int errorCode) throws RemoteException {
            int statusCode = (closeReason == ChannelStatusCodes.CLOSE_REASON_NORMAL)
                    ? ChannelStatusCodes.SUCCESS : closeReason;
            ChannelManager.sendFileResult(callbacks, statusCode);
        }
    }

    private static void deliver(IWearableCallbacks callbacks, Status status) {
        try {
            callbacks.onStatus(status);
        } catch (RemoteException e) {
            Log.d(TAG, "Failed to deliver result to app", e);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    public void syncWifiCredentials(IWearableCallbacks callbacks) throws RemoteException {
        postNetwork(callbacks, () -> {
            WearableWifiService service = wearable.getWearWifiService();
            if (service == null) {
                Log.e(TAG, "syncWifiCredentials: wifiService is null!");
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
                return;
            }
            try {
                deliver(callbacks, service.syncCredentials(true)
                        ? Status.SUCCESS
                        : new Status(WearableStatusCodes.WIFI_CREDENTIAL_SYNC_NO_CREDENTIAL_FETCHED));
            } catch (RuntimeException e) {
                Log.e(TAG, "syncWifiCredentials: exception during processing", e);
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    public void syncWifiCredentialWithSsid(IWearableCallbacks callbacks, String nodeId, String ssid) throws RemoteException {
        postNetwork(callbacks, () -> {
            WearableWifiService service = wearable.getWearWifiService();
            if (service == null) {
                Log.e(TAG, "syncWifiCredential: wifiService is null!");
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
                return;
            }
            try {
                service.syncCredentialToNode(nodeId, ssid, status -> deliver(callbacks, status));
            } catch (RuntimeException e) {
                Log.e(TAG, "syncWifiCredential: exception during processing", e);
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    public void syncWifiCredentialForNode(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        postNetwork(callbacks, () -> {
            WearableWifiService service = wearable.getWearWifiService();
            if (service == null) {
                Log.e(TAG, "syncConnectedWifiCredential: wifiService is null!");
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
                return;
            }
            try {
                service.syncConnectedCredentialToNode(nodeId, status -> deliver(callbacks, status));
            } catch (RuntimeException e) {
                Log.e(TAG, "syncConnectedWifiCredential: exception during processing", e);
                deliver(callbacks, new Status(CommonStatusCodes.INTERNAL_ERROR));
            }
        });
    }

    /*
     * Connection deprecated
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    @Deprecated
    public void putConnection(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        Log.d(TAG, "putConnection: " + config.name);
        config.packageName = this.packageName;
        postMain(callbacks, () -> {
            wearable.createConnection(config);
            wearable.enableConnection(config.name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    @Deprecated
    public void getConnection(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfig");
        postMain(callbacks, () -> {
            ConnectionConfiguration[] configurations = wearable.getConfigurations();
            if (configurations == null || configurations.length == 0) {
                callbacks.onGetConfigResponse(new GetConfigResponse(1, new ConnectionConfiguration(null, null, 0, 0, false)));
            } else {
                callbacks.onGetConfigResponse(new GetConfigResponse(0, configurations[0]));
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    @Deprecated
    public void enableConnection(IWearableCallbacks callbacks) throws RemoteException {
        postMain(callbacks, () -> {
            ConnectionConfiguration[] configurations = wearable.getConfigurations();
            if (configurations.length > 0) {
                enableConfig(callbacks, configurations[0].name);
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    @Deprecated
    public void disableConnection(IWearableCallbacks callbacks) throws RemoteException {
        postMain(callbacks, () -> {
            ConnectionConfiguration[] configurations = wearable.getConfigurations();
            if (configurations.length > 0) {
                disableConfig(callbacks, configurations[0].name);
            }
        });
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }

    public abstract class CallbackRunnable implements Runnable {
        private IWearableCallbacks callbacks;

        public CallbackRunnable(IWearableCallbacks callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public void run() {
            try {
                run(callbacks);
            } catch (RemoteException e) {
                mainHandler.post(() -> {
                    try {
                        callbacks.onStatus(Status.CANCELED);
                    } catch (RemoteException e2) {
                        Log.w(TAG, e);
                    }
                });
            }
        }

        public abstract void run(IWearableCallbacks callbacks) throws RemoteException;
    }

    public interface RemoteExceptionRunnable {
        void run() throws RemoteException;
    }
}
