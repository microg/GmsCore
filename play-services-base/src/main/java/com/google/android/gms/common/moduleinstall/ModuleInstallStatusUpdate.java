/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.os.Parcel;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The status update of a {@link ModuleInstallRequest}.
 */
@SafeParcelable.Class
public class ModuleInstallStatusUpdate extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSessionId")
    private final int sessionId;
    @Field(value = 2, getterName = "getInstallState", type = "int")
    private final @InstallState int installState;
    @Field(value = 3, getter = "$object.getProgressInfo() != null ? $object.getProgressInfo().getBytesDownloaded() : null")
    private final Long bytesDownloaded;
    @Field(value = 4, getter = "$object.getProgressInfo() != null ? $object.getProgressInfo().getTotalBytesToDownload() : null")
    private final Long totalBytesToDownload;
    @Field(value = 5, getterName = "getErrorCode")
    private final int errorCode;
    private final @Nullable ProgressInfo progressInfo;

    @Constructor
    @Hide
    public ModuleInstallStatusUpdate(@Param(1) int sessionId, @Param(2) int installState, @Param(3) Long bytesDownloaded, @Param(4) Long totalBytesToDownload, @Param(5) int errorCode) {
        this.sessionId = sessionId;
        this.installState = installState;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytesToDownload = totalBytesToDownload;
        this.errorCode = errorCode;
        if (bytesDownloaded == null || totalBytesToDownload == null || totalBytesToDownload == 0) {
            this.progressInfo = null;
        } else {
            this.progressInfo = new ProgressInfo(bytesDownloaded, totalBytesToDownload);
        }
    }

    /**
     * Returns the error code from {@link ModuleInstallStatusCodes}, or {@link ModuleInstallStatusCodes#SUCCESS} if the install is
     * successful or in progress.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the {@link ModuleInstallStatusUpdate.InstallState} of an optional module install session.
     */
    public @InstallState int getInstallState() {
        return installState;
    }

    /**
     * Returns the download progress information including the bytes downloaded so far and total bytes to download.
     * When there are modules to download, the progress info would be provided on the following state:
     * {@link ModuleInstallStatusUpdate.InstallState#STATE_DOWNLOADING},
     * {@link ModuleInstallStatusUpdate.InstallState#STATE_DOWNLOAD_PAUSED},
     * {@link ModuleInstallStatusUpdate.InstallState#STATE_INSTALLING} and
     * {@link ModuleInstallStatusUpdate.InstallState#STATE_COMPLETED}. Otherwise, this method would return {@code null}.
     */
    public @Nullable ProgressInfo getProgressInfo() {
        return progressInfo;
    }

    /**
     * Returns the session id that corresponding to a {@link ModuleInstallRequest}.
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * The current install state for the install request.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({InstallState.STATE_UNKNOWN, InstallState.STATE_PENDING, InstallState.STATE_DOWNLOADING, InstallState.STATE_CANCELED, InstallState.STATE_COMPLETED, InstallState.STATE_FAILED, InstallState.STATE_INSTALLING, InstallState.STATE_DOWNLOAD_PAUSED})
    @interface InstallState {
        int STATE_UNKNOWN = 0;
        /**
         * The request is pending and will be processed soon.
         */
        int STATE_PENDING = 1;
        /**
         * The optional module download is in progress.
         */
        int STATE_DOWNLOADING = 2;
        /**
         * The optional module download has been canceled.
         */
        int STATE_CANCELED = 3;
        /**
         * Installation is completed; the optional modules are available to the client app.
         */
        int STATE_COMPLETED = 4;
        /**
         * The optional module download or installation has failed.
         */
        int STATE_FAILED = 5;
        /**
         * The optional modules have been downloaded and the installation is in progress.
         */
        int STATE_INSTALLING = 6;
        /**
         * The optional module download has been paused.
         * <p>
         * This usually happens when connectivity requirements can't be met during download. Once the connectivity requirements
         * are met, the download will be resumed automatically.
         */
        int STATE_DOWNLOAD_PAUSED = 7;
    }

    /**
     * Download progress information for an {@link ModuleInstallStatusUpdate}.
     */
    public static class ProgressInfo {
        private final long bytesDownloaded;
        private final long totalBytesToDownload;

        ProgressInfo(long bytesDownloaded, long totalBytesToDownload) {
            this.bytesDownloaded = bytesDownloaded;
            this.totalBytesToDownload = totalBytesToDownload;
        }

        /**
         * Returns the number of bytes downloaded so far.
         */
        public long getBytesDownloaded() {
            return bytesDownloaded;
        }

        /**
         * Returns the total number of bytes to download in this session.
         * <p>
         * The total number of bytes to download is guaranteed to be greater than 0.
         */
        public long getTotalBytesToDownload() {
            return totalBytesToDownload;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ModuleInstallStatusUpdate> CREATOR = findCreator(ModuleInstallStatusUpdate.class);
}
