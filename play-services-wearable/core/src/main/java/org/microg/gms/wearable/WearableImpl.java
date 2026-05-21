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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.RemoteListenerProxy;
import org.microg.gms.common.Utils;
import org.microg.gms.wearable.bluetooth.BleManager;
import org.microg.gms.wearable.bluetooth.BluetoothClient;
import org.microg.gms.wearable.network.NetworkConnectionManager;
import org.microg.gms.wearable.channel.ChannelAssetApiEnum;
import org.microg.gms.wearable.channel.ChannelCallbacks;
import org.microg.gms.wearable.channel.ChannelManager;
import org.microg.gms.wearable.channel.ChannelToken;
import org.microg.gms.wearable.channel.TrustedPeersService;
import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.AppKeys;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.ControlMessage;
import org.microg.gms.wearable.proto.FilePiece;
import org.microg.gms.wearable.proto.Request;
import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SetAsset;
import org.microg.gms.wearable.proto.SetDataItem;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import okio.ByteString;

public class WearableImpl {

    public static final int WEAR_TCP_PORT = 5601;
    public static final int TYPE_BLUETOOTH_RFCOMM = 1;
    public static final int TYPE_NETWORK = 2;
    public static final int TYPE_BLE = 3;
    public static final int TYPE_CLOUD = 4;
    public static final int ROLE_CLIENT = 1;
    public static final int ROLE_SERVER = 2;
    private static final String TAG = "GmsWear";
    private static final long ASSET_FETCH_COOLDOWN_MS = 500;
    private static final int ASSET_BATCH_SIZE = 10;
    private static final long NODE_DISCONNECT_DEBOUNCE_MS = 2500;
    private final Context context;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;
    private final Map<String, List<ListenerInfo>> listeners = new ConcurrentHashMap<>();
    private final Set<Node> connectedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, WearableConnection> activeConnections = new ConcurrentHashMap<>();
    private final Handler disconnectDebounceHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingDisconnects = new ConcurrentHashMap<>();
    private final Map<String, DataTransport> peerTransports = new ConcurrentHashMap<>();
    private final AssetManager assetManager = new AssetManager(this);
    private final Map<String, PendingRpcRequest> pendingRpcRequests = new ConcurrentHashMap<>();
    public Handler networkHandler;
    private RpcHelper rpcHelper;
    private SocketConnectionThread sct;
    private ConnectionConfiguration[] configurations;
    private boolean configurationsUpdated = false;
    private ClockworkNodePreferences clockworkNodePreferences;
    private CountDownLatch networkHandlerLock = new CountDownLatch(1);
    private HandlerThread networkHandlerThread;
    private BluetoothClient bluetoothClient;
    private BleManager bleManager;
    private volatile ChannelManager channelManager;
    private NodeMigrationController migrationController;
    private NodeMigrationTracker migrationTracker;
    private volatile long lastAssetFetchTime = 0;
    private AssetFetcher assetFetcher;
    private NetworkConnectionManager networkManager;

    public WearableImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.clockworkNodePreferences = new ClockworkNodePreferences(context);
        this.rpcHelper = new RpcHelper(context);
        this.migrationTracker = new NodeMigrationTracker(nodeDatabase);

        Map<String, ConnectionConfiguration> best = new HashMap<>();
        for (ConnectionConfiguration c : configDatabase.getAllConfigurations()) {
            if (c.address == null) continue;
            String key = c.address.toLowerCase();
            ConnectionConfiguration prev = best.get(key);
            if (prev == null) {
                best.put(key, c);
                continue;
            }

            boolean prevIsAddr = prev.name != null && prev.name.equalsIgnoreCase(prev.address);
            boolean curIsAddr = c.name != null && c.name.equalsIgnoreCase(c.address);
            if (prevIsAddr && !curIsAddr) {
                configDatabase.deleteConfiguration(prev.name);
                best.put(key, c);
            } else {
                configDatabase.deleteConfiguration(c.name);
            }
        }

        configurationsUpdated = true;

        networkHandlerThread = new HandlerThread("wearNetworkHandler");
        networkHandlerThread.start();
        networkHandler = new Handler(networkHandlerThread.getLooper());
        networkHandlerLock.countDown();

        new Thread(() -> {
            try {
                networkHandlerLock.await();
                TrustedPeersService trustedPeers = new TrustedPeersService(context);
                channelManager = new ChannelManager(networkHandler, this, getLocalNodeId(), trustedPeers);
                WearableChannelCallbacks callbacks = new WearableChannelCallbacks();
                channelManager.setChannelCallbacks(callbacks);
                channelManager.setCallbacks(ChannelAssetApiEnum.ORIGIN_CHANNEL_API, callbacks);
                channelManager.start();
            } catch (InterruptedException e) {
                Log.w(TAG, "Failed to initialize ChannelManager", e);
            }
        }).start();

