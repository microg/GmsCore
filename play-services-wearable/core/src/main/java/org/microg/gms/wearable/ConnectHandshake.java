package org.microg.gms.wearable;

import android.content.OperationApplicationException;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.Wearable;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;

public class ConnectHandshake {
    private static final String TAG = "WearConnectHandshake";

    public static final int PEER_VERSION = 1; // still not sure if 2 or 1, but set to what in gms(maybe)
    public static final int PEER_MIN_VERSION = 0;

    private ConnectHandshake() {}

    public static final class LocalIdentity {
        public final String nodeId;
        public final String nodeName;
        public final long androidId;
        public final String networkId;
        public final String packageName;
        public final boolean migrating;
        public final String migratingFromNodeId;

        public LocalIdentity(String nodeId, String nodeName, long androidId, String networkId,
                             String packageName, boolean migrating, String migratingFromNodeId) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.androidId = androidId;
            this.networkId = networkId;
            this.packageName = packageName;
            this.migrating = migrating;
            this.migratingFromNodeId = migratingFromNodeId;
        }
    }

    public static Connect perform(WearableConnection connection, LocalIdentity local)
            throws IOException {
        connection.writeMessage(new RootMessage.Builder().connect(build(local)).build());

        return readAndValidatePeerConnect(connection, local);
    }

    public static Connect perform(
            WearableConnection connection,
            String localNodeId, String localNodeName,
            long androidId, boolean isMigrating,
            String migratingFrom
    ) throws IOException {
        return perform(connection, new LocalIdentity(localNodeId, localNodeName, androidId,
                null, null, isMigrating, migratingFrom));
    }

    public static Connect perform(
            WearableConnection connection,
            String localNodeId, String localNodeName,
            long androidId
    ) throws IOException {
        return perform(connection, localNodeId, localNodeName,
                androidId, false, null);
    }

    public static Connect build(LocalIdentity local) {
        Connect.Builder cb = new Connect.Builder()
                .id(local.nodeId)
                .name(local.nodeName != null ? local.nodeName : local.nodeId)
                .peerAndroidId(local.androidId)
                .unknown4(3)
                .peerVersion(PEER_VERSION)
                .peerMinimumVersion(PEER_MIN_VERSION)
                .androidSdkVersion(Build.VERSION.SDK_INT);

        if (!TextUtils.isEmpty(local.networkId)) {
            cb.networkId(local.networkId);
        }

        if (!TextUtils.isEmpty(local.packageName)) {
            cb.packageName(local.packageName);
        }

        if (local.migrating) {
            cb.migrating(true);
            if (!TextUtils.isEmpty(local.migratingFromNodeId)) {
                cb.migratingFromNodeId(local.migratingFromNodeId);
            }
        }

        return cb.build();
    }

    private static Connect readAndValidatePeerConnect(
            WearableConnection connection, LocalIdentity local
    ) throws IOException {
        RootMessage incoming = connection.readMessage();
        Log.d(TAG, "readAndValidatePeerConnect: received=" + incoming);
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
        checkMigrationParity(peer, local.migrating);
        checkNetworkId(peer, local);
        checkMigrationSource(peer, local);

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

    private static void checkNetworkId(Connect peer, LocalIdentity local) throws IOException {
        if (local.migrating) return;
        String peerNetworkId = peer.networkId;
        if (TextUtils.isEmpty(peerNetworkId) || TextUtils.isEmpty(local.networkId)) return;
        if (!local.networkId.equals(peerNetworkId)) {
            throw new IOException("networkId mismatch - expected " + local.networkId
                    + " but peer advertised " + peerNetworkId);
        }
    }

    private static void checkMigrationSource(Connect peer, LocalIdentity local) throws IOException {
        if (!local.migrating) return;
        if (!TextUtils.isEmpty(local.migratingFromNodeId)) return;
        if (TextUtils.isEmpty(peer.migratingFromNodeId)) {
            throw new IOException("Peer is migrating but Connect is missing migratingFromNodeId");
        }
    }
}
