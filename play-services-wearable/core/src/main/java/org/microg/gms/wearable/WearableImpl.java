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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.RemoteListenerProxy;
import org.microg.gms.common.Utils;
import org.microg.gms.wearable.bluetooth.BleClientManager;
import org.microg.gms.wearable.bluetooth.BluetoothClient;
import org.microg.gms.wearable.bluetooth.BluetoothServer;
import org.microg.gms.wearable.channel.ChannelCallbacks;
import org.microg.gms.wearable.channel.ChannelManager;
import org.microg.gms.wearable.channel.ChannelToken;
import org.microg.wearable.SocketConnectionThread;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.AckAsset;
import org.microg.wearable.proto.AppKey;
import org.microg.wearable.proto.AppKeys;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.FetchAsset;
import org.microg.wearable.proto.FilePiece;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;
import org.microg.wearable.proto.SetAsset;
import org.microg.wearable.proto.SetDataItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import okio.ByteString;

public class WearableImpl {

    private static final String TAG = "GmsWear";

    public static final int WEAR_TCP_PORT = 5601;

    private final Context context;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;
    private final Map<String, List<ListenerInfo>> listeners = new HashMap<String, List<ListenerInfo>>();
    private final Set<Node> connectedNodes = new HashSet<Node>();
    private final Map<String, WearableConnection> activeConnections = new HashMap<String, WearableConnection>();
    private RpcHelper rpcHelper;
    private SocketConnectionThread sct;
    private ConnectionConfiguration[] configurations;
    private boolean configurationsUpdated = false;
    private ClockworkNodePreferences clockworkNodePreferences;
    private CountDownLatch networkHandlerLock = new CountDownLatch(1);
    public Handler networkHandler;

    private BluetoothClient bluetoothClient;
    private BluetoothServer bluetoothServer;
    private BleClientManager bleClientManager;

    public static final int TYPE_BLUETOOTH_RFCOMM = 1;
    public static final int TYPE_NETWORK = 2;
    public static final int TYPE_BLE = 3;
    public static final int TYPE_CLOUD = 4;

    public static final int ROLE_CLIENT = 1;
    public static final int ROLE_SERVER = 2;

    private ChannelManager channelManager;

    public WearableImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.clockworkNodePreferences = new ClockworkNodePreferences(context);
        this.rpcHelper = new RpcHelper(context);
        new Thread(() -> {
            Looper.prepare();
            networkHandler = new Handler(Looper.myLooper());
            networkHandlerLock.countDown();
            Looper.loop();
        }).start();

        new Thread(() -> {
            try {
                networkHandlerLock.await();
                channelManager = new ChannelManager(networkHandler, this, getLocalNodeId());
                channelManager.setChannelCallbacks(new WearableChannelCallbacks());
                channelManager.start();
            } catch (InterruptedException e) {
                Log.w(TAG, "Failed to initialize ChannelManager", e);
            }
        }).start();
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public AppKey getAppKey(String packageName) {
        String signatureDigest = PackageUtils.firstSignatureDigest(context, packageName);
        return new AppKey(packageName, signatureDigest);
    }

    private class WearableChannelCallbacks implements ChannelCallbacks {
        @Override
        public void onChannelOpened(ChannelToken token, String path) {
            Log.d(TAG, "onChannelOpened: " + token + ", path=" + path);
            invokeListeners(null, listener -> {
                // todo
            });
        }

        @Override
        public void onChannelClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelClosed: " + token + ", reason=" + closeReason);
            invokeListeners(null, listener -> {
                // todo
            });
        }

