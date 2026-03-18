package org.microg.gms.wearable;

import android.content.OperationApplicationException;
import android.util.Log;

import com.google.android.gms.wearable.Wearable;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;

public class ConnectHandshake {
    private static final String TAG = "WearConnectHandshake";

    public static final int PEER_VERSION = 1; // gms sends 1 and so we are
    public static final int PEER_MIN_VERSION = 0;

    private ConnectHandshake() {}

    public static Connect perform(
            WearableConnection connection,
            String localNodeId, String localNodeName,
            long androidId, boolean isMigrating,
            String migratingFrom
    ) throws IOException {
        sendLocalConnect(connection, localNodeId, localNodeName,
                androidId, isMigrating, migratingFrom);

        return readAndValidatePeerConnect(connection, isMigrating);
    }

    public static Connect perform(
            WearableConnection connection,
            String localNodeId, String localNodeName,
            long androidId
    ) throws IOException {
        return perform(connection, localNodeId, localNodeName,
                androidId, false, null);
    }

    private static void sendLocalConnect(
            WearableConnection connection,
            String localNodeId, String localNodeName,
            long androidId, boolean isMigrating,
            String migratingFrom
    ) throws IOException {
        Connect.Builder cb = new Connect.Builder()
                .id(localNodeId)
                .name(localNodeName != null ? localNodeName : localNodeId)
                .peerAndroidId(androidId)
                .unknown4(3)
                .peerVersion(PEER_VERSION)
                .peerMinimumVersion(PEER_MIN_VERSION)
                .androidSdkVersion(Build.VERSION.SDK_INT);

        if (isMigrating) {
            cb.migrating(true);
            if (migratingFrom != null && !migratingFrom.isEmpty()) {
                cb.migratingFromNodeId(migratingFrom);
            }
        }

        connection.writeMessage(new RootMessage.Builder().connect(cb.build()).build());
        Log.d(TAG, "sendLocalConnect: localNodeId=" + localNodeId +
                " version=" + PEER_VERSION + "/" + PEER_MIN_VERSION +
                " sdkInt=" + Build.VERSION.SDK_INT + " migrating=" + isMigrating);
    }

    private static Connect readAndValidatePeerConnect(
            WearableConnection connection, boolean isMigrating
    ) throws IOException {
        RootMessage incoming = connection.readMessage();
        if (incoming == null) {
            throw new IOException("Null message during Connect handshake");
        }
        if (incoming.connect == null) {
            throw new IOException("Peer did not start with Connect message, got: " + incoming);
        }

        Connect peer = incoming.connect;

        if (peer.id == null || peer.id.isEmpty()) {
            throw new IOException("Peer sent an empty node id in Connect message");
        }

        checkVersionCompatibility(peer);
        checkMigrationParity(peer, isMigrating);

        connection.setPeerConnect(peer);

        Log.d(TAG, "readAndValidatePeerConnect: handshake complete, peerNodeId=" + peer.id +
                " peerName=" + peer.name + " peerVersion=" + peer.peerVersion + "/" + peer.peerMinimumVersion);
        return peer;
    }

    private static void checkVersionCompatibility(Connect peer) throws IOException {
        int peerVer = peer.peerVersion != null ? peer.peerVersion : 0;
        int peerMinVer = peer.peerMinimumVersion != null ? peer.peerMinimumVersion : 0;

        boolean incompatible = (peerVer <= 0) ? (peerVer < 0) : (peerMinVer > PEER_VERSION);

        if (incompatible) {
            throw new IOException("Protocol version mismatch - version=" + PEER_VERSION
                    + "min=" + PEER_MIN_VERSION + "; peer version=" + peerVer + " min=" + peerMinVer);
        }
    }

    private static void checkMigrationParity(Connect peer, boolean migrating) throws IOException {
        boolean peerMigrating = Boolean.TRUE.equals(peer.migrating);
        if (migrating != peerMigrating) {
            throw new IOException("isMigrating state mismatch: local=" + migrating
                    + " peer=" + peerMigrating);
        }
    }
}
