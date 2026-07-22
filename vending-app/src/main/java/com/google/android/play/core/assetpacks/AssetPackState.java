/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import com.google.android.play.core.assetpacks.model.AssetPackErrorCode;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import com.google.android.play.core.assetpacks.model.AssetPackUpdateAvailability;

/**
 * The state of an individual asset pack.
 */
public abstract class AssetPackState {
    public abstract String availableVersionTag();

    /**
     * Returns the total number of bytes already downloaded for the pack.
     */
    public abstract long bytesDownloaded();

    /**
     * Returns the error code for the pack, if Play has failed to download the pack. Returns
     * {@link AssetPackErrorCode#NO_ERROR} if the download was successful or is in progress or has not been attempted.
     *
     * @return A value from {@link AssetPackErrorCode}.
     */
    @AssetPackErrorCode
    public abstract int errorCode();

    public abstract String installedVersionTag();

    /**
     * Returns the name of the pack.
     */
    public abstract String name();

    /**
     * Returns the download status of the pack.
     * <p>
     * If the pack has never been requested before its status is {@link AssetPackStatus#UNKNOWN}.
     *
     * @return a value from {@link AssetPackStatus}
     */
    @AssetPackStatus
    public abstract int status();

    /**
     * Returns the total size of the pack in bytes.
     */
    public abstract long totalBytesToDownload();

    /**
     * Returns the percentage of the asset pack already transferred to the app.
     * <p>
     * This value is only defined when the status is {@link AssetPackStatus#TRANSFERRING}.
     *
     * @return a value between 0 and 100 inclusive.
     */
    public abstract int transferProgressPercentage();

    @AssetPackUpdateAvailability
    public abstract int updateAvailability();
}
