package org.microg.gms.wearable;

import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.wearable.Asset;

import org.microg.gms.wearable.channel.ChannelManager;
import org.microg.gms.wearable.proto.FetchAsset;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AssetFetcher {
    private static final String TAG = "GmsWearAssetFetch";

    private final NodeDatabaseHelper nodeDatabase;
    private final Handler networkHandler;

    private final Set<String> fetchingAssets = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final Map<String, AssetFetchAttempt> failedAssets = new ConcurrentHashMap<>();

    private static final int ASSET_BATCH_SIZE = 10;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_COOLDOWN_MS = 5000; // 5 seconds before retry
    private static final long FAILED_ASSET_EXPIRY_MS = 300000; // 5 minutes

    public AssetFetcher(NodeDatabaseHelper nodeDatabase, Handler networkHandler) {
        this.nodeDatabase = nodeDatabase;
        this.networkHandler = networkHandler;
    }

    public void fetchMissingAssets(String nodeId, WearableConnection connection,
                                   Map<String, WearableConnection> activeConnections,
                                   ChannelManager channelManager) {
        if (connection == null) {
            Log.d(TAG, "Connection no longer active for node: " + nodeId);
            return;
        }

        cleanupExpiredFailures();

        Cursor cursor = nodeDatabase.listMissingAssets();
        if (cursor == null) {
            return;
        }

        try {
            int fetchCount = 0;
            int skippedCount = 0;
            int alreadyFetchingCount = 0;

            while (cursor.moveToNext()) {
                if (!activeConnections.containsKey(nodeId)) {
                    Log.d(TAG, "Connection closed during asset fetch, stopping (fetched="
                            + fetchCount + ", skipped=" + skippedCount + ")");
                    break;
                }

                String assetDigest = cursor.getString(13);
                String assetName = cursor.getString(12);
                String packageName = cursor.getString(1);
                String signatureDigest = cursor.getString(2);

                if (fetchingAssets.contains(assetDigest)) {
                    alreadyFetchingCount++;
                    continue;
                }

                AssetFetchAttempt attempt = failedAssets.get(assetDigest);
                if (attempt != null) {
                    if (attempt.retryCount >= MAX_RETRY_COUNT) {
                        skippedCount++;
                        continue;
                    }

                    long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
                    if (timeSinceLastAttempt < RETRY_COOLDOWN_MS) {
                        skippedCount++;
                        continue;
                    }
                }

                try {
                    fetchingAssets.add(assetDigest);

                    connection.writeMessage(new RootMessage.Builder()
                            .fetchAsset(new FetchAsset.Builder()
                                    .assetName(assetName)
                                    .packageName(packageName)
                                    .signatureDigest(signatureDigest)
                                    .build())
                            .build());

                    fetchCount++;

                    failedAssets.remove(assetDigest);

                    if (fetchCount % ASSET_BATCH_SIZE == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Asset fetch interrupted");
                            break;
                        }
                    }

                } catch (IOException e) {
                    Log.w(TAG, "Error fetching asset " + assetDigest +
                            " (fetched " + fetchCount + " so far): " + e.getMessage());

                    recordFailure(assetDigest);

                    fetchingAssets.remove(assetDigest);

                    if (isConnectionError(e)) {
                        break;
                    }
                }
            }

            if (fetchCount > 0 || skippedCount > 0 || alreadyFetchingCount > 0) {
                Log.d(TAG, "Asset fetch summary: fetched=" + fetchCount +
                        ", skipped=" + skippedCount +
                        ", alreadyFetching=" + alreadyFetchingCount);
            }

            if (fetchCount > 100 && channelManager != null) {
                Log.d(TAG, "Large asset batch (" + fetchCount + "), applying cooldown");
                channelManager.setOperationCooldown(1000);
            }

        } finally {
            cursor.close();
        }
    }

    public void fetchMissingAssetsForRecord(WearableConnection connection,
                                            DataItemRecord record,
                                            List<Asset> missingAssets) {
        int successCount = 0;
        int skipCount = 0;

        for (Asset asset : missingAssets) {
            String digest = asset.getDigest();

            if (fetchingAssets.contains(digest)) {
                skipCount++;
                continue;
            }

            AssetFetchAttempt attempt = failedAssets.get(digest);
            if (attempt != null) {
                if (attempt.retryCount >= MAX_RETRY_COUNT) {
                    Log.d(TAG, "Asset " + digest + " failed too many times, skipping");
                    skipCount++;
                    continue;
                }

                long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
                if (timeSinceLastAttempt < RETRY_COOLDOWN_MS) {
                    skipCount++;
                    continue;
                }
            }

            try {
                Log.d(TAG, "Fetching missing asset for record: " + digest);

                fetchingAssets.add(digest);

                FetchAsset fetchAsset = new FetchAsset.Builder()
                        .assetName(digest)
                        .packageName(record.packageName)
                        .signatureDigest(record.signatureDigest)
                        .permission(false)
                        .build();

                connection.writeMessage(new RootMessage.Builder()
                        .fetchAsset(fetchAsset)
                        .build());

                successCount++;

                failedAssets.remove(digest);

            } catch (IOException e) {
                Log.w(TAG, "Error fetching asset " + digest + " for record", e);

                recordFailure(digest);
                fetchingAssets.remove(digest);
            }
        }

        if (successCount > 0 || skipCount > 0) {
            Log.d(TAG, "Record asset fetch: success=" + successCount + ", skipped=" + skipCount);
        }
    }

    public void onAssetReceived(String digest) {
        fetchingAssets.remove(digest);
        failedAssets.remove(digest);
        Log.v(TAG, "Asset received and tracked: " + digest);
    }

    public void onAssetFetchFailed(String digest) {
        fetchingAssets.remove(digest);
        recordFailure(digest);
        Log.d(TAG, "Asset fetch failed: " + digest);
    }

    private void recordFailure(String digest) {
        AssetFetchAttempt attempt = failedAssets.get(digest);
        if (attempt == null) {
            attempt = new AssetFetchAttempt(digest);
            failedAssets.put(digest, attempt);
        }
        attempt.recordFailure();
    }

    private boolean isConnectionError(IOException e) {
        String message = e.getMessage();
        if (message == null) return false;

        return message.contains("Connection") ||
                message.contains("Broken pipe") ||
                message.contains("Socket closed") ||
                message.contains("Connection reset");
    }

    private void cleanupExpiredFailures() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, AssetFetchAttempt> entry : failedAssets.entrySet()) {
            if (now - entry.getValue().firstAttemptTime > FAILED_ASSET_EXPIRY_MS) {
                toRemove.add(entry.getKey());
            }
        }

        for (String digest : toRemove) {
            failedAssets.remove(digest);
        }

        if (!toRemove.isEmpty()) {
            Log.d(TAG, "Cleaned up " + toRemove.size() + " expired failed asset records");
        }
    }

    public AssetFetchStats getStats() {
        int failedCount = 0;
        int retryingCount = 0;

        for (AssetFetchAttempt attempt : failedAssets.values()) {
            if (attempt.retryCount >= MAX_RETRY_COUNT) {
                failedCount++;
            } else {
                retryingCount++;
            }
        }

        return new AssetFetchStats(
                fetchingAssets.size(),
                retryingCount,
                failedCount
        );
    }

    public void resetTracking() {
        fetchingAssets.clear();
        failedAssets.clear();
        Log.d(TAG, "Asset fetch tracking reset");
    }

    private static class AssetFetchAttempt {
        final String digest;
        final long firstAttemptTime;
        long lastAttemptTime;
        int retryCount;

        AssetFetchAttempt(String digest) {
            this.digest = digest;
            this.firstAttemptTime = System.currentTimeMillis();
            this.lastAttemptTime = firstAttemptTime;
            this.retryCount = 0;
        }

        void recordFailure() {
            this.lastAttemptTime = System.currentTimeMillis();
            this.retryCount++;
        }
    }

    public static class AssetFetchStats {
        public final int currentlyFetching;
        public final int retrying;
        public final int failed;

        AssetFetchStats(int currentlyFetching, int retrying, int failed) {
            this.currentlyFetching = currentlyFetching;
            this.retrying = retrying;
            this.failed = failed;
        }

        @Override
        public String toString() {
            return "AssetFetchStats" +
                    "{fetching="+currentlyFetching+", " +
                    "retrying="+retrying+", " +
                    "failed="+failed+"}";
        }
    }
}