        @Override
        public void onChannelInputClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelInputClosed: " + token);
            invokeListeners(null, listener -> {
                // todo
            });
        }

        @Override
        public void onChannelOutputClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelOutputClosed: " + token);
            invokeListeners(null, listener -> {
                // todo
            });
        }
    }


    public String getLocalNodeId() {
        return clockworkNodePreferences.getLocalNodeId();
    }

    public DataItemRecord putDataItem(String packageName, String signatureDigest, String source, DataItemInternal dataItem) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = packageName;
        record.signatureDigest = signatureDigest;
        record.deleted = false;
        record.source = source;
        record.dataItem = dataItem;
        record.v1SeqId = clockworkNodePreferences.getNextSeqId();
        if (record.source.equals(getLocalNodeId())) record.seqId = record.v1SeqId;
        nodeDatabase.putRecord(record);
        return record;
    }

    public DataItemRecord putDataItem(DataItemRecord record) {
        nodeDatabase.putRecord(record);
        if (!record.assetsAreReady) {
            for (Asset asset : record.dataItem.getAssets().values()) {
                if (!nodeDatabase.hasAsset(asset)) {
                    Log.d(TAG, "Asset is missing: " + asset);
                }
            }
        }
        Intent intent = new Intent("com.google.android.gms.wearable.DATA_CHANGED");
        intent.setPackage(record.packageName);
        intent.setData(record.dataItem.uri);
        invokeListeners(intent, listener -> listener.onDataChanged(record.toEventDataHolder()));
        return record;
    }

    private Asset prepareAsset(String packageName, Asset asset) {
        if (asset.getFd() != null && asset.data == null) {
            try {
                asset.data = Utils.readStreamToEnd(new FileInputStream(asset.getFd().getFileDescriptor()));
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
        if (asset.data != null) {
            String digest = calculateDigest(asset.data);
            File assetFile = createAssetFile(digest);
            boolean success = assetFile.exists();
            if (!success) {
                File tmpFile = new File(assetFile.getParent(), assetFile.getName() + ".tmp");

                try {
                    FileOutputStream stream = new FileOutputStream(tmpFile);
                    stream.write(asset.data);
                    stream.close();
                    success = tmpFile.renameTo(assetFile);
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }
            if (success) {
                Log.d(TAG, "Successfully created asset file " + assetFile);
                return Asset.createFromRef(digest);
            } else {
                Log.w(TAG, "Failed creating asset file " + assetFile);
            }
        }
        return null;
    }

    public File createAssetFile(String digest) {
        File dir = new File(new File(context.getFilesDir(), "assets"), digest.substring(digest.length() - 2));
        dir.mkdirs();
        return new File(dir, digest + ".asset");
    }

    private File createAssetReceiveTempFile(String name) {
        File dir = new File(context.getFilesDir(), "piece");
        dir.mkdirs();
        return new File(dir, name);
    }

    private String calculateDigest(byte[] data) {
        try {
            return Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(data), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ConnectionConfiguration getConfigurationByName(String name) {
        if (configurations == null) {
            configurations = configDatabase.getAllConfigurations();
        }

        for (ConnectionConfiguration configuration : configurations) {
            if (configuration.name != null && configuration.name.equals(name)) {
                return configuration;
            }
        }

        return null;
    }

    public synchronized ConnectionConfiguration getConfigurationByNodeId(String nodeId) {
        if (configurations == null) {
            configurations = configDatabase.getAllConfigurations();
        }

        for (ConnectionConfiguration configuration : configurations) {
            if (configuration.nodeId.equals(nodeId)) return configuration;
        }

        return null;
    }

    public synchronized ConnectionConfiguration getConfigurationByAddress(String address) {
        if (configurations == null) {
            configurations = configDatabase.getAllConfigurations();
        }

        for (ConnectionConfiguration configuration : configurations) {
            if (configuration.address.equals(address)) return configuration;
        }

        return null;
    }

    public synchronized ConnectionConfiguration[] getConfigurations() {
        if (configurations == null) {
            configurations = configDatabase.getAllConfigurations();
        }
        if (configurationsUpdated) {
            configurationsUpdated = false;
            ConnectionConfiguration[] newConfigurations = configDatabase.getAllConfigurations();
            for (ConnectionConfiguration configuration : configurations) {
                for (ConnectionConfiguration newConfiguration : newConfigurations) {
                    if (newConfiguration.address != null &&
                            newConfiguration.address.equalsIgnoreCase(configuration.address)) {
                        newConfiguration.connected = configuration.connected;
                        newConfiguration.peerNodeId = configuration.peerNodeId;
                        newConfiguration.nodeId = configuration.nodeId;
                        break;
                    }
                }
            }
            configurations = newConfigurations;
        }

        // companion app crash if name is null
        // name can be null in failed pair (maybe),
        // or maybe i something broke,
        // or not setting name properly somewhere
        for (int i = 0; i < configurations.length; i++) {
            ConnectionConfiguration c = configurations[i];
            if (c.name == null || c.name.isEmpty() || "null".equals(c.name)) {
                String fallbackName = (c.address != null) ? c.address : "Unknown";
                configurations[i] = new ConnectionConfiguration(
                        fallbackName,
                        c.address,
                        c.type,
                        c.role,
                        c.enabled,
                        c.nodeId
                );
                configurations[i].connected = c.connected;
                configurations[i].peerNodeId = c.peerNodeId;
            }
        }


        Log.d(TAG, "Configurations reported: " + Arrays.toString(configurations));
        return configurations;
    }

    private void addConnectedNode(Node node) {
        connectedNodes.add(node);
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    private void removeConnectedNode(String nodeId) {
        for (Node connectedNode : new ArrayList<Node>(connectedNodes)) {
            if (connectedNode.getId().equals(nodeId))
                connectedNodes.remove(connectedNode);
        }
        onConnectedNodes(getConnectedNodesParcelableList());
    }


    public Context getContext() {
        return context;
    }

    public void syncToPeer(String peerNodeId, String nodeId, long seqId) {
        Log.d(TAG, "-- Start syncing over to " + peerNodeId + ", nodeId " + nodeId + " starting with seqId " + seqId);
        Cursor cursor = nodeDatabase.getModifiedDataItems(nodeId, seqId, true);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (!syncRecordToPeer(peerNodeId, DataItemRecord.fromCursor(cursor))) break;
            }
            cursor.close();
        }
        Log.d(TAG, "-- Done syncing over to " + peerNodeId + ", nodeId " + nodeId + " starting with seqId " + seqId);
    }


    void syncRecordToAll(DataItemRecord record) {
        for (String nodeId : new ArrayList<String>(activeConnections.keySet())) {
            syncRecordToPeer(nodeId, record);
        }
    }

    public Map<String, WearableConnection> getActiveConnections() {
        return activeConnections;
    }

    private boolean syncRecordToPeer(String nodeId, DataItemRecord record) {
        for (Asset asset : record.dataItem.getAssets().values()) {
            try {
                syncAssetToPeer(nodeId, record, asset);
            } catch (Exception e) {
                Log.w(TAG, "Could not sync asset " + asset + " for " + nodeId + " and " + record, e);
                closeConnection(nodeId);
                return false;
            }
        }

        try {
            SetDataItem item = record.toSetDataItem();
            activeConnections.get(nodeId).writeMessage(new RootMessage.Builder().setDataItem(item).build());
        } catch (Exception e) {
            Log.w(TAG, e);
            closeConnection(nodeId);
            return false;
        }
        return true;
    }

    private void syncAssetToPeer(String nodeId, DataItemRecord record, Asset asset) throws IOException {
        RootMessage announceMessage = new RootMessage.Builder().setAsset(new SetAsset.Builder()
                .digest(asset.getDigest())
                .appkeys(new AppKeys(Collections.singletonList(new AppKey(record.packageName, record.signatureDigest))))
                .build()).hasAsset(true).build();
        activeConnections.get(nodeId).writeMessage(announceMessage);
        File assetFile = createAssetFile(asset.getDigest());
        String fileName = calculateDigest(announceMessage.encode());
        FileInputStream fis = new FileInputStream(assetFile);
        byte[] arr = new byte[12215];
        ByteString lastPiece = null;
        int c = 0;
        while ((c = fis.read(arr)) > 0) {
            if (lastPiece != null) {
                activeConnections.get(nodeId).writeMessage(new RootMessage.Builder().filePiece(new FilePiece(fileName, false, lastPiece, null)).build());
            }
            lastPiece = ByteString.of(arr, 0, c);
        }
        activeConnections.get(nodeId).writeMessage(new RootMessage.Builder().filePiece(new FilePiece(fileName, true, lastPiece, asset.getDigest())).build());
    }

    public void addAssetToDatabase(Asset asset, List<AppKey> appKeys) {
        nodeDatabase.putAsset(asset, false);
        for (AppKey appKey : appKeys) {
            nodeDatabase.allowAssetAccess(asset.getDigest(), appKey.packageName, appKey.signatureDigest);
        }
    }

    public long getCurrentSeqId(String nodeId) {
        return nodeDatabase.getCurrentSeqId(nodeId);
    }

    public void handleFilePiece(WearableConnection connection, String fileName, byte[] bytes, String finalPieceDigest) {
        File file = createAssetReceiveTempFile(fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(bytes);
            fos.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        if (finalPieceDigest != null) {
            // This is a final piece. If digest matches we're so happy!
            try {
                String digest = calculateDigest(Utils.readStreamToEnd(new FileInputStream(file)));
                if (digest.equals(finalPieceDigest)) {
                    if (file.renameTo(createAssetFile(digest))) {
                        nodeDatabase.markAssetAsPresent(digest);
                        connection.writeMessage(new RootMessage.Builder().ackAsset(new AckAsset(digest)).build());
                    } else {
                        Log.w(TAG, "Could not rename to target file name. delete=" + file.delete());
                    }
                } else {
                    Log.w(TAG, "Received digest does not match. delete=" + file.delete());
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed working with temp file. delete=" + file.delete(), e);
            }
        }
    }

    public void onConnectReceived(WearableConnection connection, String nodeId, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if (config.nodeId.equals(nodeId)) {
                if (config.nodeId != nodeId) {
                    config.nodeId = connect.id;
                    configDatabase.putConfiguration(config, nodeId);
                }
                config.peerNodeId = connect.id;
                config.connected = true;
            }
        }
        Log.d(TAG, "Adding connection to list of open connections: " + connection + " with connect " + connect);
        activeConnections.put(connect.id, connection);
        onPeerConnected(new NodeParcelable(connect.id, connect.name));
        // Fetch missing assets
        syncToPeer(connect.id, nodeId, getCurrentSeqId(nodeId));
        Cursor cursor = nodeDatabase.listMissingAssets();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    Log.d(TAG, "Fetch for " + cursor.getString(12));
                    connection.writeMessage(new RootMessage.Builder()
                            .fetchAsset(new FetchAsset.Builder()
                                    .assetName(cursor.getString(12))
                                    .packageName(cursor.getString(1))
                                    .signatureDigest(cursor.getString(2))
                                    .permission(false)
                                    .build()).build());
                } catch (IOException e) {
                    Log.w(TAG, "Error fetching asset", e);
                    closeConnection(connect.id);
                }
            }
            cursor.close();
        }
    }

    public void onDisconnectReceived(WearableConnection connection, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if (config.nodeId.equals(connect.id)) {
                config.connected = false;
            }
        }
        Log.d(TAG, "Removing connection from list of open connections: " + connection);
        activeConnections.remove(connect.id);
        onPeerDisconnected(new NodeParcelable(connect.id, connect.name));
    }

    public List<NodeParcelable> getConnectedNodesParcelableList() {
        List<NodeParcelable> nodes = new ArrayList<NodeParcelable>();
        for (Node connectedNode : connectedNodes) {
            nodes.add(new NodeParcelable(connectedNode));
        }
        return nodes;
    }

    interface ListenerInvoker {
        void invoke(IWearableListener listener) throws RemoteException;
    }

    private void invokeListeners(@Nullable Intent intent, ListenerInvoker invoker) {
        for (String packageName : new ArrayList<>(listeners.keySet())) {
            List<ListenerInfo> listeners = this.listeners.get(packageName);
            if (listeners == null) continue;
            for (int i = 0; i < listeners.size(); i++) {
                boolean filterMatched = false;
                if (intent != null) {
                    for (IntentFilter filter : listeners.get(i).filters) {
                        filterMatched |= filter.match(context.getContentResolver(), intent, false, TAG) > 0;
                    }
                }
                if (filterMatched || listeners.get(i).filters.length == 0) {
                    try {
                        invoker.invoke(listeners.get(i).listener);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Registered listener at package " + packageName + " failed, removing.");
                        listeners.remove(i);
                        i--;
                    }
                }
            }
            if (listeners.isEmpty()) {
                this.listeners.remove(packageName);
            }
        }
        if (intent != null) {
            try {
                invoker.invoke(RemoteListenerProxy.get(context, intent, IWearableListener.class, "com.google.android.gms.wearable.BIND_LISTENER"));
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to deliver message received to " + intent, e);
            }
        }
    }

    public void onPeerConnected(NodeParcelable node) {
        Log.d(TAG, "onPeerConnected: " + node);
        invokeListeners(null, listener -> listener.onPeerConnected(node));
        addConnectedNode(node);
    }

    public void onPeerDisconnected(NodeParcelable node) {
        Log.d(TAG, "onPeerDisconnected: " + node);
        invokeListeners(null, listener -> listener.onPeerDisconnected(node));
        removeConnectedNode(node.getId());
    }

    public void onConnectedNodes(List<NodeParcelable> nodes) {
        Log.d(TAG, "onConnectedNodes: " + nodes);
        invokeListeners(null, listener -> listener.onConnectedNodes(nodes));
    }

    public DataItemRecord putData(PutDataRequest request, String packageName) {
        DataItemInternal dataItem = new DataItemInternal(fixHost(request.getUri().getHost(), true), request.getUri().getPath());
        for (Map.Entry<String, Asset> assetEntry : request.getAssets().entrySet()) {
            Asset asset = prepareAsset(packageName, assetEntry.getValue());
            if (asset != null) {
                nodeDatabase.putAsset(asset, true);
                dataItem.addAsset(assetEntry.getKey(), asset);
            }
        }
        dataItem.data = request.getData();
        DataItemRecord record = putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), getLocalNodeId(), dataItem);
        syncRecordToAll(record);
        return record;
    }

    public DataHolder getDataItemsAsHolder(String packageName) {
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolder(packageName, PackageUtils.firstSignatureDigest(context, packageName));
        return new DataHolder(dataHolderItems, 0, null);
    }

    private String fixHost(String host, boolean nothingToLocal) {
        if (TextUtils.isEmpty(host) && nothingToLocal) return getLocalNodeId();
        if (TextUtils.isEmpty(host)) return null;
        if (host.equals("local")) return getLocalNodeId();
        return host;
    }

    public DataHolder getDataItemsByUriAsHolder(Uri uri, String packageName) {
        String firstSignature;
        try {
            firstSignature = PackageUtils.firstSignatureDigest(context, packageName);
        } catch (Exception e) {
            return null;
        }
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, firstSignature, fixHost(uri.getHost(), false), uri.getPath());
        DataHolder dataHolder = new DataHolder(dataHolderItems, 0, null);
        Log.d(TAG, "Returning data holder of size " + dataHolder.getCount() + " for query " + uri);
        return dataHolder;
    }

    public synchronized void addListener(String packageName, IWearableListener listener, IntentFilter[] filters) {
        if (!listeners.containsKey(packageName)) {
            listeners.put(packageName, new ArrayList<ListenerInfo>());
        }
        listeners.get(packageName).add(new ListenerInfo(listener, filters));
    }

    public void removeListener(IWearableListener listener) {
        for (List<ListenerInfo> list : listeners.values()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).listener.equals(listener)) {
                    list.remove(i);
                    i--;
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void enableConnection(String name) {
        Log.d(TAG, "enableConnection: " + name);

        ConnectionConfiguration config = getConfigurationByName(name);

        configDatabase.setEnabledState(config.name, true);
        configurationsUpdated = true;

        switch (config.type) {
            case TYPE_CLOUD:
                return;  // abort on cloud type
            case TYPE_BLUETOOTH_RFCOMM:
            case 5:
                handleLegacy(config, true);
                break;
            case TYPE_NETWORK:
                handleNetwork(config, true);
                break;
            case TYPE_BLE:
                handleBle(config, true);
                break;
            default:
                Log.w(TAG, "unimplemented config type: " + config.type);
        }
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disableConnection(String name) {
        Log.d(TAG, "disableConnection: " + name);

        configDatabase.setEnabledState(name, false);
        configurationsUpdated = true;

        ConnectionConfiguration config = configDatabase.getConfiguration(name);

        switch (config.type) {
            case TYPE_CLOUD:
                return;  // abort on cloud type
            case TYPE_BLUETOOTH_RFCOMM:
            case 5:
                handleLegacy(config, false);
                break;
            case TYPE_NETWORK:
                handleNetwork(config, false);
                break;
            case TYPE_BLE:
                handleBle(config, false);
                break;
            default:
                Log.w(TAG, "unimplemented config type: " + config.type);
        }
    }

    public void deleteConnection(String name) {
        configDatabase.deleteConfiguration(name);
        configurationsUpdated = true;
    }

    public void updateConfiguration(ConnectionConfiguration config) {
        Log.d(TAG, "updateConfig: " + config);
        configDatabase.putConfiguration(config);
        configurationsUpdated = true;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void createConnection(ConnectionConfiguration config) {
        if (config.nodeId == null) config.nodeId = getLocalNodeId();
        Log.d(TAG, "putConfig[nyp]: " + config);
        configDatabase.putConfiguration(config);
        configurationsUpdated = true;

        if (configurations != null) {
            ConnectionConfiguration[] newConfigs = new ConnectionConfiguration[configurations.length + 1];
            System.arraycopy(configurations, 0, newConfigs, 0, configurations.length);
            newConfigs[configurations.length] = config;
            configurations = newConfigs;
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void handleBle(ConnectionConfiguration config, boolean enabled) {
        if (config.role == ROLE_CLIENT) {
            if (enabled) {
                try {
                    networkHandlerLock.await();
                    networkHandler.post(() -> {
                        if (bleClientManager == null) {
                            Log.d(TAG, "No BleClientManager found. Initializing a new one.");
                            bleClientManager = new BleClientManager(context);
                        }
                        bleClientManager.addConfiguration(config);
                    });
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while starting BLE client", e);
                }
            } else {
                try {
                    networkHandlerLock.await();
                    networkHandler.post(() -> {
                        if (bleClientManager != null) {
                            bleClientManager.removeConfiguration(config);
                        }
                    });
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while stopping BLE client", e);
                }
            }
        } else if (config.role == ROLE_SERVER) {
            // update ble server config
        }
    }

    private void handleNetwork(ConnectionConfiguration config, boolean enabled) {
        if (enabled) {
            // initialize new network service
        } else {
            // close network service
        }
    }

    private void handleLegacy(ConnectionConfiguration config, boolean enabled) {
        try {
            if (config.role == ROLE_CLIENT) {
                if (enabled) {
                    networkHandlerLock.await();
                    networkHandler.post(() -> {
                        if (bluetoothClient == null) {
                            Log.d(TAG, "No BluetoothClient found. Initializing a new one.");
                            bluetoothClient = new BluetoothClient(context, this);
                        }
                        bluetoothClient.addConfig(config);
                    });
                } else {
                    networkHandlerLock.await();
                    networkHandler.post(() -> {
                        if (bluetoothClient != null) {
                            bluetoothClient.removeConfig(config);
                        }
                    });
                }
            } else if (config.role == ROLE_SERVER) {
                Log.d(TAG, "Bluetooth server not implemented");
                if (enabled) {
//                    networkHandlerLock.await();
//                    networkHandler.post(() -> {
//                        if (bluetoothServer == null) {
//                            Log.d(TAG, "No BluetoothClient found. Initializing a new one.");
//                            bluetoothServer = new BluetoothServer(context);
//                        }
//                        bluetoothServer.addConfiguration(config);
//                    });
                } else {
//                    networkHandlerLock.await();
//                    networkHandler.post(() -> {
//                        if (bluetoothServer != null) {
//                            bluetoothServer.removeConfiguration(config);
//                        }
//                    });
                }
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while duing stuff with bluetooth", e);
        }
    }

    public int deleteDataItems(Uri uri, String packageName) {
        List<DataItemRecord> records = nodeDatabase.deleteDataItems(packageName, PackageUtils.firstSignatureDigest(context, packageName), fixHost(uri.getHost(), false), uri.getPath());
        for (DataItemRecord record : records) {
            syncRecordToAll(record);
        }
        return records.size();
    }

    public void sendMessageReceived(String packageName, MessageEventParcelable messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        Intent intent = new Intent("com.google.android.gms.wearable.MESSAGE_RECEIVED");
        intent.setPackage(packageName);
        intent.setData(Uri.parse("wear://" + getLocalNodeId() + "/" + messageEvent.getPath()));
        invokeListeners(intent, listener -> listener.onMessageReceived(messageEvent));
    }

    public DataItemRecord getDataItemByUri(Uri uri, String packageName) {
        Cursor cursor = nodeDatabase.getDataItemsByHostAndPath(packageName, PackageUtils.firstSignatureDigest(context, packageName), fixHost(uri.getHost(), true), uri.getPath());
        DataItemRecord record = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                record = DataItemRecord.fromCursor(cursor);
            }
            cursor.close();
        }
        Log.d(TAG, "getDataItem: " + record);
        return record;
    }

    private IWearableListener getListener(String packageName, String action, Uri uri) {
        Intent intent = new Intent(action);
        intent.setPackage(packageName);
        intent.setData(uri);

        return RemoteListenerProxy.get(context, intent, IWearableListener.class, "com.google.android.gms.wearable.BIND_LISTENER");
    }

    private void closeConnection(String nodeId) {
        WearableConnection connection = activeConnections.get(nodeId);
        try {
            connection.close();
        } catch (IOException e1) {
            Log.w(TAG, e1);
        }
        if (connection == sct.getWearableConnection()) {
            sct.close();
            sct = null;
        }
        activeConnections.remove(nodeId);
        for (ConnectionConfiguration config : getConfigurations()) {
            if (nodeId.equals(config.nodeId) || nodeId.equals(config.peerNodeId)) {
                config.connected = false;
            }
        }
        onPeerDisconnected(new NodeParcelable(nodeId, "Wear device"));
        Log.d(TAG, "Closed connection to " + nodeId + " on error");
    }

    public int sendMessage(String packageName, String targetNodeId, String path, byte[] data, MessageOptions options) {
        if (activeConnections.containsKey(targetNodeId)) {
            WearableConnection connection = activeConnections.get(targetNodeId);
            RpcHelper.RpcConnectionState state = rpcHelper.useConnectionState(packageName, targetNodeId, path);
            try {
                connection.writeMessage(new RootMessage.Builder().rpcRequest(new Request.Builder()
                        .targetNodeId(targetNodeId)
                        .path(path)
                        .rawData(ByteString.of(data))
                        .packageName(packageName)
                        .signatureDigest(PackageUtils.firstSignatureDigest(context, packageName))
                        .sourceNodeId(getLocalNodeId())
                        .generation(state.generation)
                        .requestId(state.lastRequestId)
                        .build()).build());
            } catch (IOException e) {
                Log.w(TAG, "Error while writing, closing link", e);
                closeConnection(targetNodeId);
                return -1;
            }
            return (state.generation + 527) * 31 + state.lastRequestId;
        }
        Log.d(TAG, targetNodeId + " seems not reachable");
        return -1;
    }

    public int sendRequest(String packageName, String targetNodeId, String path, byte[] data, MessageOptions options) {
        return -1;
    }

    public void stop() {
        try {
            if (channelManager != null) {
                channelManager.stop();
            }
            this.networkHandlerLock.await();
            this.networkHandler.getLooper().quit();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }

    private class ListenerInfo {
        private IWearableListener listener;
        private IntentFilter[] filters;

        private ListenerInfo(IWearableListener listener, IntentFilter[] filters) {
            this.listener = listener;
            this.filters = filters;
        }
    }
}
