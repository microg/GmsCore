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
 * Error codes for the download of an asset pack.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@IntDef({AssetPackErrorCode.NO_ERROR, AssetPackErrorCode.APP_UNAVAILABLE, AssetPackErrorCode.PACK_UNAVAILABLE, AssetPackErrorCode.INVALID_REQUEST, AssetPackErrorCode.DOWNLOAD_NOT_FOUND, AssetPackErrorCode.API_NOT_AVAILABLE, AssetPackErrorCode.NETWORK_ERROR, AssetPackErrorCode.ACCESS_DENIED, AssetPackErrorCode.INSUFFICIENT_STORAGE, AssetPackErrorCode.APP_NOT_OWNED, AssetPackErrorCode.PLAY_STORE_NOT_FOUND, AssetPackErrorCode.NETWORK_UNRESTRICTED, AssetPackErrorCode.CONFIRMATION_NOT_REQUIRED, AssetPackErrorCode.UNRECOGNIZED_INSTALLATION, AssetPackErrorCode.INTERNAL_ERROR})
public @interface AssetPackErrorCode {
    int NO_ERROR = 0;
    /**
     * The requesting app is unavailable.
     */
    int APP_UNAVAILABLE = -1;
    /**
     * The requested asset pack isn't available.
     * <p>
     * This can happen if the asset pack wasn't included in the Android App Bundle that was published to the Play Store.
     */
    int PACK_UNAVAILABLE = -2;
    /**
     * The request is invalid.
     */
    int INVALID_REQUEST = -3;
    /**
     * The requested download isn't found.
     */
    int DOWNLOAD_NOT_FOUND = -4;
    /**
     * The Asset Delivery API isn't available.
     */
    int API_NOT_AVAILABLE = -5;
    /**
     * Network error. Unable to obtain the asset pack details.
     */
    int NETWORK_ERROR = -6;
    /**
     * Download not permitted under the current device circumstances (e.g. in background).
     */
    int ACCESS_DENIED = -7;
    /**
     * Asset pack download failed due to insufficient storage.
     */
    int INSUFFICIENT_STORAGE = -10;
    /**
     * The Play Store app is either not installed or not the official version.
     */
    int PLAY_STORE_NOT_FOUND = -11;
    /**
     * Returned if {@link AssetPackManager#showCellularDataConfirmation(Activity)} is called but no asset packs are
     * waiting for Wi-Fi.
     */
    int NETWORK_UNRESTRICTED = -12;
    /**
     * The app isn't owned by any user on this device. An app is "owned" if it has been installed via the Play Store.
     */
    int APP_NOT_OWNED = -13;
    /**
     * Returned if {@link AssetPackManager#showConfirmationDialog(Activity)} is called but no asset packs require user
     * confirmation.
     */
    int CONFIRMATION_NOT_REQUIRED = -14;
    /**
     * The installed app version is not recognized by Play. This can happen if the app was not installed by Play.
     */
    int UNRECOGNIZED_INSTALLATION = -15;
    /**
     * Unknown error downloading an asset pack.
     */
    int INTERNAL_ERROR = -100;
}
