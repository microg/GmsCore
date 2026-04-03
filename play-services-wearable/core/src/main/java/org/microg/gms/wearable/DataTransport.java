package org.microg.gms.wearable;

import android.util.Log;

import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SyncStart;
import org.microg.gms.wearable.proto.SyncTableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataTransport {
    private static final String TAG = "WearDataTransport";

    public final String localNodeId;
    public final String peerNodeId;

    private final WearableImpl wearable;
    private final Object lock = new Object();

    private WearableWriter writer;

    private Map<String, Long> peerSeqIds = new HashMap<>();

    private Thread syncThread;

    private final AtomicBoolean initialSyncFinished = new AtomicBoolean(false);

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
        synchronized (lock) {
            this.writer = writer;
            this.initialSyncFinished.set(false);
        }
        sendSyncStart();
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

        Map<String, Long> seqIds = wearable.getNodeDatabase().getAllCurrentSeqIds();

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
        Log.d(TAG, "runSync start: peer=" + peerNodeId);
        try {
            Map<String, Long> localSeqIds = wearable.getNodeDatabase().getAllCurrentSeqIds();
            for (Map.Entry<String, Long> local : localSeqIds.entrySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "runSync interrupted for peer=" + peerNodeId);
                    return;
                }

                String src = local.getKey();
                long peer = remotePeerSeqIds.containsKey(src) ? remotePeerSeqIds.get(src) : -1l;

                wearable.syncToPeer(peerNodeId, src, peer);
            }
        } catch (Exception e) {
            Log.w(TAG, "runSync exception for peer=" + peerNodeId, e);
        } finally {
            initialSyncFinished.set(true);
            Log.d(TAG, "runSync done: peer=" + peerNodeId);
        }
    }

    public boolean sendDataItem(DataItemRecord record) {
        WearableWriter currWriter;
        synchronized (lock) {
            currWriter = this.writer;
        }
        if (currWriter == null) {
            Log.w(TAG, "sendDataItem: no writer for peer=" + peerNodeId);
            return false;
        }

        currWriter.enqueue(
                new RootMessage.Builder()
                        .setDataItem(record.toSetDataItem())
                        .build()
        );

        long seqId = isV1Peer ? record.v1SeqId : record.seqId;

        String src = (record.source != null) ? record.source : localNodeId;

        updateSeqIdForSource(src, seqId);

        Log.d(TAG, "sendDataItem enqueued: peer=" + peerNodeId + " src=" + src + " seqId" + seqId);
        return true;
    }

    public boolean isInitialSyncFinished() {
        return initialSyncFinished.get();
    }

    public boolean isV1Peer() {
        return isV1Peer;
    }

    @Override
    public String toString() {
        return "DataTransport{local="+ localNodeId + ", peer=" + peerNodeId
                + ", isV1=" + isV1Peer + ", syncDone=" + initialSyncFinished.get() + "}";
    }
}
