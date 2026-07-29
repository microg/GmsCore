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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final CompanionPairingManager companionPairing;
    private final ChannelManager channelManager;

    public WearableServiceImpl(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
        this.capabilities = new CapabilityManager(context, wearable, packageName);
        this.companionPairing = new CompanionPairingManager(context, wearable, packageName);
        this.channelManager = new ChannelManager(wearable);
        this.mainHandler = new Handler(context.getMainLooper());
    }

    /**
     * Returns the CompanionPairingManager for Wear OS 3+ companion device management.
     */
    public CompanionPairingManager getCompanionPairingManager() {
        return companionPairing;
    }

    /**
     * Returns the ChannelManager for Wear OS Channel API data streams.
     */
    public ChannelManager getChannelManager() {
        return channelManager;
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
        postMain(callbacks, () -> {
            wearable.createConnection(config);
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


    @Override
    public void enableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        Log.d(TAG, "enableConfig: " + name);
        postMain(callbacks, () -> {
            wearable.enableConnection(name);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void disableConfig(IWearableCallbacks callbacks, final String name) throws RemoteException {
        Log.d(TAG, "disableConfig: " + name);
        postMain(callbacks, () -> {
            wearable.disableConnection(name);
            callbacks.onStatus(Status.SUCCESS);
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
        Log.d(TAG, "sendMessage: " + targetNodeId + " / " + path + ": " + (data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP)));
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
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    @Deprecated
    public void getCloudSyncOptInDone(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInDone");
    }

    @Override
    public void setCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCloudSyncSetting");
    }

    @Override
    public void getCloudSyncSetting(IWearableCallbacks callbacks) throws RemoteException {
        callbacks.onGetCloudSyncSettingResponse(new GetCloudSyncSettingResponse(0, false));
    }

    @Override
    public void getCloudSyncOptInStatus(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInStatus");
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
        Log.d(TAG, "unimplemented Method: getConnectedCapability " + capability + ", " + nodeFilter);
        postMain(callbacks, () -> {
            List<NodeParcelable> nodes = new ArrayList<>();
            for (String host : capabilities.getNodesForCapability(capability)) {
                nodes.add(new NodeParcelable(host, host));
            }
            CapabilityInfoParcelable capabilityInfo = new CapabilityInfoParcelable(capability, nodes);
            callbacks.onGetCapabilityResponse(new GetCapabilityResponse(0, capabilityInfo));
        });
    }

    @Override
    public void getAllCapabilities(IWearableCallbacks callbacks, int nodeFilter) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getConnectedCapaibilties: " + nodeFilter);
        callbacks.onGetAllCapabilitiesResponse(new GetAllCapabilitiesResponse());
    }

    @Override
    public void addLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        Log.d(TAG, "unimplemented Method: addLocalCapability: " + capability);
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                callbacks.onAddLocalCapabilityResponse(new AddLocalCapabilityResponse(capabilities.add(capability)));
            }
        });
    }

    @Override
    public void removeLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
        Log.d(TAG, "unimplemented Method: removeLocalCapability: " + capability);
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                callbacks.onRemoveLocalCapabilityResponse(new RemoveLocalCapabilityResponse(capabilities.remove(capability)));
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
        Log.d(TAG, "endCall");
        postMain(callbacks, () -> {
            CallBridge.handleCommand(context, CallBridge.CMD_REJECT);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void acceptRingingCall(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "acceptRingingCall");
        postMain(callbacks, () -> {
            CallBridge.handleCommand(context, CallBridge.CMD_ACCEPT);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void silenceRinger(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "silenceRinger");
        postMain(callbacks, () -> {
            CallBridge.handleCommand(context, CallBridge.CMD_SILENCE_RINGER);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    /*
     * Apple Notification Center Service
     */

    @Override
    public void injectAncsNotificationForTesting(IWearableCallbacks callbacks, AncsNotificationParcelable notification) throws RemoteException {
        Log.d(TAG, "unimplemented Method: injectAncsNotificationForTesting: " + notification);
    }

    @Override
    public void doAncsPositiveAction(IWearableCallbacks callbacks, int uid) throws RemoteException {
        Log.d(TAG, "doAncsPositiveAction: uid=" + uid);
        postMain(callbacks, () -> {
            NotificationBridge.doPositiveAction(context, uid);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void doAncsNegativeAction(IWearableCallbacks callbacks, int uid) throws RemoteException {
        Log.d(TAG, "doAncsNegativeAction: uid=" + uid);
        postMain(callbacks, () -> {
            NotificationBridge.doNegativeAction(context, uid);
            callbacks.onStatus(Status.SUCCESS);
        });
    }

    @Override
    public void openChannel(IWearableCallbacks callbacks, String targetNodeId, String path) throws RemoteException {
        Log.d(TAG, "openChannel: node=" + targetNodeId + " path=" + path);
        postMain(callbacks, () -> {
            ChannelParcelable channel = channelManager.openChannel(
                    targetNodeId, path, packageName, "");
            if (channel != null) {
                callbacks.onOpenChannelResponse(new OpenChannelResponse(0, channel));
            } else {
                callbacks.onOpenChannelResponse(new OpenChannelResponse(8, null));
            }
        });
    }

    /*
     * Channels
     */

    @Override
    public void closeChannel(IWearableCallbacks callbacks, String token) throws RemoteException {
        Log.d(TAG, "closeChannel: token=" + token);
        postMain(callbacks, () -> {
            boolean success = channelManager.closeChannel(token);
            callbacks.onStatus(success ? Status.SUCCESS : Status.INTERNAL_ERROR);
        });
    }

    @Override
    public void closeChannelWithError(IWearableCallbacks callbacks, String token, int errorCode) throws RemoteException {
        Log.d(TAG, "closeChannelWithError: token=" + token + " errorCode=" + errorCode);
        closeChannel(callbacks, token);
    }

    @Override
    public void getChannelInputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String token) throws RemoteException {
        Log.d(TAG, "getChannelInputStream: token=" + token);
        postMain(callbacks, () -> {
            InputStream is = channelManager.getInputStream(token);
            if (is != null) {
                try {
                    channelCallbacks.onStreamReady(ParcelFileDescriptor.adoptFd(-1));
                    callbacks.onGetChannelInputStreamResponse(
                            new GetChannelInputStreamResponse(0, new ChannelParcelable()));
                } catch (RemoteException e) {
                    Log.w(TAG, "getChannelInputStream: RemoteException", e);
                    callbacks.onStatus(Status.INTERNAL_ERROR);
                }
            } else {
                callbacks.onStatus(Status.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public void getChannelOutputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String token) throws RemoteException {
        Log.d(TAG, "getChannelOutputStream: token=" + token);
        postMain(callbacks, () -> {
            OutputStream os = channelManager.getOutputStream(token);
            if (os != null) {
                callbacks.onGetChannelOutputStreamResponse(
                        new GetChannelOutputStreamResponse(0, new ChannelParcelable()));
            } else {
                callbacks.onStatus(Status.INTERNAL_ERROR);
            }
        });
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
        Log.d(TAG, "unimplemented Method: syncWifiCredentials");
    }

    /*
     * Connection deprecated
     */

    @Override
    @Deprecated
    public void putConnection(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        Log.d(TAG, "unimplemented Method: putConnection");
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
