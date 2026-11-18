package org.microg.gms.wearable;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.wearable.Wearable;

import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.AppKeys;
import org.microg.gms.wearable.proto.FetchAsset;
import org.microg.gms.wearable.proto.FilePiece;
import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SetAsset;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.ByteString;

public class AssetManager {
    private static final String TAG = "WearAssetManager";

    private static final int CHUNK_SIZE = 12215;

    private final WearableImpl wearable;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread t = new Thread(r, TAG);
                t.setDaemon(true);
                return t;
            }
    );

    private final Map<String, FetchEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, WearableWriter> writers = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private static final long SEND_DEDUPE_MS = 60_000;
    private final ConcurrentMap<String, Long> sends = new ConcurrentHashMap<>();

    public AssetManager(WearableImpl wearable) {
        this.wearable = wearable;
    }

    public void addWriter(String nodeId, WearableWriter writer) {
        writers.put(nodeId, writer);

        if (entries.isEmpty()) return;

        Log.d(TAG, "addWriter: replaying " + entries.size() + " to " + nodeId);

        for (FetchEntry e : entries.values()) {
            writer.enqueue(buildFetchMessage(e));
        }
    }

    public void removeWriter(String nodeId) {
        writers.remove(nodeId);
        Log.d(TAG, "removeWriter: " + nodeId);
    }

    public void onAssetMissing(String digest, String packageName, String signature) {
		FetchEntry existing = entries.get(digest);
		if (existing != null && !existing.permission) {
			Log.d(TAG, "onAssetMissing: full fetch already pending for " + digest);
			return;
		}
        enqueueFetch(digest, packageName, signature, false);
    }

    public void onAssetPermissionMissing(String digest, String packageName, String signature) {
        Log.d(TAG, "onAssetPermissionMissing: " + digest);
        FetchEntry existing = entries.get(digest);
        if (existing != null && !existing.permission) {
            Log.d(TAG, "onAssetPermissionMissing: full fetch already pending for " + digest);
            return;
        }
        enqueueFetch(digest, packageName, signature, true);
    }

    private void enqueueFetch(String digest, String packageName,
                              String signature, boolean permission) {
        FetchEntry fetchEntry = new FetchEntry(digest, packageName, signature, permission);
        entries.put(digest, fetchEntry);

        RootMessage msg = buildFetchMessage(fetchEntry);
        for (WearableWriter writer: writers.values()) {
            writer.enqueue(msg);
        }
    }

    private static RootMessage buildFetchMessage(FetchEntry entry) {
        return new RootMessage.Builder()
                .fetchAsset(
                        new FetchAsset.Builder()
                                .assetName(entry.digest)
                                .packageName(entry.packageName)
                                .signatureDigest(entry.signatureDigest)
                                .permission(entry.permission)
                                .build()
                ).build();
    }

    public void onAckAsset(String digest) {
        if (digest == null) return;
        Log.d(TAG, "onAckAsset: " + digest);
        entries.remove(digest);
        checkCompletion();
    }

    private boolean beginSend(String peer, String digest) {
        String key = peer + "|" + digest;
        long now = SystemClock.elapsedRealtime();
        Long prev = sends.putIfAbsent(key, now);
        if (prev == null) return true;
        if (now - prev < SEND_DEDUPE_MS) return false;   // already sending (Google ignores it too)
        return sends.replace(key, prev, now);            // stale entry, e.g. peer never acked
    }

    public void onAckReceived(String peerNodeId, String digest) {
        if (peerNodeId != null && digest != null) sends.remove(peerNodeId + "|" + digest);
    }

    public void onPeerGone(String peerNodeId) {
        String prefix = peerNodeId + "|";
        for (String k : sends.keySet()) if (k.startsWith(prefix)) sends.remove(k);
    }

    public void handleFetchAsset(final WearableConnection connection, final String peerNodeId,
                                 final FetchAsset f) {
        if (f == null || f.assetName == null || peerNodeId == null) return;
        final String digest = f.assetName;
        final String pkg = f.packageName != null ? f.packageName : "";
        final String sig = f.signatureDigest != null ? f.signatureDigest : "";
        final boolean permissionOnly = Boolean.TRUE.equals(f.permission);

        if (!beginSend(peerNodeId, digest)) {
            Log.d(TAG, "handleFetchAsset: already sending " + digest + " to " + peerNodeId);
            return;
        }

        ioExecutor.execute(() -> {
            boolean ok = false;
            try {
                if (!wearable.getNodeDatabase().isAssetAllowedFor(digest, pkg, sig)) {
                    Log.w(TAG, "handleFetchAsset: " + pkg + " not allowed for " + digest + ", empty reply");
                    writeSetAsset(connection, digest, new AppKeys(Collections.<AppKey>emptyList()), false);
                    ok = true;
                    return;
                }
                AppKeys keys = new AppKeys(Collections.singletonList(new AppKey(pkg, sig)));
                File file = wearable.createAssetFile(digest);
                if (permissionOnly || !file.isFile()) {
                    writeSetAsset(connection, digest, keys, false);
                } else {
                    RootMessage msg = writeSetAsset(connection, digest, keys, true);
                    streamFilePieces(connection, file, WearableConnection.calculateDigest(msg.encode()), digest);
                }
                ok = true;
            } catch (IOException e) {
                Log.w(TAG, "handleFetchAsset: failed for " + digest + ": " + e.getMessage());
            } finally {
                if (!ok) sends.remove(peerNodeId + "|" + digest);   // failed: allow a retry
            }
        });
    }

    private RootMessage writeSetAsset(WearableConnection c, String digest, AppKeys keys, boolean hasAsset)
            throws IOException {
        RootMessage msg = new RootMessage.Builder()
                .setAsset(new SetAsset.Builder().digest(digest).appkeys(keys).build())
                .hasAsset(hasAsset).build();
        c.writeMessage(msg);
        return msg;
    }

    private void sendPermissionConfirmation(WearableConnection connection,
                                            String digest, AppKeys appKeys) {
        try {
            connection.writeMessage(
                    new RootMessage.Builder()
                            .setAsset(
                                    new SetAsset.Builder()
                                            .digest(digest)
                                            .appkeys(appKeys)
                                            .build()
                            )
                            .hasAsset(false)
                            .build()
            );
            Log.d(TAG, "sendPermissionConfirmation: sent for " + digest);
        } catch (IOException e) {
            Log.w(TAG, "sendPermissionConfirmation: failed for " + digest + ": " + e.getMessage());
        }
    }

    private void sendFullAsset(WearableConnection connection, String digest,
                               File assetFile, AppKeys appKeys) {
        try {
            RootMessage msg = new RootMessage.Builder()
                    .setAsset(
                            new SetAsset.Builder()
                                    .digest(digest)
                                    .appkeys(appKeys)
                                    .build()
                    )
                    .hasAsset(true)
                    .build();
            connection.writeMessage(msg);

            String fileName = WearableConnection.calculateDigest(msg.encode());
            streamFilePieces(connection, assetFile, fileName, digest);

            Log.d(TAG, "sendFullAsset: competed for " + digest);
        } catch (IOException e) {
            Log.w(TAG, "sendFullAsset: failed for " + digest + ": " + e.getMessage());
        }
    }

    private void streamFilePieces(WearableConnection connection, File file,
                                  String fileName, String digest) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[CHUNK_SIZE];
            ByteString pending = null;
            int n;

            while ((n = fis.read(buf)) > 0) {
                if (pending != null) {
                    connection.writeMessage(piece(fileName, false, pending, null));
                }
                pending = ByteString.of(buf, 0, n);
            }
            if (pending == null) pending = ByteString.EMPTY;   // empty file: still send a final piece
            connection.writeMessage(piece(fileName, true, pending, digest));
        }
    }

    private static RootMessage piece(String fileName, boolean last, ByteString data, String digest) {
        return new RootMessage.Builder()
                .filePiece(new FilePiece(fileName, last, data, digest))
                .build();
    }

    public void addCompletionListener(Runnable listener) {
        listeners.add(listener);
        checkCompletion();
    }

    private void checkCompletion() {
        if (!entries.isEmpty()) return;

        List<Runnable> toFire = new ArrayList<>(listeners);
        listeners.removeAll(toFire);

        for (Runnable r : toFire) {
            try {
                r.run();
            } catch (Exception e) {
                Log.w(TAG, "checkCompletion: " + e.getMessage());
            }
        }
    }

    public void onAssetReceived(String digest) {
        entries.remove(digest);
        checkCompletion();
    }

    public void onAssetFetchFailed(String digest) {
        entries.remove(digest);
        checkCompletion();
    }

    public int getFetchCount() {
        return entries.size();
    }

    public void reset() {
        entries.clear();
        writers.clear();
        listeners.clear();
    }

    public void shutdown() {
        reset();
        ioExecutor.shutdownNow();
    }

    private AppKeys resolveAppKeys(String digest, String fallbackPackageName, String fallbackSignature) {
        if (fallbackPackageName == null || fallbackPackageName.isEmpty()) {
            return new AppKeys(Collections.emptyList());
        }

        List<AppKey> keys = new ArrayList<>(1);
        keys.add(new AppKey(fallbackPackageName, fallbackSignature != null ? fallbackSignature : ""));
        return new AppKeys(keys);
    }

    private static final class FetchEntry {
        final String digest;
        final String packageName;
        final String signatureDigest;
        final boolean permission;

        FetchEntry(String digest, String packageName, String signatureDigest, boolean permission) {
            this.digest = digest;
            this.packageName = packageName;
            this.signatureDigest = signatureDigest;
            this.permission = permission;
        }
    }
}
