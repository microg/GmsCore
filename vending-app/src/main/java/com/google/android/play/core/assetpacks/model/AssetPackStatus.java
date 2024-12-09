/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.model;

import android.app.Activity;
import androidx.annotation.IntDef;
import com.google.android.play.core.assetpacks.AssetPackManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Status of the download of an asset pack.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@IntDef({AssetPackStatus.UNKNOWN, AssetPackStatus.PENDING, AssetPackStatus.DOWNLOADING, AssetPackStatus.TRANSFERRING, AssetPackStatus.COMPLETED, AssetPackStatus.FAILED, AssetPackStatus.CANCELED, AssetPackStatus.WAITING_FOR_WIFI, AssetPackStatus.NOT_INSTALLED, AssetPackStatus.REQUIRES_USER_CONFIRMATION})
public @interface AssetPackStatus {
    /**
     * The asset pack state is unknown.
     */
    int UNKNOWN = 0;
    /**
     * The asset pack download is pending and will be processed soon.
     */
    int PENDING = 1;
    /**
     * The asset pack download is in progress.
     */
    int DOWNLOADING = 2;
    /**
     * The asset pack is being decompressed and copied (or patched) to the app's internal storage.
     */
    int TRANSFERRING = 3;
    /**
     * The asset pack download and transfer is complete; the assets are available to the app.
     */
    int COMPLETED = 4;
    /**
     * The asset pack download or transfer has failed.
     */
    int FAILED = 5;
    /**
     * The asset pack download has been canceled by the user through the Play Store or the download notification.
     */
    int CANCELED = 6;
    /**
     * The asset pack download is waiting for Wi-Fi to become available before proceeding.
     * <p>
     * The app can ask the user to download a session that is waiting for Wi-Fi over cellular data by using
     * {@link AssetPackManager#showCellularDataConfirmation(Activity)}.
     */
    int WAITING_FOR_WIFI = 7;
    /**
     * The asset pack is not installed.
     */
    int NOT_INSTALLED = 8;
    /**
     * The asset pack requires user consent to be downloaded.
     * <p>
     * This can happen if the current app version was not installed by Play.
     * <p>
     * If the asset pack is also waiting for Wi-Fi, this state takes precedence.
     */
    int REQUIRES_USER_CONFIRMATION = 9;
}