        this.migrationController = new NodeMigrationController();
        this.assetFetcher = new AssetFetcher(nodeDatabase, networkHandler);
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public NodeMigrationTracker getMigrationTracker() {
        return migrationTracker;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public AppKey getAppKey(String packageName) {
        String signatureDigest = PackageUtils.firstSignatureDigest(context, packageName);
        return new AppKey(packageName, signatureDigest);
    }

    public void startNodeMigration(String newNodeId, String migratingFromId) {
        Log.d(TAG, "startNodeMigration: node=" + newNodeId + " from=" + migratingFromId);
        try {
            android.database.sqlite.SQLiteDatabase db = nodeDatabase.getWritableDatabase();
            migrationTracker.updateMigrationInfo(db, newNodeId, migratingFromId);
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(TAG, "DB error while recording node migration state", e);
            return;
        }
        migrationController.startMigrationForNode(newNodeId);
    }

    public void terminateAssociation(String nodeId, boolean removeBond, String reason) {
        Log.d(TAG, "terminateAssociation: node=" + nodeId
                + ", removeBond=" + removeBond + ", reason=" + reason);

        ConnectionConfiguration target = null;
        for (ConnectionConfiguration cfg : getConfigurations()) {
            if (nodeId.equals(cfg.nodeId) || nodeId.equals(cfg.peerNodeId)) {
                target = cfg;
                break;
            }
        }

        if (target == null) {
            Log.i(TAG, "terminateAssociation: no config found for node " + nodeId);
            return;
        }

        WearableConnection conn = activeConnections.get(nodeId);
        if (conn != null) {
            try {
                conn.close();
            } catch (java.io.IOException ignored) {
            }
            activeConnections.remove(nodeId);
            onPeerDisconnected(new com.google.android.gms.wearable.internal.NodeParcelable(nodeId, target.name));
        }

        deleteConnection(target.name);

        if (removeBond && target.address != null) {
            try {
                android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    android.bluetooth.BluetoothDevice device = adapter.getRemoteDevice(target.address);
                    if (device != null) {
                        java.lang.reflect.Method m = device.getClass().getMethod("removeBond");
                        m.invoke(device);
                        Log.d(TAG, "Removed BT bond for " + target.address);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to remove BT bond for " + target.address, e);
            }
        }
    }

    public void registerPeerWriter(String peerNodeId, WearableWriter writer) {
        DataTransport transport = peerTransports.get(peerNodeId);

        if (transport == null) {
            transport = new DataTransport(getLocalNodeId(), peerNodeId, this);
            peerTransports.put(peerNodeId, transport);
        }

        transport.onConnected(writer);
        Log.d(TAG, "registerPeerWriter for " + peerNodeId);

        assetManager.addWriter(peerNodeId, writer);
    }

    public DataTransport getDataTransport(String peerNodeId) {
        return peerTransports.get(peerNodeId);
    }

    public String getLocalNodeId() {
        ConnectionConfiguration[] cfgs = getConfigurations();
        if (cfgs != null) {
            for (ConnectionConfiguration c : cfgs) {
                if (c.nodeId != null && !c.nodeId.isEmpty()) return c.nodeId;
            }
        }
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
        record.seqId = record.v1SeqId;
        nodeDatabase.putRecord(record);
        return record;
    }

    private void maybeDispatchCapabilityChanged(DataItemRecord record) {
        String path = record.dataItem.path;
        if (path == null || !path.startsWith("/capabilities/")) return;

        String[] segments = path.split("/");
        if (segments.length < 5) return;

        String capabilityName = Uri.decode(segments[4]);
        if (capabilityName == null || capabilityName.isEmpty()) return;

        Cursor cursor = nodeDatabase.getDataItemsByHostAndPath(
                record.packageName, record.signatureDigest, null,
                "/capabilities/" + segments[3] + "/" + segments[4]
        );

        Set<String> nodeIds = new HashSet<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DataItemRecord r = DataItemRecord.fromCursor(cursor);
                if (!r.deleted && r.dataItem.host != null) {
                    nodeIds.add(r.dataItem.host);
                }
            }
            cursor.close();
        }

        List<NodeParcelable> nodes = new ArrayList<>();
        for (String nid : nodeIds) {
            ConnectionConfiguration cfg = getConfigurationByNodeId(nid);
            if (cfg == null) {
                cfg = getConfigurationByPeerNodeId(nid);
            }

            String name = (cfg != null && cfg.name != null) ? cfg.name : nid;
            nodes.add(new NodeParcelable(nid, name));
        }

        CapabilityInfoParcelable capInfo = new CapabilityInfoParcelable(capabilityName, nodes);
        Intent capIntent = new Intent("com.google.android.gms.wearable.CAPABILITY_CHANGED",
                new Uri.Builder().scheme("wear").authority("").path(capabilityName).build());

        capIntent.setPackage(record.packageName);
        invokeListeners(capIntent, listener -> listener.onConnectedCapabilityChanged(capInfo));

    }

    public DataItemRecord putDataItem(DataItemRecord record) {
        boolean allAssetsPresent = true;
        for (Asset asset : record.dataItem.getAssets().values()) {
            String digest = asset.getDigest();
            if (digest != null && !assetFileExists(digest)) {
                Log.d(TAG, "Asset is missing: " + asset);
                allAssetsPresent = false;
                nodeDatabase.markAssetAsMissing(digest, record.packageName, record.signatureDigest);
            }
        }
        record.assetsAreReady = allAssetsPresent;

        nodeDatabase.putRecord(record);

        maybeDispatchCapabilityChanged(record);

        Intent intent = new Intent("com.google.android.gms.wearable.DATA_CHANGED");
        intent.setPackage(record.packageName);
        intent.setData(record.dataItem.uri);
        if (migrationController.shouldDeliverEvents(record.packageName, record.source)) {
            invokeListeners(intent, listener -> listener.onDataChanged(record.toEventDataHolder()));
        } else {
            Log.d(TAG, "Suppressing DATA_CHANGED for " + record.packageName
                    + " from migrating node " + record.source);
        }

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
        if (TextUtils.isEmpty(digest)) {
            throw new IllegalArgumentException("createAssetFile: digest must not be null or empty");
        }
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
            if (configuration.nodeId != null && configuration.nodeId.equals(nodeId)) return configuration;
        }

