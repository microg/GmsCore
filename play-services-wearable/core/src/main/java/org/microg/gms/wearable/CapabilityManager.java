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
import android.net.Uri;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PackageUtils;

import java.util.HashSet;
import java.util.Set;

public class CapabilityManager {
    private static final Uri ROOT = Uri.parse("wear:/capabilities/");
    private final Context context;
    private final WearableImpl wearable;
    private final String packageName;

    private Set<String> capabilities = new HashSet<String>();

    public CapabilityManager(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
    }

    private Uri buildCapabilityUri(String capability, boolean withAuthority) {
        Uri.Builder builder = ROOT.buildUpon();
        if (withAuthority) builder.authority(wearable.getLocalNodeId());
        builder.appendPath(packageName);
        builder.appendPath(PackageUtils.firstSignatureDigest(context, packageName));
        builder.appendPath(Uri.encode(capability));
        return builder.build();
    }

    public Set<String> getNodesForCapability(String capability) {
        DataHolder dataHolder = wearable.getDataItemsByUriAsHolder(buildCapabilityUri(capability, false), packageName);
        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < dataHolder.getCount(); i++) {
            nodes.add(dataHolder.getString("host", i, 0));
        }
        dataHolder.close();
        return nodes;
    }

    public int add(String capability) {
        if (this.capabilities.contains(capability)) {
            return WearableStatusCodes.DUPLICATE_CAPABILITY;
        }
        DataItemInternal dataItem = new DataItemInternal(buildCapabilityUri(capability, true));
        DataItemRecord record = wearable.putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), wearable.getLocalNodeId(), dataItem);
        this.capabilities.add(capability);
        wearable.syncRecordToAll(record);
        return CommonStatusCodes.SUCCESS;
    }

    public int remove(String capability) {
        if (!this.capabilities.contains(capability)) {
            return WearableStatusCodes.UNKNOWN_CAPABILITY;
        }
        wearable.deleteDataItems(buildCapabilityUri(capability, true), packageName);
        capabilities.remove(capability);
        return CommonStatusCodes.SUCCESS;
    }

    public java.util.List<com.google.android.gms.wearable.internal.CapabilityInfoParcelable> getAllCapabilities(int nodeFilter) {
        java.util.Map<String, Set<String>> capToHosts = new java.util.LinkedHashMap<>();
        String localNodeId = wearable.getLocalNodeId();
        for (String localCap : this.capabilities) {
            Set<String> hosts = capToHosts.computeIfAbsent(localCap, k -> new HashSet<>());
            if (localNodeId != null) {
                hosts.add(localNodeId);
            }
        }
        Set<String> connectedIds = new HashSet<>();
        if (localNodeId != null) {
            connectedIds.add(localNodeId);
        }
        for (NodeParcelable n : wearable.getConnectedNodesParcelableList()) {
            if (n != null && n.id != null) {
                connectedIds.add(n.id);
            }
        }
        DataHolder dataHolder = wearable.getDataItemsByUriAsHolder(ROOT, packageName);
        try {
            for (int i = 0; i < dataHolder.getCount(); i++) {
                String host = dataHolder.getString("host", i, 0);
                String path = dataHolder.getString("path", i, 0);
                if (path == null) continue;
                String[] segments = path.split("/");
                if (segments.length >= 4) {
                    String decodedCap = Uri.decode(segments[segments.length - 1]);
                    if (decodedCap != null && !decodedCap.isEmpty()) {
                        if (nodeFilter == 0 || connectedIds.contains(host)) {
                            capToHosts.computeIfAbsent(decodedCap, k -> new HashSet<>()).add(host);
                        }
                    }
                }
            }
        } finally {
            dataHolder.close();
        }
        java.util.List<com.google.android.gms.wearable.internal.CapabilityInfoParcelable> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Set<String>> entry : capToHosts.entrySet()) {
            java.util.List<NodeParcelable> nodes = new java.util.ArrayList<>();
            for (String host : entry.getValue()) {
                if (host != null && (nodeFilter == 0 || connectedIds.contains(host))) {
                    nodes.add(new NodeParcelable(host, host));
                }
            }
            result.add(new com.google.android.gms.wearable.internal.CapabilityInfoParcelable(entry.getKey(), nodes));
        }
        return result;
    }
}
