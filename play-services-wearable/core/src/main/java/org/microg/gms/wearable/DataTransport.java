package org.microg.gms.wearable;

import android.util.Log;

import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.channel.ChannelManager;
import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SyncStart;
import org.microg.gms.wearable.proto.SyncTableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataTransport {
    private static final String TAG = "WearDataTransport";

    public enum DataSyncMode {
        SYNC_ENABLED,
        SYNC_DISABLED_UNTIL_STARTED,
        SYNC_DISABLED_UNTIL_MESSAGE_RECEIVED
    }

    public final String localNodeId;
    public final String peerNodeId;

    private final WearableImpl wearable;
    private final Object lock = new Object();

    private WearableWriter writer;

    private Map<String, Long> peerSeqIds = new HashMap<>();

    private Thread syncThread;
    private final AtomicBoolean initialSyncFinished = new AtomicBoolean(false);

    private volatile SyncStart pendingSyncStart = null;
    private volatile DataSyncMode dataSyncMode = DataSyncMode.SYNC_ENABLED;
    private volatile boolean isV1Peer = false;

    public DataTransport(String localNodeId, String peerNodeId, WearableImpl wearable) {
        this.wearable = wearable;
        this.localNodeId = localNodeId;
        this.peerNodeId = peerNodeId;
    }

    public long getSeqIdForSource(String sourceNodeId) {
        synchronized (lock) {
            Long l = peerSeqIds.get(sourceNodeId);
            return l != null ? l : -1L;
        }
    }

    public long updateSeqIdForSource(String sourceNodeId, long seqId) {
        synchronized (lock) {
            Long current = peerSeqIds.get(sourceNodeId);
            if (current == null || seqId > current) {
                peerSeqIds.put(sourceNodeId, seqId);
                return seqId;
            }
            return current;
        }
    }

    public void onConnected(WearableWriter writer) {
        onConnected(writer, DataSyncMode.SYNC_ENABLED);
    }

    public void onConnected(WearableWriter writer, DataSyncMode mode) {
        DataSyncMode effective = (mode != null) ? mode : DataSyncMode.SYNC_ENABLED;
        synchronized (lock) {
            this.writer = writer;
            this.dataSyncMode = effective;
            this.initialSyncFinished.set(false);
        }
        
        if (effective == DataSyncMode.SYNC_ENABLED)
            sendSyncStart();
        else
            Log.d(TAG, "onConnected: SyncStart withheld, mode=" + effective
                    + " peer=" + peerNodeId);
    }

    public void onDisconnect() {
        WearableWriter prevWriter;
        Thread prev;
        synchronized (lock) {
            prev = this.syncThread;
            prevWriter = this.writer;
            this.syncThread = null;
            this.writer = null;
            this.initialSyncFinished.set(false);
            this.peerSeqIds = new HashMap<>();
            this.pendingSyncStart = null;
            this.dataSyncMode = DataSyncMode.SYNC_ENABLED;
        }
        if (prevWriter != null) prevWriter.close();
        if (prev != null) prev.interrupt();
        Log.d(TAG, "onDisconnect: node=" + localNodeId + " peer=" + peerNodeId);
    }

    public void sendSyncStart() {
        WearableWriter currentWriter;
        synchronized (lock){
            currentWriter = this.writer;
        }

        if (currentWriter == null) {
            Log.w(TAG, "sendSyncStart: no writer for peer=" + peerNodeId);
            return;
        }

        Map<String, Long> allSeqIds = wearable.getNodeDatabase().getAllCurrentSeqIds();
        Set<String> activePeers = wearable.getConnectedPeerNodeIds();
        Map<String, Long> seqIds = new HashMap<>();

        for (Map.Entry<String, Long> e : allSeqIds.entrySet()) {
            String src = e.getKey();
            if (src != null && (src.equals(localNodeId) || activePeers.contains(src)))
                seqIds.put(src, e.getValue());
        }

        List<SyncTableEntry> table = new ArrayList<>(seqIds.size());
        for (Map.Entry<String, Long> e : seqIds.entrySet()) {
            table.add(
                    new SyncTableEntry.Builder()
                            .key(e.getKey())
                            .value(e.getValue())
                            .build()
            );
        }

        Long receivedSeqId = seqIds.containsKey(peerNodeId) ? seqIds.get(peerNodeId) : -1L;

        SyncStart syncStart = new SyncStart.Builder()
                .receivedSeqId(receivedSeqId)
                .version(2)
                .syncTable(table)
                .build();

        currentWriter.enqueue(new RootMessage.Builder().syncStart(syncStart).build());

        Log.d(TAG, "sendSyncStart enqueued: node=" + localNodeId + " peer=" + peerNodeId
                + " receivedSeqId=" + receivedSeqId + " entries=" + table.size());
    }

    public void respondToSyncStart(SyncStart syncStart) {
        synchronized (lock) {
            if (dataSyncMode == DataSyncMode.SYNC_DISABLED_UNTIL_STARTED) {
                Log.d(TAG, "respondToSyncStart: parked (sync disabled) for " + peerNodeId);
                pendingSyncStart = syncStart;
                return;
            }

            if (dataSyncMode == DataSyncMode.SYNC_DISABLED_UNTIL_MESSAGE_RECEIVED) {
                Log.d(TAG, "respondToSyncStart: enabling sync because SyncStart arrived for " + peerNodeId);
                pendingSyncStart = syncStart;
                onSyncEnabledLocked();
                return;
            }

            doRespondToSyncStart(syncStart);
        }
    }

    public void onSyncEnabled() {
        synchronized (lock) {
            onSyncEnabledLocked();
        }
    }

    private void onSyncEnabledLocked()
    {
        if (dataSyncMode == DataSyncMode.SYNC_ENABLED || writer == null)
            return;

        dataSyncMode = DataSyncMode.SYNC_ENABLED;
        SyncStart pending = pendingSyncStart;
        pendingSyncStart = null;
        sendSyncStart();

        if (pending != null)
            doRespondToSyncStart(pending);
    }

    private void doRespondToSyncStart(SyncStart syncStart) {
        if (syncStart == null) return;

        boolean v1 = (syncStart.version == null || syncStart.version < 2);
        this.isV1Peer = v1;

        Map<String, Long> remote = new HashMap<>();
        if (syncStart.syncTable != null) {
            for (SyncTableEntry e : syncStart.syncTable) {
                if (e.key != null && e.value != null) {
                    remote.put(e.key, e.value);
                }
            }
        }

        Log.d(TAG, "respondToSyncStart: node=" + localNodeId + " peer=" + peerNodeId
                    + " version=" + syncStart.version + " isV1=" + v1 + " remoteEntries=" + remote.size());

        WearableWriter currentWriter;
        synchronized (lock) {
            this.peerSeqIds = remote;
            currentWriter = this.writer;
        }

        if (currentWriter == null) {
            Log.w(TAG, "respondToSyncStart: no writer for perr=" + peerNodeId);
            return;
        }

        startSyncThread(new HashMap<>(remote));
    }

    private void startSyncThread(Map<String, Long> remotePeers) {
        String name = "SyncStart" + (isV1Peer ? "V1" : "V2") + "-" + peerNodeId;

        Thread newThread = new Thread(() -> runSync(remotePeers), name);

        Thread prev;
        synchronized (lock) {
            prev = this.syncThread;
            this.syncThread = newThread;
        }
        if (prev != null) prev.interrupt();
        newThread.start();
        Log.d(TAG, "Sync thread started: " + name);
    }

    private void runSync(Map<String, Long> remotePeerSeqIds) {
        Set<String> activePeers = wearable.getConnectedPeerNodeIds();
        Log.d(TAG, "runSync start: peer=" + peerNodeId
                + " activePeers=" + activePeers.size());
        try {
            Map<String, Long> localSeqIds = wearable.getNodeDatabase().getAllCurrentSeqIds();
            int synced = 0;
            int skipped = 0;
            int ghostsExcluded = 0;
            int echoSkipped = 0;
            for (Map.Entry<String, Long> local : localSeqIds.entrySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "runSync interrupted for peer=" + peerNodeId);
                    return;
                }

                String src = local.getKey();

                if (src == null || (!src.equals(localNodeId) && !activePeers.contains(src))) {
                    ghostsExcluded++;
                    continue;
                }

                if (src.equals(peerNodeId)){
                    echoSkipped++;
                    continue;
                }

                long peer = remotePeerSeqIds.containsKey(src) ? remotePeerSeqIds.get(src) : -1;
                long localMax = local.getValue() != null ? local.getValue() : -1;

                if (localMax <= peer) {
                    skipped++;
                    continue;
                }

                if (hasActiveChannelForPeer()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                wearable.syncToPeer(peerNodeId, src, peer);
                synced++;
            }
            Log.d(TAG, "runSync: peer=" + peerNodeId
                    + "synced=" + synced + " upToDate=" + skipped
                    + " ghostsExcluded=" + ghostsExcluded
                    + " selfEchoSkipped=" + echoSkipped);
        } catch (Exception e) {
            Log.w(TAG, "runSync exception for peer=" + peerNodeId, e);
        } finally {
            initialSyncFinished.set(true);
            Log.d(TAG, "runSync done: peer=" + peerNodeId);
        }
    }

    private boolean hasActiveChannelForPeer() {
        ChannelManager cm = wearable.getChannelManager();
        if (cm == null)
            return false;
        return cm.hasActiveChannelForNode(peerNodeId);
    }

    public boolean sendDataItem(DataItemRecord record) {
        long seqId = isV1Peer ? record.v1SeqId : record.seqId;
        String src = (record.source != null) ? record.source : localNodeId;
        synchronized (lock) {
            WearableWriter currWriter = this.writer;

            if (currWriter == null) {
                Log.w(TAG, "sendDataItem: no writer for peer=" + peerNodeId);
                return false;
            }

            boolean enqueued = currWriter.enqueue(
                    new RootMessage.Builder()
                            .setDataItem(record.toSetDataItem())
                            .build()
            );

            if (!enqueued) {
                Log.w(TAG, "sendDataItem: writer closed for peer=" + peerNodeId
                        + " src=" + src + " seqId=" + seqId
                        + " — message dropped, NOT advancing watermark");
                return false;
            }

            updateSeqIdForSource(src, seqId);
        }
        Log.d(TAG, "sendDataItem enqueued: peer=" + peerNodeId + " src=" + src + " seqId" + seqId);
        return true;
    }

    public boolean isInitialSyncFinished() {
        return initialSyncFinished.get();
    }

    public boolean isV1Peer() {
        return isV1Peer;
    }

    public void onDataItemSyncEnabled() {
        onSyncEnabled();
    }

    public void onPeerChannelClosed() {}

    @Override
    public String toString() {
        return "DataTransport{local="+ localNodeId + ", peer=" + peerNodeId
                + ", isV1=" + isV1Peer + ", syncDone=" + initialSyncFinished.get() + "}";
    }
}
