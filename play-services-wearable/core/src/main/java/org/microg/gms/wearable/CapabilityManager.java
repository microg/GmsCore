/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.CapabilityPaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** All operations run on the wearable network handler. */
public class CapabilityManager {
    private final Context context;
    private final WearableImpl wearable;
    private final String packageName;

    public CapabilityManager(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
        wearable.networkHandler.post(this::announceDeclared);
    }

    private String signature() {
        return PackageUtils.firstSignatureDigest(context, packageName);
    }

    private Set<String> declared() {
        Set<String> result = new HashSet<>();
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(packageName);
            int id = resources.getIdentifier("android_wear_capabilities", "array", packageName);
            if (id != 0) {
                for (String name : resources.getStringArray(id)) {
                    if (CapabilityPaths.validName(name)) result.add(name);
                }
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
            // An uninstalled application no longer declares capabilities.
        }
        return result;
    }

    private Map<String, Byte> local(String signature) {
        Map<String, Byte> result = new HashMap<>();
        try (Cursor cursor = wearable.getCapabilityRecords()) {
            while (cursor.moveToNext()) {
                if (!wearable.getLocalNodeId().equals(cursor.getString(0))) continue;
                String name = CapabilityPaths.name(cursor.getString(1), packageName, signature);
                if (name == null) continue;
                byte[] data = cursor.getBlob(2);
                result.put(name, data != null && data.length > 0 ? data[0] : (byte) 'd');
            }
        }
        return result;
    }

    private void announceDeclared() {
        try {
            String signature = signature();
            Set<String> declared = declared();
            Map<String, Byte> local = local(signature);
            for (Map.Entry<String, Byte> entry : local.entrySet()) {
                if (entry.getValue() == 's' && !declared.contains(entry.getKey())) {
                    wearable.deleteCapability(CapabilityPaths.path(packageName, signature, entry.getKey()));
                }
            }
            for (String name : declared) {
                if (!local.containsKey(name) || local.get(name) != 's') {
                    wearable.putCapability(CapabilityPaths.path(packageName, signature, name), (byte) 's');
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Package/signature became unavailable before the queued operation ran.
        }
    }

    public List<CapabilityInfoParcelable> getAll(int filter) {
        return snapshot(wearable, packageName, signature(), filter);
    }

    /** Also used for change callbacks, without announcing local capabilities as a side effect. */
    static List<CapabilityInfoParcelable> snapshot(WearableImpl wearable, String packageName,
                                                  String signature, int filter) {
        if (filter != CapabilityApi.FILTER_ALL && filter != CapabilityApi.FILTER_REACHABLE) {
            throw new IllegalArgumentException("Invalid capability filter");
        }
        Map<String, Set<String>> nodes = new HashMap<>();
        try (Cursor cursor = wearable.getCapabilityRecords()) {
            while (cursor.moveToNext()) {
                String name = CapabilityPaths.name(cursor.getString(1), packageName, signature);
                String host = cursor.getString(0);
                if (name == null || !CapabilityPaths.visibleNode(host, wearable.getLocalNodeId(),
                        wearable.connectionForNode(host) != null, filter == CapabilityApi.FILTER_REACHABLE)) continue;
                nodes.computeIfAbsent(name, key -> new HashSet<>()).add(host);
            }
        }
        List<CapabilityInfoParcelable> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : nodes.entrySet()) {
            List<NodeParcelable> nodeList = new ArrayList<>();
            for (String host : entry.getValue()) {
                boolean nearby = wearable.connectionForNode(host) != null;
                nodeList.add(new NodeParcelable(host, host, nearby ? 1 : 0, nearby));
            }
            result.add(new CapabilityInfoParcelable(entry.getKey(), nodeList));
        }
        return result;
    }

    public CapabilityInfoParcelable get(String name, int filter) {
        return snapshot(wearable, packageName, signature(), name, filter);
    }

    static CapabilityInfoParcelable snapshot(WearableImpl wearable, String packageName, String signature,
                                             String name, int filter) {
        if (!CapabilityPaths.validName(name)) throw new IllegalArgumentException("Invalid capability name");
        for (CapabilityInfoParcelable info : snapshot(wearable, packageName, signature, filter)) {
            if (name.equals(info.getName())) return info;
        }
        return new CapabilityInfoParcelable(name, new ArrayList<>());
    }

    public int add(String name) {
        if (!CapabilityPaths.validName(name)) return CommonStatusCodes.DEVELOPER_ERROR;
        String signature = signature();
        if (local(signature).containsKey(name)) return WearableStatusCodes.DUPLICATE_CAPABILITY;
        wearable.putCapability(CapabilityPaths.path(packageName, signature, name), (byte) 'd');
        return CommonStatusCodes.SUCCESS;
    }

    public int remove(String name) {
        if (!CapabilityPaths.validName(name)) return CommonStatusCodes.DEVELOPER_ERROR;
        String signature = signature();
        Byte kind = local(signature).get(name);
        if (kind == null || kind == 's') return WearableStatusCodes.UNKNOWN_CAPABILITY;
        wearable.deleteCapability(CapabilityPaths.path(packageName, signature, name));
        return CommonStatusCodes.SUCCESS;
    }
}
