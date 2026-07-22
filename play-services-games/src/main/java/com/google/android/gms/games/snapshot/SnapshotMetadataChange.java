/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games.snapshot;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;

/**
 * A collection of changes to apply to the metadata of a snapshot. Fields that are not set will retain their current values.
 */
public interface SnapshotMetadataChange {
    /**
     * Returns the new cover image to set for the snapshot.
     */
    @Nullable
    Bitmap getCoverImage();

    /**
     * Returns the new description to set for the snapshot.
     */
    @Nullable
    String getDescription();

    /**
     * Returns the new played time to set for the snapshot.
     */
    @Nullable
    Long getPlayedTimeMillis();

    /**
     * Returns the new progress value to set for the snapshot.
     */
    @Nullable
    Long getProgressValue();
}
