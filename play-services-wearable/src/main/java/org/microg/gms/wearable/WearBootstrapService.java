package org.microg.gms.wearable;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Watch-side bootstrap service.
 * Discovers phone capabilities and starts feature services accordingly.
 */
public class WearBootstrapService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WearBootstrap";

    private GoogleApiClient googleApiClient;
    private WearOSCapabilityAdvertiser.CapabilityData currentCaps;
    private boolean featuresStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopFeatureServices();
        if (googleApiClient != null) googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected, discovering capabilities...");
        new Thread(this::discoverCapabilities, "CapDiscover").start();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "Suspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection failed: " + result);
    }

    private void discoverCapabilities() {
        try {
            WearOSCapabilityAdvertiser.CapabilityData caps =
                    WearOSCapabilityAdvertiser.queryCapabilities(googleApiClient);
            if (caps == null) {
                Thread.sleep(5000);
                caps = WearOSCapabilityAdvertiser.queryCapabilities(googleApiClient);
            }
            if (caps != null) {
                Log.i(TAG, "Capabilities: " + caps);
                currentCaps = caps;
                updateFeatureServices(caps);
            } else {
                Log.w(TAG, "No capabilities found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Discovery failed", e);
        }
    }

    private void updateFeatureServices(WearOSCapabilityAdvertiser.CapabilityData caps) {
        if (caps.mediaEnabled && !featuresStarted) startFeatureServices();
        else if (!caps.mediaEnabled && featuresStarted) stopFeatureServices();
    }

    private void startFeatureServices() {
        if (featuresStarted) return;
        try {
            startService(new Intent(this, WearMediaControlService.class));
            featuresStarted = true;
            Log.i(TAG, "Feature services started");
        } catch (Exception e) { Log.e(TAG, "Failed to start features", e); }
    }

    private void stopFeatureServices() {
        if (!featuresStarted) return;
        try {
            stopService(new Intent(this, WearMediaControlService.class));
            featuresStarted = false;
        } catch (Exception e) { Log.e(TAG, "Failed to stop features", e); }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (dataEvents == null) return;
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED
                    && WearableDataPaths.CAPABILITIES.equals(
                            event.getDataItem().getUri().getPath())) {
                DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                WearOSCapabilityAdvertiser.CapabilityData caps
                        = new WearOSCapabilityAdvertiser.CapabilityData();
                caps.version = map.getInt("version", 0);
                caps.notificationsEnabled = map.getBoolean("notifications_enabled", false);
                caps.mediaEnabled = map.getBoolean("media_enabled", false);
                caps.cloudSyncEnabled = map.getBoolean("cloud_sync_enabled", false);
                caps.phoneModel = map.getString("phone_model", "Unknown");
                caps.timestamp = map.getLong("timestamp", 0);
                updateFeatureServices(caps);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (event == null) return;
        if (WearableDataPaths.HEARTBEAT.equals(event.getPath())) {
            Wearable.MessageApi.sendMessage(googleApiClient,
                    event.getSourceNodeId(),
                    WearableDataPaths.HEARTBEAT_ACK, new byte[0]);
        }
    }
}
