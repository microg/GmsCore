package org.microg.gms.wearable;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top-level orchestrator for all WearOS subsystems.
 *
 * SOLVES:
 * - A-03: Three classes independently calling MessageApi.addListener()
 *   causing race conditions and duplicate processing.
 *
 * DESIGN:
 * - SINGLE MessageApi.addListener() call in this class
 * - Path-prefix-based dispatch to registered MessageHandlers
 * - Lifecycle management for all subsystems in dependency order
 * - Feature toggles at runtime (enable/disable notifications, media)
 *
 * STARTUP ORDER:
 *   1. WearableNodeManager (node discovery + heartbeat)
 *   2. WearOSCapabilityAdvertiser (feature advertisement)
 *   3. MessageRetryQueue (reliable delivery)
 *   4. WearOSNotificationForwarder (if enabled + permission granted)
 *   5. WearOSMediaController (if enabled + permission granted)
 */
public class WearableConnectionManager {

    private static final String TAG = "WearConnMgr";

    private final Context context;

    // Subsystems
    private WearableNodeManager nodeManager;
    private WearOSCapabilityAdvertiser capabilityAdvertiser;
    private MessageRetryQueue retryQueue;
    private WearOSNotificationForwarder notificationForwarder;
    private WearOSMediaController mediaController;

    // Message routing
    private final Map<String, MessageHandler> messageHandlers
            = new ConcurrentHashMap<>();

    // State
    private GoogleApiClient googleApiClient;
    private volatile boolean isStarted = false;
    private boolean notificationsEnabled = true;
    private boolean mediaEnabled = true;

    public WearableConnectionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // --- Lifecycle ---

    public synchronized void onConnected(GoogleApiClient apiClient) {
        if (isStarted) {
            handleReconnection(apiClient);
            return;
        }

        this.googleApiClient = apiClient;

        Log.i(TAG, "Starting subsystems...");

        // 1. Node Manager
        nodeManager = new WearableNodeManager(context);
        nodeManager.addListener(createNodeListener());
        nodeManager.start(apiClient);

        // 2. Global Message Listener (A-03 FIX)
        registerGlobalMessageListener(apiClient);

        // 3. Capability Advertiser
        capabilityAdvertiser = new WearOSCapabilityAdvertiser(context);
        capabilityAdvertiser.setNotificationCapability(notificationsEnabled);
        capabilityAdvertiser.setMediaCapability(mediaEnabled);
        capabilityAdvertiser.startAdvertising(apiClient);

        // 4. Retry Queue
        retryQueue = new MessageRetryQueue();
        retryQueue.setDeadLetterHandler((nodeId, path, data, attempts) ->
                Log.w(TAG, "Dead letter: " + path + " after " + attempts));
        retryQueue.start(apiClient);

        // 5. Register heartbeat handler
        registerMessageHandler(WearableDataPaths.HEARTBEAT,
                this::handleHeartbeatMessage);
        registerMessageHandler(WearableDataPaths.HEARTBEAT_ACK,
                this::handleHeartbeatAck);

        // 6. Notification Forwarder
        if (notificationsEnabled) {
            startNotificationForwarder(apiClient);
        }

        // 7. Media Controller
        if (mediaEnabled) {
            startMediaController(apiClient);
        }

        isStarted = true;
        Log.i(TAG, "All subsystems started");
    }

    public synchronized void onDisconnected() {
        if (!isStarted) return;

        Log.i(TAG, "Stopping subsystems...");

        if (mediaController != null) { mediaController.stop(); mediaController = null; }
        if (notificationForwarder != null) { notificationForwarder.stop(); notificationForwarder = null; }
        if (retryQueue != null) { retryQueue.stop(); retryQueue = null; }
        if (capabilityAdvertiser != null) { capabilityAdvertiser.stopAdvertising(); capabilityAdvertiser = null; }
        if (nodeManager != null) { nodeManager.stop(); nodeManager = null; }

        messageHandlers.clear();
        isStarted = false;
        Log.i(TAG, "All subsystems stopped");
    }

    public synchronized void onConnectionSuspended(int cause) {
        Log.w(TAG, "Connection suspended: " + cause);
        if (nodeManager != null) nodeManager.onConnectionSuspended(cause);
    }

    private void handleReconnection(GoogleApiClient apiClient) {
        this.googleApiClient = apiClient;
        Log.i(TAG, "Reconnecting subsystems...");
        if (nodeManager != null) nodeManager.onConnected(apiClient);
        if (capabilityAdvertiser != null) capabilityAdvertiser.startAdvertising(apiClient);
        if (retryQueue != null) retryQueue.updateApiClient(apiClient);
        if (mediaController != null) mediaController.onWearOSReconnected();
    }

