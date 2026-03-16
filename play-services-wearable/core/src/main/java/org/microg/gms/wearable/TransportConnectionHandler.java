package org.microg.gms.wearable;

import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.wearable.bluetooth.BluetoothWearableConnection;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.MessagePiece;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransportConnectionHandler {
    private static final String TAG = "WearTransportHandler";

    private final WearableImpl wearable;
    private final ConnectionConfiguration config;

    private final AtomicBoolean ownsMigrationSlot = new AtomicBoolean(false);

    public TransportConnectionHandler(WearableImpl wearable, ConnectionConfiguration config) {
        this.wearable = wearable;
        this.config = config;
    }

    public void handle(WearableConnection connection) {
        String peerNodeId = null;
        String peerNodeName = null;
        WearableReader reader = null;
        WearableWriter writer = null;

        try {
            if (config.address != null){
                try {
                    wearable.updateConfiguration(config);
                    Log.d(TAG, "Persisted BT address " + config.address);
                } catch (Exception e) {
                    Log.w(TAG, "Could not persist BT address:" + e.getMessage());
                }
            }

            NodeMigrationController migrationController = wearable.getMigrationController();

            if (config.migrating) {
                if (migrationController.isMigrating(config.nodeId)) {
                    Log.e(TAG, "Already migrating to " + config.nodeId);
                    return;
                }

                migrationController.startMigrationForNode(config.nodeId);
                ownsMigrationSlot.set(true);
                Log.i(TAG, "Migration slot aquired for "+ config.nodeId);
            }

            if (connection instanceof BluetoothWearableConnection) {
                Connect peerConnect = connection.getPeerConnect();
                if (peerConnect != null) {
                    peerNodeId = peerConnect.id;
                    peerNodeName = peerConnect.name;
                } else {
                    peerNodeId = config.peerNodeId != null ? config.peerNodeId : config.nodeId;
                    peerNodeName = config.name;
                }
            }

            if (peerNodeId == null || peerNodeId.isEmpty()) {
                Log.e(TAG, "Cannot resolve peer node-id, aborting");
                return;
            }

            Log.d(TAG, "Handling connection to peer " + peerNodeId + " (" + peerNodeName + ")");

            config.peerNodeId = peerNodeId;
            config.connected = true;

            try {
                wearable.updateConfiguration(config);
            } catch (Exception e) {
                Log.w(TAG, "Could not update config nodeId: " + e.getMessage());
            }

            if (config.migrating) {
                suspendOldConnections(peerNodeId);
            }

            Log.d(TAG, "Setting up writer/reader for " + peerNodeId);

            MessageHandler handler = new MessageHandler(wearable.getContext(), wearable, config);

            writer = new WearableWriter(peerNodeId, connection);

            WearableConnection writerFacade = buildWriterFacade(writer, peerNodeId);

            reader = new WearableReader(peerNodeId, connection, writerFacade, handler);

            Connect peerConnect = connection.getPeerConnect();

            if (peerConnect != null) {
                wearable.onConnectReceived(connection, config.nodeId, peerConnect);
            } else {
                Connect synthetic = new Connect.Builder()
                        .id(peerNodeId)
                        .name(peerNodeName != null ? peerNodeName : peerNodeId)
                        .build();
                wearable.onConnectReceived(connection, config.nodeId, synthetic);
            }

            wearable.registerPeerWriter(peerNodeId, writer);

            Log.d(TAG, "Starting writer for " + peerNodeId);
            writer.start();

            Log.d(TAG, "Starting reader for " + peerNodeId);
            reader.start();

            Log.d(TAG, "Blocking until network loop finishes for " + peerNodeId);
            reader.awaitFinished();
            Log.i(TAG, "Network loop finished for " + peerNodeId);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error for " + config.address, e);
        } finally {
            if (writer != null) {
                writer.close();
                writer.awaitFinished();
            }

            try {
                connection.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing connection: " + e.getMessage());
            }

            if (peerNodeId != null) {
                wearable.getActiveConnections().remove(peerNodeId);
                for (ConnectionConfiguration cfg : wearable.getConfigurations()) {
                    if (peerNodeId.equals(cfg.peerNodeId) || peerNodeId.equals(cfg.nodeId)) {
                        cfg.connected = false;
                    }
                }

                config.peerNodeId = null;
                config.connected = false;

                String displayName = (peerNodeName != null && !peerNodeName.isEmpty() ? peerNodeName : peerNodeId);

                wearable.onPeerDisconnected(new NodeParcelable(peerNodeId, displayName));
            }

            if (ownsMigrationSlot.getAndSet(false)) {
                wearable.getMigrationController()
                        .markNodeMigrationCompleted(config.nodeId);
                Log.d(TAG, "Migration slot released for " + config.nodeId);
            }

            Log.d(TAG, "finished for " + config.address);
        }
    }

    private void suspendOldConnections(String peerNodeId) {
        Log.d(TAG, "Suspending old connections before migrating to " + peerNodeId);

        Map<String, WearableConnection> active = wearable.getActiveConnections();
        for (String existing : new ArrayList<>(active.keySet())) {
            if (existing.equals(peerNodeId)) continue;

            Log.d(TAG, "Suspending old node " + existing);

            try {
                active.get(existing).close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing old connection to " + existing + ": " + e.getMessage());
            }

            active.remove(existing);
        }
    }

    private static WearableConnection buildWriterFacade(WearableWriter writer, String nodeId) {
        return new WearableConnection(new WearableConnection.Listener() {
            @Override
            public void onConnected(WearableConnection connection) {}

            @Override
            public void onMessage(WearableConnection connection, RootMessage message) {}

            @Override
            public void onDisconnected() {}
        }) {
            @Override
            public void writeMessage(RootMessage message) {
                writer.enqueue(message);
            }

            @Override
            protected void writeMessagePiece(MessagePiece piece) throws IOException {
                throw new UnsupportedOperationException("write-only facade for " + nodeId);
            }

            @Override
            protected MessagePiece readMessagePiece() throws IOException {
                throw new UnsupportedOperationException("write-only facade for " + nodeId);
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

}
