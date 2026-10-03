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
import android.content.Intent;
import android.content.IntentFilter;
import android.bluetooth.BluetoothAdapter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.RemoteListenerProxy;
import org.microg.gms.common.Utils;
import org.microg.wearable.SocketConnectionThread;
import org.microg.wearable.CapabilityPaths;
import org.microg.wearable.AssetTransfers;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.WearableRequests;
import org.microg.wearable.proto.AckAsset;
import org.microg.wearable.proto.AppKey;
import org.microg.wearable.proto.AppKeys;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.ChannelRequest;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;

import okio.ByteString;

public class WearableImpl {

    private static final String TAG = "GmsWear";

    private static final int WEAR_TCP_PORT = 5601;

    private final Context context;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;
    private final Map<String, List<ListenerInfo>> listeners = new HashMap<String, List<ListenerInfo>>();
    private final CapabilityListenerDispatcher capabilityListeners;
    private final Set<Node> connectedNodes = Collections.synchronizedSet(new HashSet<Node>());
    private final Map<String, WearableConnection> activeConnections = new ConcurrentHashMap<>();
    private final Map<WearableConnection, File> incomingAssets = new ConcurrentHashMap<>();
    private RpcHelper rpcHelper;
    final ChannelManager channels;
    final RpcRequestManager requests;
    private SocketConnectionThread sct;
    private String tcpConfigurationName;
    private final Map<String, BluetoothConnectionThread> bluetoothConnections = new HashMap<>();
    private volatile boolean stopped;
    private ConnectionConfiguration[] configurations;
    private boolean configurationsUpdated = false;
    private ClockworkNodePreferences clockworkNodePreferences;
    private CountDownLatch networkHandlerLock = new CountDownLatch(1);
    public Handler networkHandler;

