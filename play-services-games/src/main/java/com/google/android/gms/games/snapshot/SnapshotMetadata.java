/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Player;

public interface SnapshotMetadata extends Parcelable, Freezable<SnapshotMetadata> {
    long PLAYED_TIME_UNKNOWN = -1L;
    long PROGRESS_VALUE_UNKNOWN = -1L;

    Game getGame();

    Player getOwner();

    String getSnapshotId();

    @Nullable
    Uri getCoverImageUri();

    /** @deprecated */
    @Deprecated
    @Nullable
    String getCoverImageUrl();

    float getCoverImageAspectRatio();

    String getUniqueName();

    String getTitle();

    String getDescription();

    void getDescription(CharArrayBuffer var1);

    long getLastModifiedTimestamp();

    long getPlayedTime();

    boolean hasChangePending();

    long getProgressValue();

    String getDeviceName();
}
