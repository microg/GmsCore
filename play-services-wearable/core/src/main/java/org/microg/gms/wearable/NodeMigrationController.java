package org.microg.gms.wearable;

import android.os.Build;
import android.util.Log;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeMigrationController {
    private static final String TAG = "NodeMigrationController";

    public static final int CTRL_UNKNOWN = 1;
    public static final int CTRL_TERMINATE_ASSOCIATION = 2;
    public static final int CTRL_SUSPEND_SYNC = 3;
    public static final int CTRL_RESUME_SYNC = 4;
    public static final int CTRL_MIGRATION_FAILED = 5;
    public static final int CTRL_ACCOUNT_MATCHING = 6;
    public static final int CTRL_MIGRATION_CANCELLED = 7;

    public final ReentrantReadWriteLock migrationLock = new ReentrantReadWriteLock();
    public final ReentrantReadWriteLock archiveLock = new ReentrantReadWriteLock();

    public final ConcurrentHashMap<String, Set<String>> nodeToCompletedAppsMap = new ConcurrentHashMap<>();
    public final Object denylistLock = new Object();
    public final ConcurrentHashMap<String, Set<String>> nodeToDenylistMap = new ConcurrentHashMap<>();
    public final Map<String, Long> archiveNodeHighwaterMap = new ConcurrentHashMap<>();
    public final Set<String> completedNodes;

    public final AtomicBoolean migrationActive = new AtomicBoolean(false);

    private final AtomicInteger cancellationState = new AtomicInteger(0);

    private final AtomicReference<String> migratingTo = new AtomicReference<>(null);
    private final AtomicReference<String> migratingFrom = new AtomicReference<>(null);

    private final Set<String> suspendedNodes;

    public NodeMigrationController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.completedNodes = ConcurrentHashMap.newKeySet();
            this.suspendedNodes = ConcurrentHashMap.newKeySet();
        } else {
            this.completedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
            this.suspendedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
        }
    }

    public void startMigrationForNode(String nodeId) {
        Log.d(TAG, "Starting migration for node " + nodeId);
        Set<String> slot = Collections.newSetFromMap(new ConcurrentHashMap<>());
        migrationLock.writeLock().lock();
        try {
            Set<String> existing = nodeToCompletedAppsMap.get(nodeId);
            if (existing == null) {
                nodeToCompletedAppsMap.put(nodeId, slot);
            }
            if (existing != null) {
                Log.d(TAG, "Node " + nodeId + " already migrating with completed apps: " + existing);
            }
        } finally {
            migrationLock.writeLock().unlock();
        }
    }

    public void markNodeMigrationCompleted(String nodeId) {
        migrationLock.writeLock().lock();
        try {
            Set<String> completedApps = nodeToCompletedAppsMap.remove(nodeId);
            if (completedApps != null) {
                Log.d(TAG, "Marking " + nodeId + " as completed with apps: " + completedApps);
            }
        } finally {
            migrationLock.writeLock().unlock();
        }

        synchronized (denylistLock) {
            nodeToDenylistMap.remove(nodeId);
        }
    }

    public void markAppMigrationComplete(String nodeId, String packageName) {
        migrationLock.writeLock().lock();
        try {
            Set<String> completedApps = nodeToCompletedAppsMap.get(nodeId);
            if (completedApps != null) {
                completedApps.add(packageName);
                Log.d(TAG, "App " + packageName + " migration complete for node " + nodeId);
            }
        } finally {
            migrationLock.writeLock().unlock();
        }
    }

    public void addCompletedNode(String nodeId) {
        completedNodes.add(nodeId);
    }

    public boolean shouldDeliverEvents(String packageName, String sourceNodeId) {
        synchronized (denylistLock) {
            Set<String> denylist = nodeToDenylistMap.get(sourceNodeId);
            if (denylist != null) {
                return !denylist.contains(packageName);
            }
        }

        migrationLock.readLock().lock();
        try {
            Set<String> completedApps = nodeToCompletedAppsMap.get(sourceNodeId);
            return completedApps == null || completedApps.contains(packageName);
        } finally {
            migrationLock.readLock().unlock();
        }
    }

    public boolean isMigrating(String nodeId) {
        migrationLock.readLock().lock();
        try {
            return nodeToCompletedAppsMap.containsKey(nodeId);
        } finally {
            migrationLock.readLock().unlock();
        }
    }

    public boolean startPhoneSwitchMigration(String newNodeId, String oldNodeId) {
        if (!migrationActive.compareAndSet(false, true)) {
            Log.e(TAG, "startPhoneSwitchMigration: already migrating to " + migratingTo.get());
            return false;
        }

        migratingTo.set(newNodeId);
        migratingFrom.set(oldNodeId);
        cancellationState.set(1);
        Log.i(TAG, "startPhoneSwitchMigration: to="+ newNodeId + " from=" + oldNodeId);
        return true;
    }

    public void setMigratingFromNodeId(String oldNodeId) {
        migratingFrom.set(oldNodeId);
        Log.d(TAG, "setMigratingFromNodeId: " + oldNodeId);
    }

    public void onMigrationSucceeded() {
        String from = migratingFrom.getAndSet(null);
        String to = migratingTo.getAndSet(null);
        migrationActive.set(false);
        cancellationState.set(0);
        if (from != null) {
            suspendedNodes.remove(from);
        }
        Log.i(TAG, "onMigrationSucceeded: to=" + to + " from=" + from);
    }

    public void onMigrationAborted() {
        String from = migratingFrom.getAndSet(null);
        String to = migratingTo.getAndSet(null);
        migrationActive.set(false);
        cancellationState.set(0);
        if (from != null) {
            suspendedNodes.remove(from);
        }
        if (to != null) {
            nodeToCompletedAppsMap.remove(to);
        }
        Log.i(TAG, "onMigrationAborted: to=" + to + " from=" + from);
    }

    public boolean isMigrationActive() {
        return migrationActive.get();
    }

    public String getMigratingToNodeId() {
        return migratingTo.get();
    }

    public String getMigratingFromNodeId() {
        return migratingFrom.get();
    }

    public boolean markCancelled() {
        boolean ok = cancellationState.compareAndSet(1, 3);
        if (ok) {
            Log.d(TAG, "markCancelled: Migration marked cancelled");
        } else {
            Log.w(TAG, "markCancelled: could not cancel, state=" + cancellationState.get());
        }
        return ok;
    }
    
    public boolean isCancelled() {
        return cancellationState.get() == 3;
    }
    
    public void resetCancellable() {
        if (cancellationState.compareAndSet(3, 1)) {
            Log.d(TAG, "resetCancellable: reset to cancellable");
        }
    }

    public void suspendNode(String nodeId) {
        if (suspendedNodes.add(nodeId)) {
            Log.d(TAG, "suspendNode: " + nodeId);
        }
    }

    public void resumeNode(String nodeId) {
        if (suspendedNodes.remove(nodeId)) {
            Log.d(TAG, "resumeNode: " + nodeId);
        }
    }

    public boolean isNodeSuspended(String nodeId) {
        return suspendedNodes.contains(nodeId);
    }
}