        return null;
    }

    public synchronized ConnectionConfiguration getConfigurationByPeerNodeId(String peerNodeId) {
        if (configurations == null) configurations = configDatabase.getAllConfigurations();
        for (ConnectionConfiguration c : configurations) {
            if (peerNodeId.equals(c.peerNodeId)) return c;
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

                        if (newConfiguration.peerNodeId == null && configuration.peerNodeId != null)
                            newConfiguration.peerNodeId = configuration.peerNodeId;

                        if (!newConfiguration.connected && configuration.connected)
                            newConfiguration.connected = true;

                        if (newConfiguration.nodeId == null && configuration.nodeId != null)
                            newConfiguration.nodeId = configuration.nodeId;

                        break;
                    }
                }
            }
            configurations = newConfigurations;
        }

        Map<String, String> peerToAddress = new HashMap<>();
        for (ConnectionConfiguration c : configurations) {
            if (c.peerNodeId != null && c.address != null) {
                peerToAddress.put(c.peerNodeId, c.address);
            }
        }

//        for (String activePeerId : activeConnections.keySet()) {
//            String addr = peerToAddress.get(activePeerId);
//            if (addr == null) continue;
//            for (ConnectionConfiguration c : configurations) {
//                if (addr.equalsIgnoreCase(c.address) && !c.connected) {
//                    c.connected = true;
//                    c.peerNodeId = activePeerId;
//                }
//            }
//        }

        for (Map.Entry<String, WearableConnection> e : activeConnections.entrySet()) {
            String activePeerId = e.getKey();
            String addr = peerToAddress.get(activePeerId);
            if (addr != null) {
                for (ConnectionConfiguration c : configurations) {
                    if (addr.equalsIgnoreCase(c.address) && !c.connected) {
                        c.connected = true;
                        c.peerNodeId = activePeerId;
                    }
                }
                continue;
            }
            String remoteAddr = e.getValue().getRemoteAddress();
            if (remoteAddr == null) continue;
            for (ConnectionConfiguration c : configurations) {
                if (remoteAddr.equalsIgnoreCase(c.address) && !c.connected) {
                    Log.d(TAG, "getConfigurations: live-overlay first-connect"
                            + " addr=" + remoteAddr + " peer=" + activePeerId);
                    c.connected = true;
                    c.peerNodeId = activePeerId;
                }
            }
        }

        // companion app crash if name is null
        // name can be null due failed pair (maybe),
        // or maybe i something broke,
        // or not setting name properly somewhere
        // so we just set address as name
        for (int i = 0; i < configurations.length; i++) {
            ConnectionConfiguration c = configurations[i];
            if (c.name == null || c.name.isEmpty() || "null".equals(c.name)) {
                String fallbackName = (c.address != null) ? c.address : "Unknown";
                configurations[i] = new ConnectionConfiguration(
                        fallbackName, c.address, c.type, c.role, c.enabled,
                        c.connected, c.peerNodeId, c.btlePriority,
                        c.nodeId, c.packageName, c.connectionRetryStrategy,
                        c.allowedConfigPackages, c.migrating,
                        c.dataItemSyncEnabled, c.connectionRestrictions,
                        c.removeConnectionWhenBondRemovedByUser,
                        c.connectionDelayFilters,
                        c.maxSupportedRemoteAndroidSdkVersion, c.runtimeType);

            }
        }


        Log.d(TAG, "Configurations reported: " + Arrays.toString(configurations));
        return configurations;
    }

    private void addConnectedNode(Node node) {
        Runnable pending = pendingDisconnects.remove(node.getId());
        if (pending != null) {
            disconnectDebounceHandler.removeCallbacks(pending);
            Log.d(TAG, "addConnectedNode: cancelled debounced disconnect for " + node.getId());
        }
        connectedNodes.add(node);
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    private void removeConnectedNode(String nodeId) {
        for (Node connectedNode : new ArrayList<Node>(connectedNodes)) {
            if (connectedNode.getId().equals(nodeId))
                connectedNodes.remove(connectedNode);
        }

        List<NodeParcelable> nowNodes = getConnectedNodesParcelableList();
        if (!nowNodes.isEmpty()) {
            onConnectedNodes(nowNodes);
            return;
        }

        Runnable r = () -> {
            pendingDisconnects.remove(nodeId);
            List<NodeParcelable> latestNodes = getConnectedNodesParcelableList();
            Log.d(TAG, "removeConnectedNode: debounce fired for " + nodeId
                    + ", current nodes=" + latestNodes);
            onConnectedNodes(latestNodes);
        };

        Runnable old = pendingDisconnects.put(nodeId, r);
        if (old != null)
            disconnectDebounceHandler.removeCallbacksAndMessages(old);

        disconnectDebounceHandler.postDelayed(r, NODE_DISCONNECT_DEBOUNCE_MS);
        Log.d(TAG, "removeConnectedNode: debouncing disconnect broadcast for "
                + nodeId + " (" + NODE_DISCONNECT_DEBOUNCE_MS + " ms)");
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
        if (migrationController.isNodeSuspended(nodeId)) {
            Log.d(TAG, "syncRecordToPeer: " + nodeId + " is suspended, deferring");
            return true;
        }

        WearableConnection connection = activeConnections.get(nodeId);
        if (connection == null) {
            Log.w(TAG, "Cannot sync to " + nodeId + " - connection not found");
            return false;
        }

        for (Asset asset : record.dataItem.getAssets().values()) {
            try {
                syncAssetToPeer(connection, record, asset);
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

    private void syncAssetToPeer(WearableConnection connection, DataItemRecord record, Asset asset) throws IOException {
        String digest = asset.getDigest();
        if (TextUtils.isEmpty(digest)) {
            Log.w(TAG, "syncAssetToPeer: skipping asset with empty name for record " + record.dataItem.uri);
            return;
        }

        RootMessage announceMessage = new RootMessage.Builder().setAsset(new SetAsset.Builder()
                .digest(digest)
                .appkeys(
                        new AppKeys(
                                Collections.singletonList(
                                        new AppKey(record.packageName, record.signatureDigest)
                                )
                        )
                ).build()).hasAsset(true).build();

        connection.writeMessage(announceMessage);

        File assetFile = createAssetFile(asset.getDigest());
        String filename = calculateDigest(announceMessage.encode());

        try (FileInputStream fis = new FileInputStream(assetFile)) {
            byte[] arr = new byte[12215];
            ByteString lastPiece = null;
            int c;
            while ((c = fis.read(arr)) > 0) {
                if (lastPiece != null) {
                    connection.writeMessage(
                            new RootMessage.Builder()
                                    .filePiece(
                                            new FilePiece(filename, false, lastPiece, null)
                                    ).build()
                    );
                }
                lastPiece = ByteString.of(arr, 0, c);
            }
            connection.writeMessage(
                    new RootMessage.Builder()
                            .filePiece(
                                    new FilePiece(filename, true, lastPiece, asset.getDigest())
                            ).build()
            );
        }
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

    public boolean assetFileExists(String digest) {
        if (digest == null) return false;
        File assetFile = createAssetFile(digest);
        return assetFile.exists();
    }

    public File createAssetReceiveTempFile(String name) {
        File dir = new File(context.getFilesDir(), "piece");
        dir.mkdirs();
        return new File(dir, name);
    }

    public void onConnectReceived(WearableConnection connection, String nodeId, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if ((config.nodeId != null && config.nodeId.equals(nodeId))
                    || (config.nodeId == null && connect.id.equals(config.peerNodeId))) {
                config.peerNodeId = connect.id;
                config.connected = true;
            }
        }

        String remoteAddr = connection.getRemoteAddress();
        if (remoteAddr != null) {
            boolean marked = false;
            for (ConnectionConfiguration c : getConfigurations()) {
                if (connect.id.equals(c.peerNodeId) && c.connected) {
                    marked = true;
                    break;
                }
            }
            if (!marked) {
                for (ConnectionConfiguration c : getConfigurations()) {
                    if (remoteAddr.equalsIgnoreCase(c.address)) {
                        Log.i(TAG, "onConnectReceived: address-match "
                                + " addr=" + remoteAddr + " peer=" + connect.id);
                        c.peerNodeId = connect.id;
                        c.connected = true;
                        break;
                    }
                }
            }
        }

        Log.d(TAG, "Adding connection to list of open connections: " + connection
                + " with connect " + connect);

        String btAddress = null;
        for (ConnectionConfiguration config : getConfigurations()) {
            if (connect.id.equals(config.peerNodeId) && config.address != null) {
                btAddress = config.address;
                break;
            }
        }

        if (btAddress != null) {
            Iterator<Map.Entry<String, WearableConnection>> connIter = activeConnections.entrySet().iterator();

            while (connIter.hasNext()) {
                Map.Entry<String, WearableConnection> e = connIter.next();

                if (e.getKey().equals(connect.id))
                    continue;

                for (ConnectionConfiguration c : getConfigurations()) {
                    if (e.getKey().equals(c.peerNodeId) && btAddress.equals(c.address)) {
                        connIter.remove();
                        break;
                    }
                }
            }

            for (Node n : connectedNodes) {
                if (n.getId().equals(connect.id))
                    continue;

                for (ConnectionConfiguration c : getConfigurations()) {
                    if (n.getId().equals(c.peerNodeId) && btAddress.equals(c.address)) {
                        connIter.remove();
                        break;
                    }
                }
            }
        }

        activeConnections.put(connect.id, connection);

        onPeerConnected(new NodeParcelable(connect.id, connect.name, 0, true));

        DataTransport dt = peerTransports.get(connect.id);


        Handler h = networkHandler;
        if (h != null && h.getLooper().getThread().isAlive()) {
            h.postDelayed(() -> {
                if (activeConnections.containsKey(connect.id)) {
                    fetchMissingAssets(connect.id);
                } else {
                    Log.d(TAG, "Connection closed before asset fetch could start");
                }
            }, 5000);
        } else {
            Log.w(TAG, "networkHandler is dead, skipping asset fetch scheduling for " + connect.id);
        }
    }

    private void fetchMissingAssets(String nodeId) {
        long now = System.currentTimeMillis();
        long timeSinceLastFetch = now - lastAssetFetchTime;
        if (timeSinceLastFetch < ASSET_FETCH_COOLDOWN_MS) {
            long delay = ASSET_FETCH_COOLDOWN_MS - timeSinceLastFetch;
            networkHandler.postDelayed(() -> doFetchMissingAssets(nodeId), delay);
            return;
        }

        doFetchMissingAssets(nodeId);
        lastAssetFetchTime = now;
    }

    private void doFetchMissingAssets(String nodeId) {
        WearableConnection connection = activeConnections.get(nodeId);

        assetFetcher.fetchMissingAssets(nodeId, connection, activeConnections, channelManager);
    }

    public AssetFetcher getAssetFetcher() {
        return assetFetcher;
    }

    public void onDisconnectReceived(WearableConnection connection, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if (connect.id.equals(config.peerNodeId) || config.nodeId.equals(connect.id)) {
                config.connected = false;
            }
        }
        Log.d(TAG, "Removing connection from list of open connections: " + connection);

        if (channelManager != null)
            channelManager.sendCloseForAllChannels(connect.id);

        activeConnections.remove(connect.id);

        DataTransport dt = peerTransports.remove(connect.id);
        if (dt != null) {
            dt.onDisconnect();
        }

        assetManager.removeWriter(connect.id);

        if (channelManager != null) {
            channelManager.onNodeDisconnected(connect.id);
        }

        onPeerDisconnected(new NodeParcelable(connect.id, connect.name));
    }

    public List<NodeParcelable> getConnectedNodesParcelableList() {
        List<NodeParcelable> nodes = new ArrayList<>();
        for (Node connectedNode : connectedNodes) {
            nodes.add(new NodeParcelable(connectedNode));
        }

        for (String peerId : new ArrayList<>(activeConnections.keySet())) {
            boolean seen = false;
            for (NodeParcelable n : nodes) {
                if (n.getId().equals(peerId)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                ConnectionConfiguration cfg = getConfigurationByPeerNodeId(peerId);
                String name = (cfg != null && cfg.name != null) ? cfg.name : peerId;
                nodes.add(new NodeParcelable(peerId, name, 0, true));
            }
        }

        return nodes;
    }

    public void invokeListeners(@Nullable Intent intent, ListenerInvoker invoker) {
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
        Uri uri = new Uri.Builder().scheme("wear").authority(node.getId()).build();
        Intent intent = new Intent("com.google.android.gms.wearable.NODE_CHANGED", uri);
        invokeListeners(intent, listener -> listener.onPeerConnected(node));
        addConnectedNode(node);
    }

    public void onPeerDisconnected(NodeParcelable node) {
        Log.d(TAG, "onPeerDisconnected: " + node);
        Uri uri = new Uri.Builder().scheme("wear").authority(node.getId()).build();
        Intent intent = new Intent("com.google.android.gms.wearable.NODE_CHANGED", uri);
        invokeListeners(intent, listener -> listener.onPeerDisconnected(node));
        removeConnectedNode(node.getId());
    }

    public void onConnectedNodes(List<NodeParcelable> nodes) {
        Log.d(TAG, "onConnectedNodes: " + nodes);
//        invokeListeners(null, listener -> listener.onConnectedNodes(nodes));
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

    public Set<String> getNodesByCapabilityPath(String pathPrefix, String capabilityName) {
        Set<String> nodes = new HashSet<>();
        Cursor cursor = nodeDatabase.getDataItemsByCapabilityName(capabilityName);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    DataItemRecord r = DataItemRecord.fromCursor(cursor);
                    if (!r.deleted && r.dataItem.host != null
                            && r.dataItem.path != null
                            && r.dataItem.path.endsWith("/" + capabilityName)) {
                        nodes.add(r.dataItem.host);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return nodes;
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
        List<ListenerInfo> list = listeners.get(packageName);
        if (list == null) {
            list = new ArrayList<>();
            listeners.put(packageName, list);
        }

        IBinder incoming = listener.asBinder();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).listener.asBinder().equals(incoming)) {
                list.set(i, new ListenerInfo(listener, filters));
                return;
            }
        }

        ListenerInfo info = new ListenerInfo(listener, filters);
        list.add(info);

        try {
            listener.asBinder().linkToDeath(() -> removeListener(listener), 0);
        } catch (RemoteException e) {
            list.remove(info);
        }
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

        if (configurationsUpdated) {
            getConfigurations();
        }

        ConnectionConfiguration existing = getConfigurationByAddress(config.address);
        if (existing != null) {
            Log.d(TAG, "Config already exists for address " + config.address + ", updating");

            if (config.peerNodeId == null && existing.peerNodeId != null)
                config.peerNodeId = existing.peerNodeId;

            if (!config.connected && existing.connected)
                config.connected = existing.connected;

            if (config.nodeId == null && existing.nodeId != null)
                config.nodeId = existing.nodeId;

            configDatabase.putConfiguration(config);
            configurationsUpdated = true;

            if (configurations != null) {
                for (int i = 0; i < configurations.length; i++) {
                    if (config.address != null
                            && config.address.equalsIgnoreCase(configurations[i].address)) {
                        configurations[i] = config;
                        break;
                    }
                }
            }

//            if (config.name != null && !config.name.isEmpty() && !"null".equals(config.name)
//                    && !config.name.equals(config.address)) {
//                existing.name = config.name;
//            }
//            existing.enabled = config.enabled;
//            if (existing.nodeId == null && config.nodeId != null) {
//                existing.nodeId = config.nodeId;
//            }
//            configDatabase.putConfiguration(existing);
//            configurationsUpdated = true;
            return;
        }

        Log.d(TAG, "putConfig[new]: " + config);
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
        if (bleManager == null) {
            bleManager = new BleManager(context, this);
        }
        if (enabled) {
            bleManager.enable(config);
        } else {
            bleManager.disable(config);
        }
    }

    private void handleNetwork(ConnectionConfiguration config, boolean enabled) {
        if (networkManager == null) {
            networkManager = new NetworkConnectionManager(context, this);
        }

        if (enabled) {
            networkManager.addConfig(config);
        } else {
            networkManager.removeConfig(config);
        }
    }

    private void handleLegacy(ConnectionConfiguration config, boolean enabled) {
        try {
            if (config.role == ROLE_CLIENT) {
                if (enabled) {
                    networkHandlerLock.await();
                    networkHandler.post(() -> {
                        if (bluetoothClient == null) {
                            Log.d(TAG, "Initializing BluetoothClient");
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
                Log.w(TAG, "Bluetooth role Server not implemented");
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while handling Bluetooth", e);
        }
    }

    public int deleteDataItems(Uri uri, String packageName) {
        List<DataItemRecord> records = nodeDatabase.deleteDataItems(packageName, PackageUtils.firstSignatureDigest(context, packageName), fixHost(uri.getHost(), false), uri.getPath());
        for (DataItemRecord record : records) {
            syncRecordToAll(record);
        }
        return records.size();
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
        WearableConnection connection = activeConnections.remove(nodeId);
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e1) {
                Log.w(TAG, "Error closing connection", e1);
            }
        }


        if (sct != null && connection == sct.getWearableConnection()) {
            sct.close();
            sct = null;
        }

        activeConnections.remove(nodeId);

        String name = "Wear device";
        for (ConnectionConfiguration config : getConfigurations()) {
            if (nodeId.equals(config.nodeId) || nodeId.equals(config.peerNodeId)) {
                config.connected = false;
                name = config.name;
            }
        }

        if (connection != null) {
            onPeerDisconnected(new NodeParcelable(nodeId, name));
            Log.d(TAG, "Closed connection to " + nodeId + " on error");
        }
    }

    public void sendControlMessage(String nodeId, int type) {
        WearableConnection connection = activeConnections.get(nodeId);
        if (connection == null) {
            Log.w(TAG, "sendControlMessage: no connection to=" + nodeId + " type=" + type);
            return;
        }

        try {
            connection.writeMessage(
                    new RootMessage.Builder().controlMessage(
                            new ControlMessage.Builder().type(type).build()
                    ).build()
            );
            Log.d(TAG, "sendControlMessage: type=" + type + " nodeId=" + nodeId);
        } catch (IOException e) {
            Log.w(TAG, "sendControlMessage: failed for " + nodeId + " type=" + type + ": " + e.getMessage());
        }
    }

    public void onMigrationSucceeded(String newNodeId, ConnectionConfiguration migratingConfig) {
        Log.i(TAG, "onMigrationSucceeded: newNode=" + newNodeId);

        migrationTracker.setMigrationComplete(newNodeId);
        migrationController.onMigrationSucceeded();
        migratingConfig.migrating = false;
        updateConfiguration(migratingConfig);

        try {
            String localNodeId = getLocalNodeId();
            String gmsSignature = "38918a453d07199354f8b19af05ec6562ced5788"; // hardcoded in gms
            DataItemInternal marker = new DataItemInternal(localNodeId, "/setup/sync_marker/" + newNodeId);
            marker.data = new byte[0];
            DataItemRecord markerRecord = putDataItem(
                    "com.google.android.gms", gmsSignature, localNodeId, marker);
            syncRecordToAll(markerRecord);
            Log.d(TAG, "onMigrationSucceeded: sync marker written for node=" + newNodeId);
        } catch (Exception e) {
            Log.w(TAG, "onMigrationSucceeded: failed to write sync marker", e);
        }


        DataTransport dt = peerTransports.get(newNodeId);
        if (dt != null) {
            dt.sendSyncStart();
        } else {
            // fallback, in ideal we dont want this to call
            Log.w(TAG, "onMigrationSucceeded: no DataTransport for " + newNodeId);
//            syncToPeer(newNodeId, getLocalNodeId(), -1L);
        }

        Log.d(TAG, "Migration to " + newNodeId + " complete");
    }

    public void onMigrationFailed(String newNodeId, boolean sendMsg) {
        Log.w(TAG, "onMigrationFailed: node=" + newNodeId + " sendMsg=" + sendMsg);

        if (sendMsg && newNodeId != null) {
            sendControlMessage(newNodeId, NodeMigrationController.CTRL_MIGRATION_FAILED);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        if (newNodeId != null) {
            migrationTracker.clearMigrationInfo(newNodeId);
            nodeDatabase.deleteAllItemsBySourceNode(newNodeId);
        }

        String oldNodeId = migrationController.getMigratingFromNodeId();
        migrationController.onMigrationAborted();

        if (newNodeId != null) {
            closeConnection(newNodeId);
        }

        if (oldNodeId != null) {
            migrationController.resumeNode(oldNodeId);
            sendControlMessage(oldNodeId, NodeMigrationController.CTRL_RESUME_SYNC);
            Log.d(TAG, "onMigrationFailed: cleared suspension for old node=" + oldNodeId);
        }

        Log.d(TAG, "Migration failed: newNode=" + newNodeId + " oldNode=" + oldNodeId);
    }

    public void triggerResync(String nodeId) {
        Log.d(TAG, "triggerResync: " + nodeId);
        DataTransport dt = peerTransports.get(nodeId);
        if (dt != null) {
            dt.sendSyncStart();
//        } else {
//            syncToPeer(nodeId, getLocalNodeId(), -1L);
        }
    }

    public void unregisterPeerTransport(String peerNodeId) {
        peerTransports.remove(peerNodeId);
        assetManager.removeWriter(peerNodeId);
        Log.d(TAG, "unregisterPeerTransport: " + peerNodeId);
    }

    public int sendMessage(String packageName, String targetNodeId, String path, byte[] data, MessageOptions options) {
        if (activeConnections.containsKey(targetNodeId)) {
            ConnectionConfiguration cfg = getConfigurationByPeerNodeId(targetNodeId);
            if (cfg == null)
                cfg = getConfigurationByNodeId(targetNodeId);

            if (cfg != null
                    && cfg.packageName != null
                    && packageName != null
                    && !packageName.equals(cfg.packageName)) {
                Log.w(TAG, "sendMessage: refusing delivery of " + path
                        + " to node " + targetNodeId + " - owned by " + cfg.packageName
                        + ", caller is " + packageName);
                return -1;
            }

            WearableConnection connection = activeConnections.get(targetNodeId);
            return writeToConnection(packageName, targetNodeId, connection, path, data);
        }

        if ("*".equals(targetNodeId)) {
            int lastResult = -1;
            for (Map.Entry<String, WearableConnection> e : activeConnections.entrySet()) {
                boolean nearby = false;
                for (Node n : connectedNodes) {
                    if (n.getId().equals(e.getKey()) && n.isNearby()) {
                        nearby = true;
                        break;
                    }
                }
                if (nearby) {
                    lastResult = writeToConnection(packageName, e.getKey(),
                            e.getValue(), path, data);
                }
            }
            return lastResult;
        }

        Log.d(TAG, targetNodeId + " not reachable");
        return -1;
    }

    private int writeToConnection(String packageName, String nodeId, WearableConnection connection, String path, byte[] data) {
        PendingRpcRequest pending = consumePendingRpcRequest(nodeId, path);
        if (pending != null) {
            RpcHelper.RpcConnectionState state = rpcHelper.useConnectionState(packageName, nodeId, path);
            try {
                pending.connection.writeMessage(new RootMessage.Builder()
                        .rpcRequest(new Request.Builder()
                                .targetNodeId(nodeId)
                                .path(path)
                                .rawData(ByteString.of(data))
                                .packageName(pending.packageName)
                                .signatureDigest(PackageUtils.firstSignatureDigest(context, pending.packageName))
                                .sourceNodeId(getLocalNodeId())
                                .generation(pending.gen)
                                .requestId(state.lastRequestId)
                                .requiresResponse(false)
                                .senderRequestId(pending.reqId)
                                .build()).build());
                Log.d(TAG, "writeToConnection: sent RPC response"
                        + " senderRequestId=" + pending.reqId
                        + " path=" + path + " peer=" + nodeId);
            } catch (IOException e) {
                Log.w(TAG, "writeToConnection: failed to write RPC response for " + path, e);
                closeConnection(nodeId);
                return -1;
            }
            return (state.generation + 527) * 31 + state.lastRequestId;
        }

        return writeToConnectionWithoutPending(packageName, nodeId, connection, path, data);
    }

    private int writeToConnectionWithoutPending(String packageName, String nodeId, WearableConnection connection, String path, byte[] data) {
        RpcHelper.RpcConnectionState state = rpcHelper.useConnectionState(packageName, nodeId, path);
        try {
            connection.writeMessage(new RootMessage.Builder().rpcRequest(new Request.Builder()
                    .targetNodeId(nodeId)
                    .path(path)
                    .rawData(ByteString.of(data))
                    .packageName(packageName)
                    .signatureDigest(PackageUtils.firstSignatureDigest(context, packageName))
                    .sourceNodeId(getLocalNodeId())
                    .generation(state.generation)
                    .requestId(state.lastRequestId)
                    .requiresResponse(true)
                    .build()).build());
        } catch (IOException e) {
            Log.w(TAG, "Error while writing, closing link", e);
            closeConnection(nodeId);
            return -1;
        }
        return (state.generation + 527) * 31 + state.lastRequestId;
    }

    public int sendRequest(String packageName, String targetNodeId, String path, byte[] data, MessageOptions options) {
        return -1;
    }

    public void stop() {
        if (channelManager != null) {
            channelManager.stop();
        }
        if (networkHandlerThread != null) {
            networkHandlerThread.quitSafely();
        }
    }

    public void storePendingRpcRequest(PendingRpcRequest req) {
        pendingRpcRequests.put(req.peerNodeId + ":" + req.path, req);
        Log.d(TAG, "storePendingRpcRequest: requestId=" + req.reqId
                + " path=" + req.path + " peer=" + req.peerNodeId);
    }

    public PendingRpcRequest consumePendingRpcRequest(String targetNodeId, String path) {
        return pendingRpcRequests.remove(targetNodeId + ":" + path);
    }

    public void clearPendingRpcRequestsForPeer(String peerNodeId) {
        Iterator<Map.Entry<String, PendingRpcRequest>> it =
                pendingRpcRequests.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, PendingRpcRequest> e = it.next();

            if (e.getKey().startsWith(peerNodeId + ":")) {
                it.remove();
            }
        }
    }

    public NodeDatabaseHelper getNodeDatabase() {
        return nodeDatabase;
    }

    public ClockworkNodePreferences getClockworkNodePreferences() {
        return clockworkNodePreferences;
    }

    public NodeMigrationController getMigrationController() {
        return migrationController;
    }

    public RpcHelper getRpcHelper() {
        return rpcHelper;
    }


    interface ListenerInvoker {
        void invoke(IWearableListener listener) throws RemoteException;
    }

    public static final class PendingRpcRequest {
        public final int reqId;
        public final int gen;
        public final String path;
        public final String peerNodeId;
        public final String packageName;
        public final WearableConnection connection;

        public PendingRpcRequest(int requestId, int generation, String path,
                                 String peerNodeId, String packageName,
                                 WearableConnection connection) {
            this.reqId = requestId;
            this.gen = generation;
            this.path = path;
            this.peerNodeId = peerNodeId;
            this.packageName = packageName;
            this.connection = connection;
        }
    }

    private class WearableChannelCallbacks implements ChannelCallbacks {
        @Override
        public void onChannelOpened(ChannelToken token, String path) {
            Log.d(TAG, "onChannelOpened: " + token + ", path=" + path);
            ChannelParcelable channel = token.toParcelable(path);

            Intent intent = new Intent("com.google.android.gms.wearable.CHANNEL_EVENT");
            if (token.thisNodeWasOpener)
                intent.setPackage(token.appKey.packageName);
            intent.setData(new Uri.Builder().scheme("wear").authority("").path(path).build());

            invokeListeners(intent, listener -> {
                try {
                    ChannelEventParcelable event = new ChannelEventParcelable(
                            channel, ChannelEventParcelable.EVENT_TYPE_CHANNEL_OPENED,
                            0, 0);
                    listener.onChannelEvent(event);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying listener of channel opened", e);
                }
            });
        }

        @Override
        public void onChannelClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelClosed: " + token + ", reason=" + closeReason);
            ChannelParcelable channel = token.toParcelable(path);

            Intent intent = new Intent("com.google.android.gms.wearable.CHANNEL_EVENT");
            if (token.thisNodeWasOpener)
                intent.setPackage(token.appKey.packageName);
            intent.setData(new Uri.Builder().scheme("wear").authority("").path(path).build());

            invokeListeners(intent, listener -> {
                try {
                    ChannelEventParcelable event = new ChannelEventParcelable(
                            channel, ChannelEventParcelable.EVENT_TYPE_CHANNEL_CLOSED,
                            closeReason, errorCode);
                    listener.onChannelEvent(event);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying listener of channel closed", e);
                }
            });
        }

        @Override
        public void onChannelInputClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelInputClosed: " + token);
            ChannelParcelable channel = token.toParcelable(path);

            Intent intent = new Intent("com.google.android.gms.wearable.CHANNEL_EVENT");
            if (token.thisNodeWasOpener)
                intent.setPackage(token.appKey.packageName);
            intent.setData(new Uri.Builder().scheme("wear").authority("").path(path).build());

            invokeListeners(intent, listener -> {
                try {
                    ChannelEventParcelable event = new ChannelEventParcelable(
                            channel, ChannelEventParcelable.EVENT_TYPE_INPUT_CLOSED,
                            closeReason, errorCode);
                    listener.onChannelEvent(event);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying listener of input closed", e);
                }
            });

        }

        @Override
        public void onChannelOutputClosed(ChannelToken token, String path, int closeReason, int errorCode) {
            Log.d(TAG, "onChannelOutputClosed: " + token);
            ChannelParcelable channel = token.toParcelable(path);

            Intent intent = new Intent("com.google.android.gms.wearable.CHANNEL_EVENT");
            if (token.thisNodeWasOpener)
                intent.setPackage(token.appKey.packageName);
            intent.setData(new Uri.Builder().scheme("wear").authority("").path(path).build());

            invokeListeners(intent, listener -> {
                try {
                    ChannelEventParcelable event = new ChannelEventParcelable(
                            channel, ChannelEventParcelable.EVENT_TYPE_OUTPUT_CLOSED, closeReason, errorCode);
                    listener.onChannelEvent(event);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying listener of output closed", e);
                }
            });

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
