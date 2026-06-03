package org.microg.gms.wearable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Centralized node lifecycle manager.
 *
 * SOLVES:
 * - B-04: Stale client during node refresh (re-verify after await)
 * - Fragmented node tracking across notification/media subsystems
 *
 * DESIGN:
 * - Single ScheduledExecutor for all periodic tasks (heartbeat, reconnect)
 * - ConcurrentHashMap for thread-safe node state
 * - CopyOnWriteArrayList for listener dispatch (safe during iteration)
 * - Observer pattern: subsystems register as NodeStateListeners
 */
public class WearableNodeManager {

    private static final String TAG = "WearNodeManager";

    private static final long HEARTBEAT_INTERVAL_MS = 30_000;
    private static final long HEARTBEAT_TIMEOUT_MS  = 10_000;
    private static final long NODE_STALE_MS         = 120_000;
    private static final long RECONNECT_CHECK_MS    = 15_000;

    private final Context context;
    private final ScheduledExecutorService scheduler;
    private final Handler mainHandler;
    private GoogleApiClient googleApiClient;

    private final Map<String, TrackedNode> trackedNodes = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<NodeStateListener> listeners =
            new CopyOnWriteArrayList<>();
    private final Map<String, Long> pendingHeartbeats = new ConcurrentHashMap<>();

    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> reconnectFuture;
    private volatile boolean isRunning = false;
    private volatile boolean apiConnected = false;

    // --- Tracked Node ---

    public static class TrackedNode {
        public final String nodeId;
        public final String displayName;
        public volatile boolean isNearby;
        public volatile long lastSeenTimestamp;
        public volatile long lastHeartbeatAck;

        public TrackedNode(Node node) {
            this.nodeId = node.getId();
            this.displayName = node.getDisplayName();
            this.isNearby = node.isNearby();
            this.lastSeenTimestamp = System.currentTimeMillis();
            this.lastHeartbeatAck = System.currentTimeMillis();
        }

        public boolean isHealthy() {
            return System.currentTimeMillis() - lastHeartbeatAck < NODE_STALE_MS;
        }

        @Override
        public String toString() {
            return "Node{" + displayName + ", nearby=" + isNearby
                    + ", healthy=" + isHealthy() + "}";
        }
    }

    // --- Listener Interface ---

    public interface NodeStateListener {
        default void onNodeConnected(TrackedNode node) {}
        default void onNodeDisconnected(TrackedNode node) {}
        default void onNodeUnhealthy(TrackedNode node) {}
        default void onNodeRecovered(TrackedNode node) {}
    }

    // --- Lifecycle ---

