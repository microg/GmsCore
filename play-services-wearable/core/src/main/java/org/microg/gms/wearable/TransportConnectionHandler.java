package org.microg.gms.wearable;

import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.wearable.bluetooth.BluetoothWearableConnection;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.ControlMessage;
import org.microg.gms.wearable.proto.MessagePiece;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransportConnectionHandler {
    private static final String TAG = "WearTransportHandler";

    private static final long CTRL_FLUSH_DELAY_MS = 120;

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
        boolean migrationSucceeded = false;

        try {
            if (config.address != null){
                try {
                    wearable.updateConfiguration(config);
                    Log.d(TAG, "Persisted address " + config.address);
                } catch (Exception e) {
                    Log.w(TAG, "Could not persist address:" + e.getMessage());
                }
            }

            NodeMigrationController migrationController = wearable.getMigrationController();

            if (config.migrating) {
                String fromNode = wearable.getClockworkNodePreferences().getPeerNodeId();
                if (!migrationController.startPhoneSwitchMigration(
                        config.nodeId != null ? config.nodeId : "?", fromNode
                )) {
                    Log.e(TAG, "handle: Already migrating - aborting...");
                    return;
                }

                if (config.nodeId != null) {
                    migrationController.startMigrationForNode(config.nodeId);
                }
                ownsMigrationSlot.set(true);
                Log.i(TAG, "handle: Migration slot acquired for "+ config.nodeId);
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
            handler.markHandshakeAlreadyDone();

            writer = new WearableWriter(peerNodeId, connection);

            WearableConnection writerFacade = buildWriterFacade(writer, peerNodeId);

            reader = new WearableReader(peerNodeId, connection, writerFacade, handler);

            Connect peerConnect = connection.getPeerConnect();

            wearable.registerPeerWriter(peerNodeId, writer);
            if (peerConnect != null) {
                wearable.onConnectReceived(connection, config.nodeId, peerConnect);
            } else {
                Connect synthetic = new Connect.Builder()
                        .id(peerNodeId)
                        .name(peerNodeName != null ? peerNodeName : peerNodeId)
                        .build();
                wearable.onConnectReceived(connection, config.nodeId, synthetic);
            }

            if (config.migrating) {
                wearable.onMigrationSucceeded(peerNodeId, config);
                migrationSucceeded = true;
            }

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
                DataTransport dt = wearable.getDataTransport(peerNodeId);
                if (dt != null) {
                    dt.onDisconnect();
                }
                wearable.unregisterPeerTransport(peerNodeId);

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
                if (!migrationSucceeded) {
                    String failNode = peerNodeId != null ? peerNodeId : config.nodeId;
                    wearable.onMigrationFailed(failNode, true);
                }

                if (peerNodeId != null) {
                    wearable.getMigrationController().markNodeMigrationCompleted(peerNodeId);
                }

                Log.d(TAG, "handle: Migration slot released for " + config.nodeId
                        + " (succeeded=" + migrationSucceeded + ")");
            }

            Log.d(TAG, "finished for " + config.address);
        }
    }

    private void suspendOldConnections(String peerNodeId) {
        Log.d(TAG, "Suspending old connections before migrating to " + peerNodeId);

        NodeMigrationController ctrl = wearable.getMigrationController();
        Map<String, WearableConnection> active = wearable.getActiveConnections();

        for (String existing : new ArrayList<>(active.keySet())) {
            if (existing.equals(peerNodeId)) continue;

            WearableConnection connection = active.get(existing);
            if (connection == null) continue;

            Log.d(TAG, "Suspending old node " + existing);

            ctrl.suspendNode(existing);

            try {
                connection.writeMessage(buildControlMessage(NodeMigrationController.CTRL_SUSPEND_SYNC));
            } catch (IOException e) {
                Log.w(TAG, "suspendOldConnections: SUSPEND_SYNC to " + existing
                        + " failed: " + e.getMessage());
            }

            try {
                connection.writeMessage(buildControlMessage(NodeMigrationController.CTRL_TERMINATE_ASSOCIATION));
            } catch (IOException e) {
                Log.w(TAG, "suspendOldConnections: TERMINATE_ASSOCIATION to " + existing
                        + " failed: " + e.getMessage());
            }

            try {
                Thread.sleep(CTRL_FLUSH_DELAY_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            try {
                connection.close();
            } catch (IOException e) {
                Log.w(TAG, "suspendOldConnections: close error for" + existing +
                        ": " + e.getMessage());
            }

            active.remove(existing);
            Log.d(TAG, "suspendOldConnections: " + existing + " suspended adn connection closed");
        }
    }

    private static RootMessage buildControlMessage(int type) {
        return new RootMessage.Builder()
                .controlMessage(
                        new ControlMessage.Builder().type(type).build()
                ).build();
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
