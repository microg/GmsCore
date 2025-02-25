/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import com.google.android.play.core.assetpacks.model.AssetPackStorageMethod;

/**
 * Location of a single asset, belonging to an asset pack.
 * <p>
 * If the AssetPackStorageMethod for the pack is {@link AssetPackStorageMethod#APK_ASSETS}, this will be the path to the
 * APK containing the asset, the offset of the asset inside the APK and the size of the asset. The asset file will be
 * uncompressed, unless `bundletool` has been explicitly configured to compress the asset pack.
 * <p>
 * If the AssetPackStorageMethod for the pack is {@link AssetPackStorageMethod#STORAGE_FILES}, this will be the path to
 * the specific asset, the offset will be 0 and the size will be the size of the asset file. The asset file will be
 * uncompressed.
 */
public abstract class AssetLocation {
    /**
     * Returns the file offset where the asset starts, in bytes.
     */
    public abstract long offset();

    /**
     * Returns the path to the file containing the asset.
     */
    public abstract String path();

    /**
     * Returns the size of the asset, in bytes.
     */
    public abstract long size();
}
