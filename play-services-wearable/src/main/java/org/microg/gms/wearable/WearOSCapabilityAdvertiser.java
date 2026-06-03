package org.microg.gms.wearable;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Advertises microG's WearOS capabilities to connected watches.
 *
 * SOLVES:
 * - C-04: No retry on publish timeout -> exponential backoff retry
 */
public class WearOSCapabilityAdvertiser {

    private static final String TAG = "WearOSCapAd";
    private static final int CAPABILITIES_VERSION = 1;
    private static final int MAX_PUBLISH_RETRIES = 3;
    private static final long PUBLISH_RETRY_BASE_MS = 2_000;

    private final Context context;
    private final ScheduledExecutorService executor;
    private GoogleApiClient googleApiClient;

    private boolean notificationsEnabled = true;
    private boolean mediaEnabled = true;
    private boolean cloudSyncEnabled = true;
    private volatile boolean isAdvertising = false;

    public WearOSCapabilityAdvertiser(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WearOS-CapAd");
            t.setDaemon(true);
            return t;
        });
    }

    public void startAdvertising(GoogleApiClient apiClient) {
        if (isAdvertising) return;
        this.googleApiClient = apiClient;
        executor.execute(() -> publishDataItem(0));
        isAdvertising = true;
    }

    public void stopAdvertising() { isAdvertising = false; }

    public void setNotificationCapability(boolean enabled) {
        this.notificationsEnabled = enabled;
        if (isAdvertising) executor.execute(() -> publishDataItem(0));
    }

    public void setMediaCapability(boolean enabled) {
        this.mediaEnabled = enabled;
        if (isAdvertising) executor.execute(() -> publishDataItem(0));
    }

    public void setCloudSyncCapability(boolean enabled) {
        this.cloudSyncEnabled = enabled;
        if (isAdvertising) executor.execute(() -> publishDataItem(0));
    }

    public void refresh() {
        if (isAdvertising) executor.execute(() -> publishDataItem(0));
    }

    // C-04 FIX: Retry with exponential backoff on failure
    private void publishDataItem(int attempt) {
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            Log.w(TAG, "Not connected, deferring publish");
            return;
        }
        try {
            PutDataMapRequest req = PutDataMapRequest.create(
                    WearableDataPaths.CAPABILITIES);
            DataMap map = req.getDataMap();
            map.putInt("version", CAPABILITIES_VERSION);
            map.putBoolean("notifications_enabled", notificationsEnabled);
            map.putBoolean("media_enabled", mediaEnabled);
            map.putBoolean("cloud_sync_enabled", cloudSyncEnabled);
            map.putString("phone_model", Build.MODEL);
            map.putInt("android_version", Build.VERSION.SDK_INT);
            map.putString("microg_version", getMicroGVersion());
            map.putLong("timestamp", System.currentTimeMillis());

            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(
                    googleApiClient, req.asPutDataRequest())
                    .await(10, TimeUnit.SECONDS);

            if (result == null || !result.getStatus().isSuccess()) {
                if (attempt < MAX_PUBLISH_RETRIES) {
                    long delay = PUBLISH_RETRY_BASE_MS * (long) Math.pow(2, attempt);
                    Log.w(TAG, "Publish failed, retry " + (attempt + 1) + " in " + delay + "ms");
                    executor.schedule(() -> publishDataItem(attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    Log.e(TAG, "Publish failed after " + MAX_PUBLISH_RETRIES + " retries");
                }
            } else {
                Log.d(TAG, "Capabilities published: notif=" + notificationsEnabled + " media=" + mediaEnabled);
            }
        } catch (Exception e) {
            Log.e(TAG, "Publish exception", e);
            if (attempt < MAX_PUBLISH_RETRIES) {
                long delay = PUBLISH_RETRY_BASE_MS * (long) Math.pow(2, attempt);
                executor.schedule(() -> publishDataItem(attempt + 1), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    public static CapabilityData queryCapabilities(GoogleApiClient apiClient) {
        try {
            Uri uri = new Uri.Builder().scheme("wear")
                    .path(WearableDataPaths.CAPABILITIES).build();
            DataApi.DataItemBuffer buffer = Wearable.DataApi
                    .getDataItems(apiClient, uri).await(10, TimeUnit.SECONDS);
            if (buffer == null || buffer.getCount() == 0) {
                if (buffer != null) buffer.release();
                return null;
            }
            DataItem item = buffer.get(0);
            DataMap map = DataMapItem.fromDataItem(item).getDataMap();
            buffer.release();
            CapabilityData data = new CapabilityData();
            data.version = map.getInt("version", 0);
            data.notificationsEnabled = map.getBoolean("notifications_enabled", false);
            data.mediaEnabled = map.getBoolean("media_enabled", false);
            data.cloudSyncEnabled = map.getBoolean("cloud_sync_enabled", false);
            data.phoneModel = map.getString("phone_model", "Unknown");
            data.timestamp = map.getLong("timestamp", 0);
            return data;
        } catch (Exception e) { Log.e(TAG, "Query failed", e); return null; }
    }

    public static class CapabilityData {
        public int version;
        public boolean notificationsEnabled;
        public boolean mediaEnabled;
        public boolean cloudSyncEnabled;
        public String phoneModel;
        public long timestamp;
        @Override
        public String toString() {
            return "CapData{v=" + version + ", notif=" + notificationsEnabled
                    + ", media=" + mediaEnabled + ", cloud=" + cloudSyncEnabled + "}";
        }
    }

    private String getMicroGVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) { return "unknown"; }
    }
}
