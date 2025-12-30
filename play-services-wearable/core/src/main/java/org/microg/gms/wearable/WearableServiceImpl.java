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
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.internal.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "GmsWearSvcImpl";

    private final Context context;
    private final String packageName;
    private final WearableImpl wearable;
    private final Handler mainHandler;
    private final CapabilityManager capabilities;

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
        Log.d(TAG, "sendRequest: " + targetNodeId + " / " + path + ": " + (data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP)));
        sendRequestWithOptions(callbacks, targetNodeId, path, data, new MessageOptions(0));
    }

    @Override
    public void sendRequestWithOptions(IWearableCallbacks callbacks, final String targetNodeId, final String path, final byte[] data, MessageOptions options) throws RemoteException {
        Log.d(TAG, "sendRequest: " + targetNodeId + " / " + path + ": " + (data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP)));
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                RpcResponse rpcResponse = new RpcResponse(4004, -1, new byte[0]);
                try {
                    rpcResponse.requestId = wearable.sendRequest(packageName, targetNodeId, path, data, options);
                    if (rpcResponse.requestId == -1) {
                        rpcResponse.statusCode = 4004;
                    }
                } catch (Exception e) {
                    rpcResponse.statusCode = 8;
                }
                mainHandler.post(() -> {
                    try {
                        callbacks.onRpcResponse(rpcResponse);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void getCompanionPackageForNode(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "unimplemented Method getCompanionPackageForNode");

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
        callbacks.onGetCloudSyncOptInOutDoneResponse(new GetCloudSyncOptInOutDoneResponse(0, false));
    }

    @Override
    public void setCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCloudSyncSetting");

        postMain(callbacks, () -> {
            // dummy stuff
            callbacks.onStatus(new Status(0));
        });
    }

    @Override
    public void getCloudSyncSetting(IWearableCallbacks callbacks) throws RemoteException {
        callbacks.onGetCloudSyncSettingResponse(new GetCloudSyncSettingResponse(0, false));
    }

    @Override
    public void getCloudSyncOptInStatus(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCloudSyncOptInStatus");
        callbacks.onGetCloudSyncOptInStatusResponse(new GetCloudSyncOptInStatusResponse(0, false, true));
    }

    @Override
    public void sendRemoteCommand(IWearableCallbacks callbacks, byte b) throws RemoteException {
        Log.d(TAG, "unimplemented Method: sendRemoteCommand: " + b);
    }

    @Override
    public void getConsentStatus(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getConsentStatus");

        // needed proper implementation
        this.wearable.networkHandler.post(new CallbackRunnable(callbacks) {
            @Override
            public void run(IWearableCallbacks callbacks) throws RemoteException {
                try {
                    // get data from Tos activity? idk,
                    // maybe need some Consent manager or something
                    ConsentResponse cr = new ConsentResponse(
                            0,
                            true,
                            false,
                            false,
                            false,
                            null,
                            wearable.getLocalNodeId(),
                            System.currentTimeMillis()
                    );
                    callbacks.onConsentResponse(cr);
                    Log.d(TAG, cr.toString());

                } catch (Exception e) {
                    Log.e(TAG, "getConsentStatus exception", e);
                    callbacks.onConsentResponse(new ConsentResponse(
                            13, false, false, false, false,
                            null, null, null
                    ));
                }
            }
        });
    }

    @Override
    public void addAccountToConsent(IWearableCallbacks callbacks, AddAccountToConsentRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method addAccountToConsent: "
                + "account=" + request.accountName
                + ", consent=" + request.consentGranted);

    }

    @Override
    public void someBoolUnknown(IWearableCallbacks callbacks) throws RemoteException {
        // not sure what it is, but i thinking this is to do something with a certificate verification
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
        Log.d(TAG, "unimplemented Method logCounter: "
                + request.counterName
                + ", value=" + request.value
                + ", increment=" + request.increment);

        postMain(callbacks, () -> {
            callbacks.onStatus(new Status(0));
        });
    }

    @Override
    public void logEvent(IWearableCallbacks callbacks, LogEventRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method logEvent: data length="
                + (request.eventData != null ? request.eventData.length : 0));

        postMain(callbacks, () -> {
            callbacks.onStatus(new Status(0));
        });
    }

    @Override
    public void logTimer(IWearableCallbacks callbacks, LogTimerRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method logTimer: " + request.timerName
                + ", timestamp=" + request.timestamp);

        postMain(callbacks, () -> {
            callbacks.onStatus(new Status(0));
        });
    }

    @Override
    public void clearLogs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method clearLogs");
        postMain(callbacks, () -> {
            callbacks.onStatus(new Status(0));
        });
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
    public void getNodeId(IWearableCallbacks callbacks, String address) throws RemoteException {
        postNetwork(callbacks, () -> {
            String resultNode;
            ConnectionConfiguration configuration = wearable.getConfiguration(address);
            try {
                if (address == null || configuration == null || configuration.type == 4 || !address.equals(configuration.address)) {
                    resultNode = null;
                } else {
                    resultNode = configuration.peerNodeId;
                    if (resultNode == null) resultNode = configuration.nodeId;
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

                for (String nodeId : nodeIds) {
                    if (shouldIncludeNode(nodeId, nodeFilter)) {
                        nodes.add(new NodeParcelable(nodeId, nodeId));
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
//        Log.d(TAG, "unimplemented Method: getConnectedCapaibilties: " + nodeFilter);
//        callbacks.onGetAllCapabilitiesResponse(new GetAllCapabilitiesResponse());

        Log.d(TAG, "getAllCapabilities: nodeFilter=" + nodeFilter);
        postMain(callbacks, () -> {
            try {
                Map<String, CapabilityInfoParcelable> capabilitiesMap = new HashMap<>();

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
                                        if (shouldIncludeNode(nodeId, nodeFilter)){
                                            nodes.add(new NodeParcelable(nodeId, nodeId));
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

    private boolean shouldIncludeNode(String nodeId, int nodeFilter) {
        switch (nodeFilter) {
            case 0:
                return true;
            case 1:
            case 2:
                ConnectionConfiguration[] configs = wearable.getConfigurations();
                if (configs != null) {
                    for (ConnectionConfiguration config: configs) {
                        if (nodeId.equals(config.nodeId) && config.connected) {
                            return true;
                        }
                    }
                }
            default:
                Log.w(TAG, "Unknown node filter: " + nodeFilter + ", including all nodes");
                return true;
        }
    }

    @Override
    public void addLocalCapability(IWearableCallbacks callbacks, String capability) throws RemoteException {
//        Log.d(TAG, "unimplemented Method: addLocalCapability: " + capability);
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
//        Log.d(TAG, "unimplemented Method: removeLocalCapability: " + capability);
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
    public void openChannel(IWearableCallbacks callbacks, String s1, String s2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: openChannel; " + s1 + ", " + s2);
    }

    /*
     * Channels
     */

    @Override
    public void closeChannel(IWearableCallbacks callbacks, String s) throws RemoteException {
        Log.d(TAG, "unimplemented Method: closeChannel: " + s);
    }

    @Override
    public void closeChannelWithError(IWearableCallbacks callbacks, String s, int errorCode) throws RemoteException {
        Log.d(TAG, "unimplemented Method: closeChannelWithError:" + s + ", " + errorCode);

    }

    @Override
    public void getChannelInputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getChannelInputStream: " + s);
    }

    @Override
    public void getChannelOutputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getChannelOutputStream: " + s);
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

        postMain(callbacks, () -> {
            // dummy stuff
            callbacks.onStatus(new Status(0));
        });
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