    public WearableImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.clockworkNodePreferences = new ClockworkNodePreferences(context);
        this.rpcHelper = new RpcHelper(context);
        this.channels = new ChannelManager(context, this);
        this.requests = new RpcRequestManager(context, this);
        this.capabilityListeners = new CapabilityListenerDispatcher(context, this);
        new Thread(() -> {
            Looper.prepare();
            networkHandler = new Handler(Looper.myLooper());
            networkHandlerLock.countDown();
            if (android.os.Build.VERSION.SDK_INT >= 21) WearableMediaBridge.clearUnauthorizedState(context, this);
            ConnectionConfiguration[] restored = configDatabase.getAllConfigurations();
            int enabledEmulators = 0;
            for (ConnectionConfiguration config : restored) {
                if (config.type == 2 && config.enabled) enabledEmulators++;
            }
            for (ConnectionConfiguration config : restored) {
                try {
                    if (config.enabled && config.name != null) {
                        if (config.type == 2 && config.role == 2) {
                            if (enabledEmulators != 1) throw new IllegalArgumentException("Ambiguous enabled emulators");
                            enableConnection(config.name);
                        } else {
                            restoreBluetoothConnection(config.name);
                        }
                    }
                } catch (RuntimeException e) {
                    Log.w(TAG, "Unable to restore a wearable connection (" + e.getClass().getSimpleName() + ")");
                }
            }
            if (!stopped) Looper.loop();
        }).start();
        try {
            if (!networkHandlerLock.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                stopped = true;
                throw new IllegalStateException("Wearable network handler failed to start");
            }
        } catch (InterruptedException e) {
            stopped = true;
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted starting wearable network handler", e);
        }
    }

    public String getLocalNodeId() {
        return clockworkNodePreferences.getLocalNodeId();
    }

    Cursor getCapabilityRecords() {
        return nodeDatabase.getCapabilityRecords();
    }

    void putCapability(String path, byte kind) {
        DataItemInternal item = new DataItemInternal(getLocalNodeId(), path);
        item.data = new byte[]{kind};
        syncRecordToAll(putDataItem(org.microg.wearable.CapabilityPaths.PACKAGE,
                org.microg.wearable.CapabilityPaths.SIGNATURE, getLocalNodeId(), item));
    }

    void deleteCapability(String path) {
        for (DataItemRecord record : nodeDatabase.deleteDataItems(org.microg.wearable.CapabilityPaths.PACKAGE,
                org.microg.wearable.CapabilityPaths.SIGNATURE, getLocalNodeId(), path)) {
            syncRecordToAll(record);
        }
    }

    void deleteBridgeData(String packageName, String signature, String path) {
        for (DataItemRecord record : nodeDatabase.deleteDataItems(packageName, signature, getLocalNodeId(), path)) {
            if (!record.deleted) continue;
            syncRecordToAll(record);
        }
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
        if (CapabilityPaths.PACKAGE.equals(record.packageName)
                && CapabilityPaths.SIGNATURE.equals(record.signatureDigest)
                && record.dataItem.uri.getPath() != null
                && record.dataItem.uri.getPath().startsWith("/capabilities/")) {
            // The outer system namespace carries another application's capabilities. Never
            // deliver its private path as an ordinary GMS data event.
            String path = record.dataItem.uri.getPath();
            if (!getLocalNodeId().equals(record.dataItem.uri.getHost())) {
                networkHandler.post(() -> notifyCapabilityChanged(path));
            }
            return record;
        }
        if (!record.assetsAreReady) {
            for (Asset asset : record.dataItem.getAssets().values()) {
                if (!nodeDatabase.hasAsset(asset)) {
                Log.d(TAG, "Asset is missing");
                }
            }
        }
        if (!matchesInstalledSignature(record.packageName, record.signatureDigest)) return record;
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
        if (!AssetTransfers.isDigest(digest)) throw new IllegalArgumentException("Invalid wearable asset digest");
        File dir = new File(new File(context.getFilesDir(), "assets"), digest.substring(digest.length() - 2));
        dir.mkdirs();
        return new File(dir, digest + ".asset");
    }

    private String calculateDigest(byte[] data) {
        try {
            return Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(data), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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
                    if (newConfiguration.name.equals(configuration.name)) {
                        newConfiguration.connected = configuration.connected;
                        newConfiguration.peerNodeId = configuration.peerNodeId;
                        newConfiguration.nodeId = configuration.nodeId;
                        break;
                    }
                }
            }
            configurations = newConfigurations;
        }
        Log.d(TAG, "Configurations reported: " + Arrays.toString(configurations));
        return configurations;
    }

    private void addConnectedNode(Node node) {
        synchronized (connectedNodes) {
            connectedNodes.removeIf(existing -> existing.getId().equals(node.getId()));
            connectedNodes.add(node);
        }
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    private void removeConnectedNode(String nodeId) {
        synchronized (connectedNodes) {
            for (Node connectedNode : new ArrayList<Node>(connectedNodes)) {
                if (connectedNode.getId().equals(nodeId))
                    connectedNodes.remove(connectedNode);
            }
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

    private boolean syncRecordToPeer(String nodeId, DataItemRecord record) {
        if (android.os.Build.VERSION.SDK_INT >= 21 && !WearableMediaBridge.canSynchronize(context, record)) return true;
        for (Asset asset : record.dataItem.getAssets().values()) {
            try {
                syncAssetToPeer(nodeId, record, asset);
            } catch (Exception e) {
                Log.w(TAG, "Could not sync asset for " + nodeId, e);
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
        if (!storeFilePiece(connection, fileName, bytes, finalPieceDigest)) return;
        try {
            // Acknowledge outside the service lock; the write can block on a slow peer.
            connection.writeMessage(new RootMessage.Builder().ackAsset(new AckAsset(finalPieceDigest)).build());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid wearable asset transfer", e);
        }
    }

    /** Returns true when the final piece completed and stored an asset. */
    private synchronized boolean storeFilePiece(WearableConnection connection, String fileName, byte[] bytes, String finalPieceDigest) {
        if (stopped || !activeConnections.containsValue(connection)) {
            throw new IllegalArgumentException("Wearable session is no longer active");
        }
        try {
            File directory = incomingAssets.computeIfAbsent(connection, key ->
                    new File(new File(context.getCacheDir(), "wearable-assets"), java.util.UUID.randomUUID().toString()));
            File file = AssetTransfers.append(directory, fileName, bytes, finalPieceDigest);
            if (file != null) {
                if (file.renameTo(createAssetFile(finalPieceDigest))) {
                    nodeDatabase.markAssetAsPresent(finalPieceDigest);
                    return true;
                } else {
                    file.delete();
                    throw new IOException("Cannot store wearable asset");
                }
            }
            return false;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid wearable asset transfer", e);
        }
    }

    public void onConnectReceived(WearableConnection connection,
                                  ConnectionConfiguration expected, Connect connect) {
        // Socket writes can block on a slow peer; never hold the service lock while writing.
        for (RootMessage fetch : registerConnection(connection, expected, connect)) {
            try {
                Log.d(TAG, "Fetching missing wearable asset");
                connection.writeMessage(fetch);
            } catch (IOException e) {
                Log.w(TAG, "Unable to request missing wearable assets", e);
                closeConnection(connect.id);
                break;
            }
        }
    }

    private synchronized List<RootMessage> registerConnection(WearableConnection connection,
                                                              ConnectionConfiguration expected, Connect connect) {
        if (stopped) throw new IllegalStateException("Wearable service has stopped");
        if (connection == null || expected == null || expected.name == null || expected.nodeId == null
                || connect == null || connect.id == null || connect.id.isEmpty()
                || connect.id.equals(getLocalNodeId())
                // EmulatorActivity supplies EmulatorAddr- followed by the expected remote node ID.
                // It is never a socket destination; TCP remains bound to the local ADB bridge.
                || (expected.type == 2 && expected.address != null
                    && !ConfigurationDatabaseHelper.emulatorNodeId(expected).equals(connect.id))
                || (!expected.nodeId.equals(getLocalNodeId()) && !expected.nodeId.equals(connect.id))) {
            throw new IllegalArgumentException("Invalid wearable connection identity");
        }
        if (expected.type == 2 && (sct == null || sct.getWearableConnection() != connection)) {
            throw new IllegalArgumentException("Emulator listener was replaced");
        }
        ConnectionConfiguration selected = null;
        for (ConnectionConfiguration config : getConfigurations()) {
            // Several new configurations share the local node ID. It is never a row identity.
            if (expected.name.equals(config.name)) {
                selected = config;
                break;
            }
        }
        if (selected == null || !selected.enabled || selected.type != expected.type
                || selected.role != expected.role || !Objects.equals(selected.address, expected.address)
                || (!expected.nodeId.equals(selected.nodeId) && !connect.id.equals(selected.nodeId))) {
            throw new IllegalArgumentException("Wearable connection configuration changed");
        }
        if (!connect.id.equals(selected.nodeId)) {
            String previousNodeId = selected.nodeId;
            ConnectionConfiguration updated = new ConnectionConfiguration(selected.name, selected.address,
                    selected.type, selected.role, selected.enabled, connect.id);
            configDatabase.putConfiguration(updated, previousNodeId);
            selected.nodeId = connect.id;
        }
        selected.peerNodeId = connect.id;
        selected.connected = true;
        Log.d(TAG, "Wearable peer negotiated a connection");
        WearableConnection previous = activeConnections.put(connect.id, connection);
        if (previous != null && previous != connection) {
            networkHandler.post(() -> requests.disconnected(previous));
            try {
                previous.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close replaced wearable connection", e);
            }
        }
        networkHandler.post(() -> onPeerConnected(new NodeParcelable(connect.id,
                connect.name == null ? "Wear device" : connect.name, 0, true)));
        // Fetch missing assets
        List<RootMessage> fetches = new ArrayList<>();
        Cursor cursor = nodeDatabase.listMissingAssets();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                fetches.add(new RootMessage.Builder()
                        .fetchAsset(new FetchAsset.Builder()
                                .assetName(cursor.getString(12))
                                .packageName(cursor.getString(1))
                                .signatureDigest(cursor.getString(2))
                                .permission(false)
                                .build()).build());
            }
            cursor.close();
        }
        return fetches;
    }

    public synchronized void onDisconnectReceived(WearableConnection connection, Connect connect) {
        if (connection == null) return;
        networkHandler.post(() -> channels.disconnected(connection));
        requests.disconnected(connection);
        File directory = incomingAssets.remove(connection);
        if (directory != null) AssetTransfers.clear(directory);
        if (connect.id == null || !activeConnections.remove(connect.id, connection)) return;
        for (ConnectionConfiguration config : getConfigurations()) {
            if (connect.id.equals(config.nodeId) || connect.id.equals(config.peerNodeId)) {
                config.connected = false;
            }
        }
        Log.d(TAG, "Removing connection from list of open connections: " + connection);
        networkHandler.post(() -> onPeerDisconnected(new NodeParcelable(connect.id,
                connect.name == null ? "Wear device" : connect.name)));
    }

    public List<NodeParcelable> getConnectedNodesParcelableList() {
        List<NodeParcelable> nodes = new ArrayList<NodeParcelable>();
        synchronized (connectedNodes) {
            for (Node connectedNode : connectedNodes) {
                nodes.add(new NodeParcelable(connectedNode));
            }
        }
        return nodes;
    }

    interface ListenerInvoker {
        void invoke(IWearableListener listener) throws RemoteException;
    }

    static boolean matchesTargetPackage(@Nullable String targetPackage, String listenerPackage) {
        return targetPackage == null || targetPackage.equals(listenerPackage);
    }

    boolean matchesInstalledSignature(String packageName, String signatureDigest) {
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(signatureDigest)) return false;
        try {
            return signatureDigest.equals(PackageUtils.firstSignatureDigest(context, packageName));
        } catch (Exception e) {
            return false;
        }
    }

    private void invokeListeners(@Nullable Intent intent, ListenerInvoker invoker) {
        Map<String, List<ListenerInfo>> snapshot = listenerSnapshot();
        for (String packageName : snapshot.keySet()) {
            if (intent != null && !matchesTargetPackage(intent.getPackage(), packageName)) continue;
            List<ListenerInfo> listeners = snapshot.get(packageName);
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
                        removeListener(listeners.get(i).listener);
                        listeners.remove(i);
                        i--;
                    }
                }
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
        Intent intent = new Intent("com.google.android.gms.wearable.NODE_CHANGED",
                new Uri.Builder().scheme("wear").authority(node.getId()).build());
        invokeListeners(intent, listener -> listener.onPeerConnected(node));
        addConnectedNode(node);
        notifyCapabilitiesForNode(node.getId());
    }

    private void notifyCapabilityChanged(String path) {
        if (stopped) return;
        CapabilityPaths.Key key = CapabilityPaths.parse(path);
        if (key == null || !matchesInstalledSignature(key.packageName, key.signature)) return;
        CapabilityInfoParcelable info = CapabilityManager.snapshot(this, key.packageName, key.signature,
                key.name, CapabilityApi.FILTER_REACHABLE);
        // Capability filters match names, not a peer host; this is an aggregate of reachable nodes.
        Intent intent = new Intent("com.google.android.gms.wearable.CAPABILITY_CHANGED",
                new Uri.Builder().scheme("wear").authority("").path(key.name).build());
        intent.setPackage(key.packageName);
        List<IWearableListener> targets = new ArrayList<>();
        List<ListenerInfo> registered = listenerSnapshot().get(key.packageName);
        if (registered != null) {
            for (ListenerInfo listener : registered) {
                if (!key.signature.equals(listener.signature)) continue;
                boolean matched = listener.filters.length == 0;
                for (IntentFilter filter : listener.filters) {
                    matched |= filter.match(context.getContentResolver(), intent, false, TAG) > 0;
                }
                if (matched) targets.add(listener.listener);
            }
        }
        capabilityListeners.send(path, key.signature, intent, info, targets);
    }

    private void notifyCapabilitiesForNode(String nodeId) {
        Set<String> paths = new HashSet<>();
        try (Cursor cursor = getCapabilityRecords()) {
            while (cursor.moveToNext()) {
                if (nodeId.equals(cursor.getString(0))) paths.add(cursor.getString(1));
            }
        }
        for (String path : paths) notifyCapabilityChanged(path);
    }

    private Map<String, List<ListenerInfo>> listenerSnapshot() {
        synchronized (listeners) {
            Map<String, List<ListenerInfo>> snapshot = new HashMap<>();
            for (Map.Entry<String, List<ListenerInfo>> entry : listeners.entrySet()) {
                snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return snapshot;
        }
    }

    public void onPeerDisconnected(NodeParcelable node) {
        Log.d(TAG, "onPeerDisconnected: " + node);
        Intent intent = new Intent("com.google.android.gms.wearable.NODE_CHANGED",
                new Uri.Builder().scheme("wear").authority(node.getId()).build());
        invokeListeners(intent, listener -> listener.onPeerDisconnected(node));
        removeConnectedNode(node.getId());
        notifyCapabilitiesForNode(node.getId());
    }

    public void onConnectedNodes(List<NodeParcelable> nodes) {
        Log.d(TAG, "onConnectedNodes: " + nodes);
        Intent intent = new Intent("com.google.android.gms.wearable.NODE_CHANGED", Uri.parse("wear:///"));
        invokeListeners(intent, listener -> listener.onConnectedNodes(nodes));
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
        try (Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolder(packageName, PackageUtils.firstSignatureDigest(context, packageName))) {
            return new DataHolder(dataHolderItems, 0, null);
        }
    }

    private String fixHost(String host, boolean nothingToLocal) {
        if (TextUtils.isEmpty(host) && nothingToLocal) return getLocalNodeId();
        if (TextUtils.isEmpty(host)) return null;
        if (host.equals("local")) return getLocalNodeId();
        return host;
    }

    public DataHolder getDataItemsByUriAsHolder(Uri uri, String packageName, int filterType) {
        if (uri == null) throw new IllegalArgumentException("A data item URI is required");
        String firstSignature;
        try {
            firstSignature = PackageUtils.firstSignatureDigest(context, packageName);
        } catch (Exception e) {
            return null;
        }
        try (Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, firstSignature,
                fixHost(uri.getHost(), false), uri.getPath(), filterType)) {
            DataHolder dataHolder = new DataHolder(dataHolderItems, 0, null);
            Log.d(TAG, "Returning data holder of size " + dataHolder.getCount() + " for query " + uri);
            return dataHolder;
        }
    }

    public void addListener(String packageName, IWearableListener listener, IntentFilter[] filters) {
        String signature;
        try {
            signature = PackageUtils.firstSignatureDigest(context, packageName);
        } catch (Exception e) {
            signature = null;
        }
        synchronized (listeners) {
            if (!listeners.containsKey(packageName)) {
                listeners.put(packageName, new ArrayList<ListenerInfo>());
            }
            listeners.get(packageName).add(new ListenerInfo(listener,
                    filters == null ? new IntentFilter[0] : filters.clone(), signature));
        }
    }

    public void removeListener(IWearableListener listener) {
        synchronized (listeners) {
            for (String packageName : new ArrayList<>(listeners.keySet())) {
                List<ListenerInfo> list = listeners.get(packageName);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).listener.equals(listener)) {
                        list.remove(i);
                        i--;
                    }
                }
                if (list.isEmpty()) listeners.remove(packageName);
            }
        }
    }

    public synchronized void enableConnection(String name) {
        if (stopped) throw new IllegalStateException("Wearable service has stopped");
        ConnectionConfiguration config = configDatabase.getConfiguration(name);
        if (config == null) throw new IllegalArgumentException("Unknown connection configuration");
        checkConfigurationTransport(config);
        configDatabase.enableManagedConfiguration(name);
        configurationsUpdated = true;
        if (config.type == 2) closeInactiveEmulatorConnections();
        if (config.type == 2 && config.role == 2 && (sct == null || !sct.isAlive())) {
            Log.d(TAG, "Starting server on :" + WEAR_TCP_PORT);
            tcpConfigurationName = name;
            (sct = SocketConnectionThread.serverListen(context, WEAR_TCP_PORT, new MessageHandler(context, this, config))).start();
        } else if (config.type != 2) {
            startBluetoothConnection(config);
        }
    }

    /** Re-reads the stored state under the service lock, so a concurrent disable is not undone. */
    private synchronized void restoreBluetoothConnection(String name) {
        for (ConnectionConfiguration current : configDatabase.getAllConfigurations()) {
            if (name.equals(current.name)) {
                if (current.enabled) startBluetoothConnection(current);
                return;
            }
        }
    }

    private void startBluetoothConnection(ConnectionConfiguration config) {
        if (config.address == null || (config.type != 1 && config.type != 5) || config.role != 1) return;
        synchronized (bluetoothConnections) {
            if (stopped) return;
            BluetoothConnectionThread previous = bluetoothConnections.get(config.name);
            if (previous != null && previous.isAlive()) return;
            BluetoothConnectionThread next = new BluetoothConnectionThread(
                    BluetoothAdapter.getDefaultAdapter(), config.address,
                    new MessageHandler(context, this, config));
            bluetoothConnections.put(config.name, next);
            next.start();
        }
    }

    public synchronized void disableConnection(String name) {
        configDatabase.setEnabledState(name, false);
        configurationsUpdated = true;
        synchronized (bluetoothConnections) {
            BluetoothConnectionThread connection = bluetoothConnections.remove(name);
            if (connection != null) connection.closeConnection();
        }
        if (name != null && name.equals(tcpConfigurationName) && sct != null) {
            // The connection's disconnect callback removes its node and pending operations.
            // There may not yet be an accepted connection to remove.
            sct.close();
            sct.interrupt();
            sct = null;
            tcpConfigurationName = null;
        }
    }

    private void closeInactiveEmulatorConnections() {
        boolean keepListener = false;
        for (ConnectionConfiguration config : getConfigurations()) {
            if (config.type != 2) continue;
            if (config.enabled && Objects.equals(config.name, tcpConfigurationName)) keepListener = true;
            if (!config.enabled) config.connected = false;
        }
        if (!keepListener && sct != null) {
            SocketConnectionThread previous = sct;
            sct = null;
            tcpConfigurationName = null;
            WearableConnection previousConnection = previous.getWearableConnection();
            for (String nodeId : new ArrayList<>(activeConnections.keySet())) {
                // Historical configurations can share a negotiated node. Retire only this transport.
                if (previousConnection != null && activeConnections.get(nodeId) == previousConnection) {
                    closeConnection(nodeId);
                }
            }
            previous.close();
        }
    }

    public synchronized void deleteConnection(String name) {
        disableConnection(name);
        configDatabase.deleteConfiguration(name);
        if (configurations != null) {
            List<ConnectionConfiguration> retained = new ArrayList<>();
            for (ConnectionConfiguration config : configurations) {
                if (!Objects.equals(name, config.name)) retained.add(config);
            }
            // A later put with the same name is a new link, not the deleted link's runtime state.
            configurations = retained.toArray(new ConnectionConfiguration[0]);
        }
        configurationsUpdated = true;
    }

    public synchronized void createConnection(ConnectionConfiguration config) {
        if (stopped) throw new IllegalStateException("Wearable service has stopped");
        checkConfigurationTransport(config);
        ConnectionConfiguration stored = configDatabase.putManagedConfiguration(config, getLocalNodeId());
        configurationsUpdated = true;
        if (stored.type == 2) closeInactiveEmulatorConnections();
        // putConfig is also the official emulator setup entry point. Acceptance does not mean connected.
        if (stored.enabled) enableConnection(stored.name);
        else disableConnection(stored.name);
    }

    public synchronized void updateConnection(ConnectionConfiguration config) {
        if (stopped) throw new IllegalStateException("Wearable service has stopped");
        checkConfigurationTransport(config);
        if (configDatabase.updateConfiguration(config)) {
            configurationsUpdated = true;
            if (config.type == 2) {
                // Selecting an emulator retires the others; enableConfig starts the listener.
                closeInactiveEmulatorConnections();
            } else {
                // Apply a Bluetooth link's stored enabled state, as enableConfig/disableConfig do.
                ConnectionConfiguration stored = config.name == null ? null : configDatabase.getConfiguration(config.name);
                if (stored != null && stored.enabled) {
                    startBluetoothConnection(stored);
                } else if (stored != null) {
                    synchronized (bluetoothConnections) {
                        BluetoothConnectionThread connection = bluetoothConnections.remove(stored.name);
                        if (connection != null) connection.closeConnection();
                    }
                }
            }
        }
    }

    private void checkConfigurationTransport(ConnectionConfiguration config) {
        ConfigurationDatabaseHelper.validateManagedConfiguration(config);
        if (config.type == 2) org.microg.wearable.EmulatorTransportPolicy.requireAllowed(context);
    }

    public int deleteDataItems(Uri uri, String packageName, int filterType) {
        if (uri == null) throw new IllegalArgumentException("A data item URI is required");
        List<DataItemRecord> records = nodeDatabase.deleteDataItems(packageName, PackageUtils.firstSignatureDigest(context, packageName),
                fixHost(uri.getHost(), false), uri.getPath(), filterType);
        for (DataItemRecord record : records) {
            syncRecordToAll(record);
        }
        return records.size();
    }

    public void sendMessageReceived(String packageName, String signatureDigest, MessageEventParcelable messageEvent) {
        if (!matchesInstalledSignature(packageName, signatureDigest)) return;
        Log.d(TAG, "onMessageReceived");
        Intent intent = new Intent("com.google.android.gms.wearable.MESSAGE_RECEIVED");
        intent.setPackage(packageName);
        intent.setData(new Uri.Builder().scheme("wear").authority(getLocalNodeId())
                .path(messageEvent.getPath()).build());
        invokeListeners(intent, listener -> listener.onMessageReceived(messageEvent));
    }

    IWearableListener requestListener(String packageName, MessageEventParcelable event) {
        Intent intent = new Intent("com.google.android.gms.wearable.REQUEST_RECEIVED");
        intent.setPackage(packageName);
        intent.setData(new Uri.Builder().scheme("wear").authority(event.sourceNodeId).path(event.path).build());
        // Snapshot on the network handler; only Binder delivery runs on the bounded worker pool.
        List<ListenerInfo> registered = listenerSnapshot().get(packageName);
        if (registered != null) {
            for (ListenerInfo info : registered) {
                boolean matched = info.filters.length == 0;
                for (IntentFilter filter : info.filters) {
                    matched |= filter.match(context.getContentResolver(), intent, false, TAG) > 0;
                }
                if (matched) return info.listener;
            }
        }
        return null;
    }

    Request.Builder newRpcEnvelope(String packageName, String signature, String node, String path) {
        return newRpcEnvelope(packageName, signature, node, path, 0);
    }

    Request.Builder newRpcEnvelope(String packageName, String signature, String node, String path, int priority) {
        RpcHelper.RpcConnectionState state = rpcHelper.useConnectionState(packageName, node, path, priority);
        return WearableRequests.envelope(packageName, signature, node, getLocalNodeId(), path,
                state.generation, state.lastRequestId);
    }

    WearableConnection connectionForNode(String nodeId) {
        return stopped || nodeId == null ? null : activeConnections.get(nodeId);
    }

    boolean isCurrentConnection(String nodeId, WearableConnection connection) {
        return !stopped && connection != null && connectionForNode(nodeId) == connection;
    }

    void sendChannelRequest(WearableConnection connection, String packageName, String signature,
                            String nodeId, ChannelRequest request) throws IOException {
        if (!isCurrentConnection(nodeId, connection)) throw new IOException("Channel transport disconnected");
        RpcHelper.RpcConnectionState state = rpcHelper.useUnorderedConnectionState(packageName, nodeId, "");
        connection.writeMessage(new RootMessage.Builder().channelRequest(WearableRequests.envelope(
                packageName, signature, nodeId, getLocalNodeId(), "", state.generation, state.lastRequestId)
                .request(request).build()).build());
    }

    void channelEvent(String packageName, ChannelEventParcelable event) {
        Intent intent = new Intent("com.google.android.gms.wearable.CHANNEL_EVENT");
        intent.setPackage(packageName);
        intent.setData(new Uri.Builder().scheme("wear").authority(event.channel.nodeId)
                .path(event.channel.path).build());
        invokeListeners(intent, listener -> listener.onChannelEvent(event));
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
        Log.d(TAG, "getDataItem");
        return record;
    }

    private IWearableListener getListener(String packageName, String action, Uri uri) {
        Intent intent = new Intent(action);
        intent.setPackage(packageName);
        intent.setData(uri);

        return RemoteListenerProxy.get(context, intent, IWearableListener.class, "com.google.android.gms.wearable.BIND_LISTENER");
    }

    private synchronized void closeConnection(String nodeId) {
        WearableConnection connection = activeConnections.get(nodeId);
        if (connection == null || !activeConnections.remove(nodeId, connection)) return;
        networkHandler.post(() -> channels.disconnected(connection));
        requests.disconnected(connection);
        try {
            connection.close();
        } catch (IOException e1) {
            Log.w(TAG, e1);
        }
        // Keep the emulator listener available for the next connection. Disabling or deleting
        // its configuration, or stopping the service, closes the listener explicitly.
        for (ConnectionConfiguration config : getConfigurations()) {
            if (nodeId.equals(config.nodeId) || nodeId.equals(config.peerNodeId)) {
                config.connected = false;
            }
        }
        networkHandler.post(() -> onPeerDisconnected(new NodeParcelable(nodeId, "Wear device")));
        Log.d(TAG, "Closed connection to " + nodeId + " on error");
    }

    public int sendMessage(String packageName, String targetNodeId, String path, byte[] data) {
        if (activeConnections.containsKey(targetNodeId)) {
            WearableConnection connection = activeConnections.get(targetNodeId);
            // Validate everything before consuming an ordered request ID; a skipped ID would make
            // the peer wait for a message that is never sent.
            String signature = PackageUtils.firstSignatureDigest(context, packageName);
            if (connection == null || signature == null || path == null) return -1;
            ByteString payload = data == null ? ByteString.EMPTY : ByteString.of(data);
            RpcHelper.RpcConnectionState state = rpcHelper.useConnectionState(packageName, targetNodeId, path);
            try {
                connection.writeMessage(new RootMessage.Builder().rpcRequest(WearableRequests.envelope(
                        packageName, signature,
                        targetNodeId, getLocalNodeId(), path, state.generation, state.lastRequestId)
                        .rawData(payload)
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

    public synchronized void stop() {
        capabilityListeners.stop();
        requests.stop();
        channels.stop();
        synchronized (bluetoothConnections) {
            stopped = true;
            for (BluetoothConnectionThread connection : bluetoothConnections.values()) {
                connection.closeConnection();
            }
            bluetoothConnections.clear();
        }
        if (sct != null) {
            sct.close();
            sct = null;
        }
        for (File directory : incomingAssets.values()) AssetTransfers.clear(directory);
        incomingAssets.clear();
        try {
            this.networkHandlerLock.await();
            this.networkHandler.getLooper().quit();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }

    private class ListenerInfo {
        private IWearableListener listener;
        private IntentFilter[] filters;
        private final String signature;

        private ListenerInfo(IWearableListener listener, IntentFilter[] filters, String signature) {
            this.listener = listener;
            this.filters = filters;
            this.signature = signature;
        }
    }
}
