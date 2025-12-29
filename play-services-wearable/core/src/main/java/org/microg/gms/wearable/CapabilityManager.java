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
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PackageUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class CapabilityManager {
    private static final String TAG = "CapabilityManager";

    private static final Uri ROOT = Uri.parse("wear:/capabilities/");
    private final Context context;
    private final WearableImpl wearable;
    private final String packageName;

    private final Object lock = new Object();

    private Set<String> capabilities = new HashSet<String>();

    public CapabilityManager(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
    }

    public enum CapabilityType {
        STATIC("s", "+", "+#"),
        DYNAMIC("d", "-", "-#");

        public final String typeCode;
        public final String addSymbol;
        public final String addSymbolWithHash;

        CapabilityType(String typeCode, String addSymbol, String addSymbolWithHash) {
            this.typeCode = typeCode;
            this.addSymbol = addSymbol;
            this.addSymbolWithHash = addSymbolWithHash;
        }

        public static CapabilityType fromBytes(byte[] data) {
            if (data == null || data.length == 0) return DYNAMIC;

            String code = new String(data, 0, 1, StandardCharsets.UTF_8);

            if (STATIC.typeCode.equals(code)) return STATIC;

            return DYNAMIC;
        }

        public byte[] toBytes() {
            return typeCode.getBytes(StandardCharsets.UTF_8);
        }
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
        try{
            for (int i = 0; i < dataHolder.getCount(); i++) {
                nodes.add(dataHolder.getString("host", i, 0));
            }
        } finally {
            dataHolder.close();
        }
        return nodes;
    }

    public int add(String capability) {
        return addWithType(capability, CapabilityType.DYNAMIC);
//        if (this.capabilities.contains(capability)) {
//            return WearableStatusCodes.DUPLICATE_CAPABILITY;
//        }
//        DataItemInternal dataItem = new DataItemInternal(buildCapabilityUri(capability, true));
//        DataItemRecord record = wearable.putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), wearable.getLocalNodeId(), dataItem);
//        this.capabilities.add(capability);
//        wearable.syncRecordToAll(record);
//        return CommonStatusCodes.SUCCESS;
    }

    public int addWithType(String capability, CapabilityType type) {
        synchronized (lock) {
            Uri uri = buildCapabilityUri(capability, true);
            DataHolder existingData = wearable.getDataItemsByUriAsHolder(uri, packageName);

            try {
                if (existingData.getCount() > 0) {
                    byte[] data = existingData.getByteArray("data", 0, 0);
                    CapabilityType existingType = CapabilityType.fromBytes(data);

                    if (existingType == CapabilityType.STATIC || type == CapabilityType.DYNAMIC) {
                        return WearableStatusCodes.DUPLICATE_CAPABILITY;
                    }
                }
            } finally {
                existingData.close();
            }

            DataItemInternal dataItem = new DataItemInternal(uri);
            dataItem.data = type.toBytes();

            DataItemRecord record = wearable.putDataItem(
                    packageName,
                    PackageUtils.firstSignatureDigest(context, packageName),
                    wearable.getLocalNodeId(),
                    dataItem
            );

            if (record != null) {
                capabilities.add(capability);
                wearable.syncRecordToAll(record);
                Log.d(TAG, "Added capability: " + capability + " (type=" + type + ")");
                return CommonStatusCodes.SUCCESS;
            } else {
                Log.e(TAG, "Failed to add capability: " + capability);
                return CommonStatusCodes.ERROR;
            }
        }
    }

    public int remove(String capability) {
        synchronized (lock) {
            if (!capabilities.contains(capability)) {
                Uri uri = buildCapabilityUri(capability, true);
                DataHolder existingData = wearable.getDataItemsByUriAsHolder(uri, packageName);
                try {
                    if (existingData.getCount() == 0) {
                        Log.w(TAG, "Capability not found: " + capability);
                        return WearableStatusCodes.UNKNOWN_CAPABILITY;
                    }
                } finally {
                    existingData.close();
                }
            }

//        if (!this.capabilities.contains(capability)) {
//            return WearableStatusCodes.UNKNOWN_CAPABILITY;
//        }
            wearable.deleteDataItems(buildCapabilityUri(capability, true), packageName);
            capabilities.remove(capability);
            Log.d(TAG, "Removed capability: " + capability);
            return CommonStatusCodes.SUCCESS;
        }
    }

    public Set<String> getLocalCapabilities() {
        synchronized (lock) {
            return new HashSet<>(capabilities);
        }
    }

    public boolean hasCapability(String capability) {
        synchronized (lock) {
            return capabilities.contains(capability);
        }
    }

    public void clearAll() {
        synchronized (lock) {
            for (String capability: new HashSet<>(capabilities)) {
                remove(capability);
            }
        }
    }
}
