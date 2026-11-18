package org.microg.gms.wearable.channel;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.microg.gms.wearable.proto.AppKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okio.ByteString;

public class TrustedPeersService {
    private static final String TAG = "TrustedPeersService";
    private static final String META_TRUSTED_PACKAGES = "wear-trusted-peer-packages";

    private final Context context;

    private final ConcurrentHashMap<AppKey, Set<AppKey>> localTrustMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<AppKey, Set<AppKey>>> remotePeerMaps = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Map<AppKey, AppKey>> resolvedPairs = new ConcurrentHashMap<>();

    public TrustedPeersService(Context context) {
        this.context = context.getApplicationContext();
    }
    public AppKey resolveAppKey(String nodeId, AppKey appKey) {
        Map<AppKey, AppKey> pairs = resolvedPairs.get(nodeId);
        if (pairs != null) {
            for (Map.Entry<AppKey, AppKey> entry : pairs.entrySet()) {
                if (appKey.equals(entry.getValue())) {
                    Log.d(TAG, "Resolved trusted peer " + appKey.packageName
                            + " → " + entry.getKey().packageName + " for node " + nodeId);
                    return entry.getKey();
                }
            }
        }
        return appKey;
    }

    public Set<AppKey> getTrustedRemoteKeysFor(AppKey localAppKey) {
        Set<AppKey> set = localTrustMap.get(localAppKey);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    public void onPackageUpdated(String packageName) {
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_SIGNATURES);
            AppKey localKey = buildAppKey(pi);
            if (localKey == null) return;

            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null || ai.metaData == null || !ai.metaData.containsKey(META_TRUSTED_PACKAGES)) {
                localTrustMap.remove(localKey);
                recalculateAllNodes();
                return;
            }

            String raw = ai.metaData.getString(META_TRUSTED_PACKAGES);
            if (raw == null || raw.isEmpty()) {
                localTrustMap.remove(localKey);
                recalculateAllNodes();
                return;
            }

            Set<AppKey> trusted = parseTrustedPackages(raw);
            if (trusted.isEmpty()) {
                localTrustMap.remove(localKey);
            } else {
                localTrustMap.put(localKey, trusted);
                Log.d(TAG, "Loaded " + trusted.size() + " trusted peers for " + packageName);
            }
            recalculateAllNodes();

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found during trust scan: " + packageName);
            Iterator<AppKey> it = localTrustMap.keySet().iterator();
            while (it.hasNext()) {
                AppKey key = it.next();
                if (key.packageName.equals(packageName)) {
                    it.remove();
                }
            }
            recalculateAllNodes();
        }
    }

    public void onPackageRemoved(String packageName) {
        boolean changed = false;

        Iterator<Map.Entry<AppKey, Set<AppKey>>> it = localTrustMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AppKey, Set<AppKey>> entry = it.next();
            if (entry.getKey().packageName.equals(packageName)) {
                it.remove();
                changed = true;
            }
        }

        if (changed) recalculateAllNodes();
    }

    public void updateRemotePeerMap(String nodeId, Map<AppKey, Set<AppKey>> remoteMap) {
        if (remoteMap == null || remoteMap.isEmpty()) {
            remotePeerMaps.remove(nodeId);
        } else {
            remotePeerMaps.put(nodeId, remoteMap);
        }
        recalculateNode(nodeId);
    }

    public void onNodeDisconnected(String nodeId) {
        remotePeerMaps.remove(nodeId);
        resolvedPairs.remove(nodeId);
    }

    private void recalculateAllNodes() {
        for (String nodeId : remotePeerMaps.keySet()) {
            recalculateNode(nodeId);
        }
        resolvedPairs.keySet().retainAll(remotePeerMaps.keySet());
    }

    private void recalculateNode(String nodeId) {
        Map<AppKey, Set<AppKey>> remoteMap = remotePeerMaps.get(nodeId);
        if (remoteMap == null || remoteMap.isEmpty()) {
            resolvedPairs.remove(nodeId);
            return;
        }

        ConcurrentHashMap<AppKey, AppKey> resolved = new ConcurrentHashMap<>();

        for (Map.Entry<AppKey, Set<AppKey>> localEntry : localTrustMap.entrySet()) {
            AppKey localKey = localEntry.getKey();
            for (AppKey remoteCandidate : localEntry.getValue()) {
                Set<AppKey> remotelyTrusted = remoteMap.get(remoteCandidate);
                if (remotelyTrusted != null && remotelyTrusted.contains(localKey)) {
                    resolved.put(localKey, remoteCandidate);
                    Log.d(TAG, "Resolved bidirectional trust: "
                            + localKey.packageName + " ↔ " + remoteCandidate.packageName
                            + " on node " + nodeId);
                    break;
                }
            }
        }

        if (resolved.isEmpty()) {
            resolvedPairs.remove(nodeId);
        } else {
            resolvedPairs.put(nodeId, resolved);
        }
    }

    private static Set<AppKey> parseTrustedPackages(String raw) {
        Set<AppKey> result = new HashSet<>();
        for (String entry : raw.split(",")) {
            entry = entry.trim();
            int colon = entry.lastIndexOf(':');
            if (colon < 1 || colon == entry.length() - 1) {
                Log.w(TAG, "Skipping malformed trusted-package entry: " + entry);
                continue;
            }
            String pkg = entry.substring(0, colon).trim();
            String digest = entry.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            if (!pkg.isEmpty() && !digest.isEmpty()) {
                result.add(new AppKey(pkg, digest));
            }
        }
        return result;
    }

    private static AppKey buildAppKey(PackageInfo pi) {
        if (pi.signatures == null || pi.signatures.length == 0) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(pi.signatures[0].toByteArray());
            return new AppKey(pi.packageName, bytesToHex(digest));
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-1 unavailable", e);
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
