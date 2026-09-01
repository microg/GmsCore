/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.play.core.assetpacks.protocol.BundleKeys;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Hide
public class AssetPackStatesImpl extends AssetPackStates {
    private final long totalBytes;
    @NonNull
    private final Map<String, AssetPackState> packStates;

    public AssetPackStatesImpl(long totalBytes, @NonNull Map<String, AssetPackState> packStates) {
        this.totalBytes = totalBytes;
        this.packStates = packStates;
    }

    public static AssetPackStates fromBundle(@NonNull Bundle bundle, @NonNull AssetPackManagerImpl assetPackManager) {
        return fromBundle(bundle, assetPackManager, false);
    }

    @NonNull
    public static AssetPackStates fromBundle(@NonNull Bundle bundle, @NonNull AssetPackManagerImpl assetPackManager, boolean ignoreLocalStatus) {
        long totalBytes = BundleKeys.get(bundle, BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, 0L);
        ArrayList<String> packNames = BundleKeys.get(bundle, BundleKeys.PACK_NAMES);
        Map<String, AssetPackState> packStates = new HashMap<>();
        if (packNames != null) {
            for (String packName : packNames) {
                packStates.put(packName, AssetPackStateImpl.fromBundle(bundle, packName, assetPackManager, ignoreLocalStatus));
            }
        }
        return new AssetPackStatesImpl(totalBytes, packStates);
    }

    @Override
    @NonNull
    public Map<String, AssetPackState> packStates() {
        return packStates;
    }

    @Override
    public long totalBytes() {
        return totalBytes;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AssetPackStates")
                .field("totalBytes", totalBytes)
                .field("packStates", packStates)
                .end();
    }
}
