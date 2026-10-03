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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.MessageOptions;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.wearable.consent.WearableConsentStore;
import com.google.android.gms.wearable.internal.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "GmsWearSvcImpl";

    private final Context context;
    private final String packageName;
    private final WearableImpl wearable;
    private final Handler mainHandler;
    private final CapabilityManager capabilities;

    @Override
    public void sendRequest(IWearableCallbacks callbacks, String node, String path, byte[] data) throws RemoteException {
        sendRequestWithOptions(callbacks, node, path, data, new MessageOptions());
    }

    @Override
    public void sendRequestWithOptions(IWearableCallbacks callbacks, String node, String path, byte[] data,
                                       MessageOptions options) throws RemoteException {
        // This must run on the Binder thread, before asynchronous dispatch loses the calling UID.
        PackageUtils.getAndCheckCallingPackage(context, packageName);
        wearable.requests.send(packageName, node, path, data, options, callbacks);
    }

    public WearableServiceImpl(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
        this.capabilities = new CapabilityManager(context, wearable, packageName);
        this.mainHandler = new Handler(context.getMainLooper());
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

    @Override
    public void putConfig(IWearableCallbacks callbacks, final ConnectionConfiguration config) throws RemoteException {
        ConnectionConfiguration snapshot = copyConfiguration(config);
        postConfiguration(callbacks, () -> wearable.createConnection(snapshot));
    }

    @Override
    public void updateConfig(IWearableCallbacks callbacks, final ConnectionConfiguration config) throws RemoteException {
        ConnectionConfiguration snapshot = copyConfiguration(config);
        postConfiguration(callbacks, () -> wearable.updateConnection(snapshot));
    }

    @Override
    public void deleteConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        postConfiguration(callbacks, () -> wearable.deleteConnection(name));
    }

    @Override
    public void getConfigs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfigs");
        boolean manager = isConfigurationManager();
        postMain(callbacks, () -> {
            try {
                callbacks.onGetConfigsResponse(new GetConfigsResponse(0, forCaller(wearable.getConfigurations(), manager)));
            } catch (Exception e) {
                callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
            }
        });
    }


    @Override
    public void enableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        postConfiguration(callbacks, () -> wearable.enableConnection(name));
    }

    @Override
    public void disableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        postConfiguration(callbacks, () -> wearable.disableConnection(name));
    }

    private static ConnectionConfiguration copyConfiguration(ConnectionConfiguration config) {
        if (config == null) return null;
        Parcel parcel = Parcel.obtain();
        try {
            config.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            ConnectionConfiguration copy = ConnectionConfiguration.CREATOR.createFromParcel(parcel);
            copy.hasUnsupportedConnectionPolicies |= config.hasUnsupportedConnectionPolicies;
            return copy;
        } finally {
            parcel.recycle();
        }
    }

    /** Must run on the Binder thread, which carries the caller's UID. */
    private boolean isConfigurationManager() {
        try {
            ConfigurationCaller.capture(context, packageName);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** Only the verified companion sees Bluetooth addresses, which identify the paired watch. */
    private static ConnectionConfiguration[] forCaller(ConnectionConfiguration[] configurations, boolean manager) {
        if (manager || configurations == null) return configurations;
        ConnectionConfiguration[] redacted = new ConnectionConfiguration[configurations.length];
        for (int i = 0; i < configurations.length; i++) {
            ConnectionConfiguration config = configurations[i];
            ConnectionConfiguration copy = new ConnectionConfiguration(config.name, null, config.type,
                    config.role, config.enabled, config.nodeId);
            copy.connected = config.connected;
            copy.peerNodeId = config.peerNodeId;
            redacted[i] = copy;
        }
        return redacted;
    }

    private void postConfiguration(IWearableCallbacks callbacks, Runnable operation) throws RemoteException {
        if (callbacks == null) throw new IllegalArgumentException("Missing configuration callback");
        final ConfigurationCaller caller;
        try {
            caller = ConfigurationCaller.capture(context, packageName);
        } catch (SecurityException e) {
            callbacks.onStatus(new Status(CommonStatusCodes.DEVELOPER_ERROR));
            return;
        }
        boolean posted = mainHandler.post(() -> {
            int status = CommonStatusCodes.SUCCESS;
            try {
                caller.enforceInstalled(context);
                operation.run();
            } catch (SecurityException | IllegalArgumentException e) {
                status = CommonStatusCodes.DEVELOPER_ERROR;
            } catch (RuntimeException e) {
                status = CommonStatusCodes.INTERNAL_ERROR;
            }
            try {
                callbacks.onStatus(new Status(status));
            } catch (RemoteException ignored) {
                // The callback is terminal; a dead client must not trigger a second delivery.
            }
        });
        if (!posted) callbacks.onStatus(new Status(CommonStatusCodes.INTERNAL_ERROR));
    }

    /*
     * DataItems
     */

    @Override
    public void putData(IWearableCallbacks callbacks, final PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putData");
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
            try {
                callbacks.onDataItemChanged(wearable.getDataItemsByUriAsHolder(uri, packageName, typeFilter));
            } catch (IllegalArgumentException e) {
                callbacks.onDataItemChanged(DataHolder.empty(CommonStatusCodes.DEVELOPER_ERROR));
            }
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
                try {
                    callbacks.onDeleteDataItemsResponse(new DeleteDataItemsResponse(0, wearable.deleteDataItems(uri, packageName, typeFilter)));
                } catch (IllegalArgumentException e) {
                    callbacks.onDeleteDataItemsResponse(new DeleteDataItemsResponse(CommonStatusCodes.DEVELOPER_ERROR, 0));
                }
            }
        });
    }

    @Override
    public void sendMessage(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data) throws RemoteException {
        Log.d(TAG, "sendMessage: " + targetNodeId + " / " + path);
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                SendMessageResponse sendMessageResponse = new SendMessageResponse();
                try {
                    sendMessageResponse.requestId = wearable.sendMessage(packageName, targetNodeId, path, data);
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
    public void getFdForAsset(IWearableCallbacks callbacks, final Asset asset) throws RemoteException {
        Log.d(TAG, "getFdForAsset " + asset);
        postMain(callbacks, () -> {
            // TODO: Access control
            try {
                callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(0, ParcelFileDescriptor.open(wearable.createAssetFile(asset.getDigest()), ParcelFileDescriptor.MODE_READ_ONLY)));
            } catch (FileNotFoundException e) {
                callbacks.onGetFdForAssetResponse(new GetFdForAssetResponse(8, null));
            }
        });
    }

    @Override
    public void optInCloudSync(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        completeCloudSyncSetting(callbacks, enable);
    }

    @Override
    @Deprecated
    public void getCloudSyncOptInDone(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInDone");
    }

    @Override
    public void setCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        completeCloudSyncSetting(callbacks, enable);
    }

    private void completeCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        // Cloud synchronization is unavailable; disabling preserves the existing disabled state.
        callbacks.onStatus(enable
                ? new Status(CommonStatusCodes.API_NOT_CONNECTED, "Cloud synchronization is not supported.")
                : Status.SUCCESS);
    }

    @Override
    public void getCloudSyncSetting(IWearableCallbacks callbacks) throws RemoteException {
        callbacks.onGetCloudSyncSettingResponse(new GetCloudSyncSettingResponse(0, false));
    }

    @Override
    public void getCloudSyncOptInStatus(IWearableCallbacks callbacks) throws RemoteException {
        // Completing this read does not grant consent or enable cloud synchronization.
        callbacks.onGetCloudSyncOptInStatusResponse(new GetCloudSyncOptInStatusResponse(0, false, false));
    }

    @Override
    public void getConsent(IWearableCallbacks callbacks) throws RemoteException {
        ConsentResponse response;
        try {
            ConfigurationCaller caller = ConfigurationCaller.capture(context, packageName);
            boolean accepted;
            try (WearableConsentStore store = new WearableConsentStore(context)) {
                accepted = store.read() != null;
            }
            caller.enforceInstalled(context);
            response = new ConsentResponse(0, accepted);
        } catch (SecurityException e) {
            response = new ConsentResponse(CommonStatusCodes.DEVELOPER_ERROR, false);
        } catch (RuntimeException e) {
            response = new ConsentResponse(CommonStatusCodes.INTERNAL_ERROR, false);
        }
        callbacks.onConsentResponse(response);
    }

    @Override
    public void addAccountToConsent(IWearableCallbacks callbacks, AddAccountToConsentRequest request) throws RemoteException {
        Status status;
        try {
            ConfigurationCaller.capture(context, packageName);
            // Global terms consent does not authorize associating a Google account.
            status = new Status(CommonStatusCodes.API_NOT_CONNECTED, "Account consent association is not supported.");
        } catch (SecurityException e) {
            status = new Status(CommonStatusCodes.DEVELOPER_ERROR);
        } catch (RuntimeException e) {
            status = new Status(CommonStatusCodes.INTERNAL_ERROR);
        }
        callbacks.onStatus(status);
    }

    @Override
    public void sendRemoteCommand(IWearableCallbacks callbacks, byte b) throws RemoteException {
        Log.d(TAG, "unimplemented Method: sendRemoteCommand: " + b);
    }

    @Override
    public void getLocalNode(IWearableCallbacks callbacks) throws RemoteException {
        postMain(callbacks, () -> {
            try {
                callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(0, new NodeParcelable(wearable.getLocalNodeId(), wearable.getLocalNodeId())));
            } catch (Exception e) {
                callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(8, null));
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
        postNetwork(callbacks, () -> {
            try {
                callbacks.onGetCapabilityResponse(new GetCapabilityResponse(0, capabilities.get(capability, nodeFilter)));
            } catch (IllegalArgumentException e) {
                callbacks.onGetCapabilityResponse(new GetCapabilityResponse(10, null));
            }
        });
    }

    @Override
    public void getAllCapabilities(IWearableCallbacks callbacks, int nodeFilter) throws RemoteException {
        postNetwork(callbacks, () -> {
            GetAllCapabilitiesResponse response = new GetAllCapabilitiesResponse();
            try {
                response.capabilities = capabilities.getAll(nodeFilter);
                response.statusCode = 0;
            } catch (IllegalArgumentException e) {
                response.capabilities = new ArrayList<>();
                response.statusCode = 10;
            }
            callbacks.onGetAllCapabilitiesResponse(response);
        });
    }

    @Override
    public void addLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                int status;
                try {
                    status = capabilities.add(capability);
                } catch (IllegalArgumentException e) {
                    status = 10;
                }
                callbacks.onAddLocalCapabilityResponse(new AddLocalCapabilityResponse(status));
            }
        });
    }

    @Override
    public void removeLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                int status;
                try {
                    status = capabilities.remove(capability);
                } catch (IllegalArgumentException e) {
                    status = 10;
                }
                callbacks.onRemoveLocalCapabilityResponse(new RemoveLocalCapabilityResponse(status));
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
        Log.d(TAG, "unimplemented Method: getStorageInformation");
    }

    @Override
    public void clearStorage(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: clearStorage");
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

    @Override
    public void openChannel(IWearableCallbacks callbacks, String nodeId, String path) throws RemoteException {
        postNetwork(callbacks, () -> wearable.channels.open(packageName, nodeId, path, callbacks));
    }

    /*
     * Channels
     */

    @Override
    public void closeChannel(IWearableCallbacks callbacks, String s) throws RemoteException {
        closeChannelWithError(callbacks, s, 0);
    }

    @Override
    public void closeChannelWithError(IWearableCallbacks callbacks, String s, int errorCode) throws RemoteException {
        postNetwork(callbacks, () -> wearable.channels.close(packageName, s, errorCode, callbacks));
    }

    @Override
    public void getChannelInputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) throws RemoteException {
        postNetwork(callbacks, () -> wearable.channels.input(packageName, s, channelCallbacks, callbacks));
    }

    @Override
    public void getChannelOutputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) throws RemoteException {
        postNetwork(callbacks, () -> wearable.channels.output(packageName, s, channelCallbacks, callbacks));
    }

    @Override
    public void writeChannelInputToFd(IWearableCallbacks callbacks, String s, ParcelFileDescriptor fd) throws RemoteException {
        Log.d(TAG, "unimplemented Method: writeChannelInputToFd: " + s);
    }

    @Override
    public void readChannelOutputFromFd(IWearableCallbacks callbacks, String s, ParcelFileDescriptor fd, long l1, long l2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: readChannelOutputFromFd: " + s + ", " + l1 + ", " + l2);
    }

    @Override
    public void syncWifiCredentials(IWearableCallbacks callbacks) throws RemoteException {
        callbacks.onStatus(new Status(CommonStatusCodes.API_NOT_CONNECTED,
                "Wi-Fi credential synchronization is not supported."));
    }

    /*
     * Connection deprecated
     */

    @Override
    @Deprecated
    public void putConnection(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        putConfig(callbacks, config);
    }

    @Override
    @Deprecated
    public void getConnection(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfig");
        boolean manager = isConfigurationManager();
        postMain(callbacks, () -> {
            ConnectionConfiguration[] configurations = forCaller(wearable.getConfigurations(), manager);
            if (configurations == null || configurations.length == 0) {
                callbacks.onGetConfigResponse(new GetConfigResponse(1, new ConnectionConfiguration(null, null, 0, 0, false)));
            } else {
                callbacks.onGetConfigResponse(new GetConfigResponse(0, configurations[0]));
            }
        });
    }

    @Override
    @Deprecated
    public void enableConnection(IWearableCallbacks callbacks) throws RemoteException {
        postConfiguration(callbacks, () -> {
            ConnectionConfiguration[] configurations = wearable.getConfigurations();
            if (configurations.length == 0) throw new IllegalArgumentException("Unknown connection configuration");
            wearable.enableConnection(configurations[0].name);
        });
    }

    @Override
    @Deprecated
    public void disableConnection(IWearableCallbacks callbacks) throws RemoteException {
        postConfiguration(callbacks, () -> {
            ConnectionConfiguration[] configurations = wearable.getConfigurations();
            if (configurations.length == 0) throw new IllegalArgumentException("Unknown connection configuration");
            wearable.disableConnection(configurations[0].name);
        });
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", flags=" + flags);
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
