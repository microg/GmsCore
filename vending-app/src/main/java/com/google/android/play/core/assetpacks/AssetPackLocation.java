/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import androidx.annotation.Nullable;
import com.google.android.play.core.assetpacks.model.AssetPackStorageMethod;

/**
 * Location of an asset pack on the device.
 */
public abstract class AssetPackLocation {
    /**
     * Returns the file path to the folder containing the pack's assets, if the storage method is
     * {@link AssetPackStorageMethod#STORAGE_FILES}.
     * <p>
     * The files found at this path should not be modified.
     * <p>
     * If the storage method is {@link AssetPackStorageMethod#APK_ASSETS}, this method will return {@code null}. To access assets
     * from packs installed as APKs, use Asset Manager.
     */
    @Nullable
    public abstract String assetsPath();

    /**
     * Returns whether the pack is installed as an APK or extracted into a folder on the filesystem.
     *
     * @return a value from {@link AssetPackStorageMethod}
     */
    @AssetPackStorageMethod
    public abstract int packStorageMethod();

    /**
     * Returns the file path to the folder containing the extracted asset pack, if the storage method is
     * {@link AssetPackStorageMethod#STORAGE_FILES}.
     * <p>
     * The files found at this path should not be modified.
     * <p>
     * If the storage method is {@link AssetPackStorageMethod#APK_ASSETS}, this method will return {@code null}. To access assets
     * from packs installed as APKs, use Asset Manager.
     */
    @Nullable
    public abstract String path();
}
