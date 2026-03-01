package org.microg.gms.wearable;

import android.os.Build;
import android.util.Log;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeMigrationController {
    private static final String TAG = "NodeMigrationController";
    public final ReentrantReadWriteLock migrationLock = new ReentrantReadWriteLock();
    public final ReentrantReadWriteLock archiveLock = new ReentrantReadWriteLock();

    public final ConcurrentHashMap<String, Set<String>> nodeToCompletedAppsMap = new ConcurrentHashMap<>();
    public final Object denylistLock = new Object();
    public final ConcurrentHashMap<String, Set<String>> nodeToDenylistMap = new ConcurrentHashMap<>();
    public final Map<String, Long> archiveNodeHighwaterMap = new ConcurrentHashMap<>();
    public final Set<String> completedNodes;

    public NodeMigrationController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.completedNodes = ConcurrentHashMap.newKeySet();
        } else {
            this.completedNodes = null;
        }
    }

    public void startMigrationForNode(String nodeId) {
        Log.d(TAG, "Starting migration for node " + nodeId);
        Set<String> slot = Collections.newSetFromMap(new ConcurrentHashMap<>());
        migrationLock.writeLock().lock();
        try {
            Set<String> existing = nodeToCompletedAppsMap.putIfAbsent(nodeId, slot);
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
}