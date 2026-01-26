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
        loadCapabilities();
    }

    private void loadCapabilities() {
        DataHolder dataHolder = wearable.getDataItemsByUriAsHolder(ROOT, packageName);
        for (int i = 0; i < dataHolder.getCount(); i++) {
            if (wearable.getLocalNodeId().equals(dataHolder.getString("host", i, 0))) {
                String path = dataHolder.getString("path", i, 0);
                String capability = path.substring(path.lastIndexOf('/') + 1);
                this.capabilities.add(Uri.decode(capability));
            }
        }
        dataHolder.close();
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
    public List<CapabilityInfoParcelable> getAllCapabilities() {
        DataHolder dataHolder = wearable.getDataItemsByUriAsHolder(ROOT, packageName);
        List<CapabilityInfoParcelable> result = new ArrayList<>();
        Set<String> capabilityNames = new HashSet<>();
        for (int i = 0; i < dataHolder.getCount(); i++) {
            String path = dataHolder.getString("path", i, 0);
            capabilityNames.add(Uri.decode(path.substring(path.lastIndexOf('/') + 1)));
        }
        for (String capabilityName : capabilityNames) {
            List<NodeParcelable> nodes = new ArrayList<>();
            for (int i = 0; i < dataHolder.getCount(); i++) {
                String path = dataHolder.getString("path", i, 0);
                if (capabilityName.equals(Uri.decode(path.substring(path.lastIndexOf('/') + 1)))) {
                    String host = dataHolder.getString("host", i, 0);
                    nodes.add(new NodeParcelable(host, host));
                }
            }
            result.add(new CapabilityInfoParcelable(capabilityName, nodes));
        }
        dataHolder.close();
        return result;
    }
}
