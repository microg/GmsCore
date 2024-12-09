/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import com.google.android.play.core.assetpacks.model.AssetPackUpdateAvailability;
import com.google.android.play.core.assetpacks.protocol.BundleKeys;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@Hide
public class AssetPackStateImpl extends AssetPackState {
    private final String name;
    private final @AssetPackStatus int status;
    private final @AssetPackErrorCode int errorCode;
    private final long bytesDownloaded;
    private final long totalBytesToDownload;
    private final int transferProgressPercentage;
    @AssetPackUpdateAvailability
    private final int updateAvailability;
    private final String availableVersionTag;
    private final String installedVersionTag;

    public AssetPackStateImpl(String name, @AssetPackStatus int status, @AssetPackErrorCode int errorCode, long bytesDownloaded, long totalBytesToDownload, int transferProgressPercentage, @AssetPackUpdateAvailability int updateAvailability, String availableVersionTag, String installedVersionTag) {
        this.name = name;
        this.status = status;
        this.errorCode = errorCode;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytesToDownload = totalBytesToDownload;
        this.transferProgressPercentage = transferProgressPercentage;
        this.updateAvailability = updateAvailability;
        this.availableVersionTag = availableVersionTag;
        this.installedVersionTag = installedVersionTag;
    }

    @NonNull
    public static AssetPackState fromBundle(Bundle bundle, @NonNull String name, AssetPackManagerImpl assetPackManager) {
        return fromBundle(bundle, name, assetPackManager, false);
    }

    @NonNull
    public static AssetPackState fromBundle(Bundle bundle, @NonNull String name, AssetPackManagerImpl assetPackManager, boolean ignoreLocalStatus) {
        @AssetPackStatus int remoteStatus =  BundleKeys.get(bundle, BundleKeys.STATUS, name, 0);
        @AssetPackStatus int status = ignoreLocalStatus ? remoteStatus : assetPackManager.getLocalStatus(name, remoteStatus);
        @AssetPackErrorCode int errorCode = BundleKeys.get(bundle, BundleKeys.ERROR_CODE, name, 0);
        long bytesDownloaded = BundleKeys.get(bundle, BundleKeys.BYTES_DOWNLOADED, name, 0L);
        long totalBytesToDownload = BundleKeys.get(bundle, BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, name, 0L);
        int transferProgressPercentage = assetPackManager.getTransferProgressPercentage(name);
        long packVersion = BundleKeys.get(bundle, BundleKeys.PACK_VERSION, name, 0L);
        long packBaseVersion = BundleKeys.get(bundle, BundleKeys.PACK_BASE_VERSION, name, 0L);
        int appVersionCode = BundleKeys.get(bundle, BundleKeys.APP_VERSION_CODE, 0);
        String availableVersionTag = BundleKeys.get(bundle, BundleKeys.PACK_VERSION_TAG, name, Integer.toString(appVersionCode));
        String installedVersionTag = assetPackManager.getInstalledVersionTag(name);
        int updateAvailability = AssetPackUpdateAvailability.UPDATE_NOT_AVAILABLE;
        if (status == AssetPackStatus.COMPLETED && packBaseVersion != 0 && packBaseVersion != packVersion) {
            updateAvailability = AssetPackUpdateAvailability.UPDATE_AVAILABLE;
        }
        return new AssetPackStateImpl(name, status, errorCode, bytesDownloaded, totalBytesToDownload, transferProgressPercentage, updateAvailability, availableVersionTag, installedVersionTag);
    }

    @Override
    public String availableVersionTag() {
        return availableVersionTag;
    }

    @Override
    public long bytesDownloaded() {
        return bytesDownloaded;
    }

    @Override
    @AssetPackErrorCode
    public int errorCode() {
        return errorCode;
    }

    @Override
    public String installedVersionTag() {
        return installedVersionTag;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @AssetPackStatus
    public int status() {
        return status;
    }

    @Override
    public long totalBytesToDownload() {
        return totalBytesToDownload;
    }

    @Override
    public int transferProgressPercentage() {
        return transferProgressPercentage;
    }

    @Override
    @AssetPackUpdateAvailability
    public int updateAvailability() {
        return updateAvailability;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AssetPackState")
                .field("name", name)
                .field("status", status)
                .field("errorCode", errorCode)
                .field("bytesDownloaded", bytesDownloaded)
                .field("totalBytesToDownload", totalBytesToDownload)
                .field("transferProgressPercentage", transferProgressPercentage)
                .field("updateAvailability", updateAvailability)
                .field("availableVersionTag", availableVersionTag)
                .field("installedVersionTag", installedVersionTag)
                .end();
    }
}