    public WearableNodeManager(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WearOS-NodeMgr");
            t.setDaemon(true);
            return t;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized void start(GoogleApiClient apiClient) {
        if (isRunning) return;
        this.googleApiClient = apiClient;
        this.apiConnected = apiClient != null && apiClient.isConnected();

        if (!apiConnected) {
            Log.e(TAG, "Cannot start: API client not connected");
            return;
        }

        refreshConnectedNodes();
        startHeartbeat();
        startReconnectChecker();
        registerHeartbeatListener();

        isRunning = true;
        Log.i(TAG, "Started. Nodes: " + trackedNodes.size());
    }

    public synchronized void stop() {
        if (!isRunning) return;

        if (heartbeatFuture != null) heartbeatFuture.cancel(true);
        if (reconnectFuture != null) reconnectFuture.cancel(true);

        for (TrackedNode node : trackedNodes.values()) {
            dispatchNodeDisconnected(node);
        }
        trackedNodes.clear();
        pendingHeartbeats.clear();
        isRunning = false;
        apiConnected = false;

        Log.i(TAG, "Stopped");
    }

    public void addListener(NodeStateListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(NodeStateListener listener) {
        listeners.remove(listener);
    }

    // --- Node Discovery ---

    /**
     * Refresh connected nodes from the Wearable API.
     *
     * FIX FOR B-04: Re-verify googleApiClient.isConnected() after
     * the potentially long await() call. If the client disconnected
     * during the wait, discard the result to prevent stale node entries.
     */
    public void refreshConnectedNodes() {
        if (!apiConnected || googleApiClient == null) return;

        try {
            NodeApi.GetConnectedNodesResult result = Wearable.NodeApi
                    .getConnectedNodes(googleApiClient)
                    .await(10, TimeUnit.SECONDS);

            // B-04 FIX: Re-verify connection after await
            if (googleApiClient == null || !googleApiClient.isConnected()) {
                Log.w(TAG, "Client disconnected during refresh, discarding");
                return;
            }

            if (result == null || result.getNodes() == null) {
                Log.w(TAG, "Failed to get connected nodes");
                return;
            }

            List<String> currentIds = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (Node node : result.getNodes()) {
                currentIds.add(node.getId());
                TrackedNode tracked = trackedNodes.get(node.getId());

                if (tracked == null) {
                    tracked = new TrackedNode(node);
                    trackedNodes.put(node.getId(), tracked);
                    Log.i(TAG, "New node: " + tracked);
                    dispatchNodeConnected(tracked);
                } else {
                    boolean wasUnhealthy = !tracked.isHealthy();
                    tracked.isNearby = node.isNearby();
                    tracked.lastSeenTimestamp = now;
                    if (wasUnhealthy) {
                        dispatchNodeRecovered(tracked);
                    }
                }
            }

            // Remove disconnected nodes
            List<String> toRemove = new ArrayList<>();
            for (String id : trackedNodes.keySet()) {
                if (!currentIds.contains(id)) toRemove.add(id);
            }
            for (String id : toRemove) {
                TrackedNode removed = trackedNodes.remove(id);
                if (removed != null) dispatchNodeDisconnected(removed);
            }

        } catch (IllegalStateException e) {
            Log.w(TAG, "API client invalid during refresh", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception during refresh", e);
        }
    }

    public List<TrackedNode> getHealthyNodes() {
        List<TrackedNode> healthy = new ArrayList<>();
        for (TrackedNode node : trackedNodes.values()) {
            if (node.isHealthy()) healthy.add(node);
        }
        return healthy;
    }

    public boolean hasHealthyNode() {
        for (TrackedNode node : trackedNodes.values()) {
            if (node.isHealthy()) return true;
        }
        return false;
    }

    // --- Heartbeat ---

    private void startHeartbeat() {
        if (heartbeatFuture != null) heartbeatFuture.cancel(false);
        heartbeatFuture = scheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeats() {
        if (!apiConnected || googleApiClient == null) return;
        long now = System.currentTimeMillis();

        for (TrackedNode node : trackedNodes.values()) {
            if (!node.isHealthy()) continue;
            pendingHeartbeats.put(node.nodeId, now);

            Wearable.MessageApi.sendMessage(
                    googleApiClient, node.nodeId,
                    WearableDataPaths.HEARTBEAT, new byte[0]);
        }

        scheduler.schedule(this::checkTimeouts,
                HEARTBEAT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        for (TrackedNode node : trackedNodes.values()) {
            Long sentAt = pendingHeartbeats.remove(node.nodeId);
            if (sentAt != null && node.lastHeartbeatAck < sentAt) {
                if (node.isHealthy()) {
                    Log.w(TAG, "Unhealthy: " + node.displayName);
                    dispatchNodeUnhealthy(node);
                }
            }
        }
    }

    private void registerHeartbeatListener() {
        if (googleApiClient == null) return;
        Wearable.MessageApi.addListener(googleApiClient, event -> {
            if (event == null) return;
            String path = event.getPath();

            if (WearableDataPaths.HEARTBEAT.equals(path)) {
                // Received heartbeat from watch → send ACK
                Wearable.MessageApi.sendMessage(googleApiClient,
                        event.getSourceNodeId(),
                        WearableDataPaths.HEARTBEAT_ACK, new byte[0]);
            } else if (WearableDataPaths.HEARTBEAT_ACK.equals(path)) {
                // Received ACK from watch
                TrackedNode node = trackedNodes.get(event.getSourceNodeId());
                if (node != null) {
                    boolean wasUnhealthy = !node.isHealthy();
                    node.lastHeartbeatAck = System.currentTimeMillis();
                    node.lastSeenTimestamp = System.currentTimeMillis();
                    pendingHeartbeats.remove(node.nodeId);
                    if (wasUnhealthy) dispatchNodeRecovered(node);
                }
            }
        });
    }

    // --- Reconnect Checker ---

    private void startReconnectChecker() {
        if (reconnectFuture != null) reconnectFuture.cancel(false);
        reconnectFuture = scheduler.scheduleAtFixedRate(
                () -> {
                    if (googleApiClient != null && !googleApiClient.isConnected()) {
                        apiConnected = false;
                        for (TrackedNode n : trackedNodes.values())
                            dispatchNodeDisconnected(n);
                        trackedNodes.clear();
                    } else {
                        refreshConnectedNodes();
                    }
                },
                RECONNECT_CHECK_MS, RECONNECT_CHECK_MS,
                TimeUnit.MILLISECONDS);
    }

    // --- Connection State ---

    public void onConnected(GoogleApiClient apiClient) {
        this.googleApiClient = apiClient;
        this.apiConnected = true;
        if (isRunning) refreshConnectedNodes();
    }

    public void onConnectionSuspended(int cause) {
        this.apiConnected = false;
        for (TrackedNode n : trackedNodes.values()) dispatchNodeDisconnected(n);
    }

    // --- Event Dispatch ---

    private void dispatchNodeConnected(TrackedNode node) {
        for (NodeStateListener l : listeners) {
            try { l.onNodeConnected(node); }
            catch (Exception e) { Log.e(TAG, "Listener error", e); }
        }
    }

    private void dispatchNodeDisconnected(TrackedNode node) {
        for (NodeStateListener l : listeners) {
            try { l.onNodeDisconnected(node); }
            catch (Exception e) { Log.e(TAG, "Listener error", e); }
        }
    }

    private void dispatchNodeUnhealthy(TrackedNode node) {
        for (NodeStateListener l : listeners) {
            try { l.onNodeUnhealthy(node); }
            catch (Exception e) { Log.e(TAG, "Listener error", e); }
        }
    }

    private void dispatchNodeRecovered(TrackedNode node) {
        for (NodeStateListener l : listeners) {
            try { l.onNodeRecovered(node); }
            catch (Exception e) { Log.e(TAG, "Listener error", e); }
        }
    }

    // --- Diagnostics ---

    public String dumpState() {
        StringBuilder sb = new StringBuilder("WearableNodeManager:\n");
        sb.append("  Running: ").append(isRunning).append("\n");
        sb.append("  API Connected: ").append(apiConnected).append("\n");
        sb.append("  Nodes: ").append(trackedNodes.size()).append("\n");
        for (TrackedNode n : trackedNodes.values())
            sb.append("    ").append(n).append("\n");
        return sb.toString();
    }
}
