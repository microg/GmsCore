/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.gms.common.data.BitmapTeleporter;

public interface SnapshotMetadataChange {
    SnapshotMetadataChange EMPTY_CHANGE = new SnapshotMetadataChangeEntity();

    @Nullable
    String getDescription();

    @Nullable
    Long getPlayedTimeMillis();

    @Nullable
    BitmapTeleporter getBitmapTeleporter();

    @Nullable
    Bitmap getCoverImage();

    @Nullable
    Long getProgressValue();

    class Builder {
        private String description;
        private Long playedTime;
        private Long progressValue;
        private BitmapTeleporter coverImageTeleporter;
        private Uri coverImageUri;

        public Builder() {
        }

        public Builder setDescription(String var1) {
            this.description = var1;
            return this;
        }

        public Builder setPlayedTimeMillis(long var1) {
            this.playedTime = var1;
            return this;
        }

        public Builder setProgressValue(long var1) {
            this.progressValue = var1;
            return this;
        }

        public Builder setCoverImage(Bitmap var1) {
            this.coverImageTeleporter = new BitmapTeleporter(var1);
            this.coverImageUri = null;
            return this;
        }

        public Builder fromMetadata(SnapshotMetadata var1) {
            this.description = var1.getDescription();
            this.playedTime = var1.getPlayedTime();
            this.progressValue = var1.getProgressValue();
            if (this.playedTime == -1L) {
                this.playedTime = null;
            }

            this.coverImageUri = var1.getCoverImageUri();
            if (this.coverImageUri != null) {
                this.coverImageTeleporter = null;
            }

            return this;
        }

        public SnapshotMetadataChange build() {
            return new SnapshotMetadataChangeEntity(this.description, this.playedTime, this.coverImageTeleporter, this.coverImageUri, this.progressValue);
        }
    }

}