    // --- A-03 FIX: Centralized Message Routing ---

    /**
     * Register a handler for messages matching a path prefix.
     * Only one handler per prefix. Last registration wins.
     */
    public void registerMessageHandler(String pathPrefix, MessageHandler handler) {
        messageHandlers.put(pathPrefix, handler);
        Log.d(TAG, "Handler registered: " + pathPrefix);
    }

    /**
     * SINGLE global message listener for the entire application.
     * All subsystems register handlers instead of adding their own listeners.
     */
    private void registerGlobalMessageListener(GoogleApiClient apiClient) {
        Wearable.MessageApi.addListener(apiClient, event -> {
            if (event == null) return;
            String path = event.getPath();

            for (Map.Entry<String, MessageHandler> entry
                    : messageHandlers.entrySet()) {
                if (path.startsWith(entry.getKey())) {
                    try {
                        entry.getValue().handleMessage(event);
                    } catch (Exception e) {
                        Log.e(TAG, "Handler error for " + path, e);
                    }
                    return; // First match wins
                }
            }

            Log.w(TAG, "No handler for: " + path);
        });
    }

    // --- Feature Toggles ---

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
        if (capabilityAdvertiser != null)
            capabilityAdvertiser.setNotificationCapability(enabled);
        if (isStarted && googleApiClient != null) {
            if (enabled && notificationForwarder == null)
                startNotificationForwarder(googleApiClient);
            else if (!enabled && notificationForwarder != null) {
                notificationForwarder.stop();
                notificationForwarder = null;
            }
        }
    }

    public void setMediaEnabled(boolean enabled) {
        this.mediaEnabled = enabled;
        if (capabilityAdvertiser != null)
            capabilityAdvertiser.setMediaCapability(enabled);
        if (isStarted && googleApiClient != null) {
            if (enabled && mediaController == null)
                startMediaController(googleApiClient);
            else if (!enabled && mediaController != null) {
                mediaController.stop();
                mediaController = null;
            }
        }
    }

    // --- Subsystem Initialization ---

    private void startNotificationForwarder(GoogleApiClient apiClient) {
        try {
            notificationForwarder = new WearOSNotificationForwarder(context);
            notificationForwarder.start(apiClient, retryQueue);
            registerMessageHandler(WearableDataPaths.NOTIF_DISMISS,
                    notificationForwarder::handleDismissMessage);
            registerMessageHandler(WearableDataPaths.NOTIF_REPLY,
                    notificationForwarder::handleReplyMessage);
            Log.i(TAG, "Notification forwarder started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start notification forwarder", e);
        }
    }

    private void startMediaController(GoogleApiClient apiClient) {
        try {
            mediaController = new WearOSMediaController(context);
            mediaController.start(apiClient, retryQueue);
            registerMessageHandler(WearableDataPaths.MEDIA_COMMAND,
                    mediaController::handleCommandMessage);
            registerMessageHandler(WearableDataPaths.MEDIA_DISCONNECT,
                    mediaController::handleDisconnectMessage);
            Log.i(TAG, "Media controller started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start media controller", e);
        }
    }

    // --- Heartbeat ---

    private void handleHeartbeatMessage(MessageEvent event) {
        // Received heartbeat from watch → send ACK
        if (googleApiClient != null) {
            Wearable.MessageApi.sendMessage(googleApiClient,
                    event.getSourceNodeId(),
                    WearableDataPaths.HEARTBEAT_ACK, new byte[0]);
        }
    }

    private void handleHeartbeatAck(MessageEvent event) {
        // ACK handled by WearableNodeManager's internal listener
    }

    // --- Node Listener ---

    private WearableNodeManager.NodeStateListener createNodeListener() {
        return new WearableNodeManager.NodeStateListener() {
            @Override
            public void onNodeConnected(WearableNodeManager.TrackedNode node) {
                Log.i(TAG, "Node connected: " + node.displayName);
            }

            @Override
            public void onNodeDisconnected(WearableNodeManager.TrackedNode node) {
                Log.w(TAG, "Node disconnected: " + node.displayName);
            }

            @Override
            public void onNodeUnhealthy(WearableNodeManager.TrackedNode node) {
                Log.w(TAG, "Node unhealthy: " + node.displayName);
            }

            @Override
            public void onNodeRecovered(WearableNodeManager.TrackedNode node) {
                Log.i(TAG, "Node recovered: " + node.displayName);
            }
        };
    }

    // --- Accessors ---

    public WearableNodeManager getNodeManager() { return nodeManager; }
    public MessageRetryQueue getRetryQueue() { return retryQueue; }
    public boolean isStarted() { return isStarted; }
}
