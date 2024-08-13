/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import static com.google.android.gms.games.snapshot.SnapshotColumns.COVER_ICON_IMAGE_HEIGHT;
import static com.google.android.gms.games.snapshot.SnapshotColumns.COVER_ICON_IMAGE_URI;
import static com.google.android.gms.games.snapshot.SnapshotColumns.COVER_ICON_IMAGE_URL;
import static com.google.android.gms.games.snapshot.SnapshotColumns.COVER_ICON_IMAGE_WIDTH;
import static com.google.android.gms.games.snapshot.SnapshotColumns.DESCRIPTION;
import static com.google.android.gms.games.snapshot.SnapshotColumns.DEVICE_NAME;
import static com.google.android.gms.games.snapshot.SnapshotColumns.DURATION;
import static com.google.android.gms.games.snapshot.SnapshotColumns.EXTERNAL_SNAPSHOT_ID;
import static com.google.android.gms.games.snapshot.SnapshotColumns.PENDING_CHANGE_COUNT;
import static com.google.android.gms.games.snapshot.SnapshotColumns.PROGRESS_VALUE;
import static com.google.android.gms.games.snapshot.SnapshotColumns.TITLE;
import static com.google.android.gms.games.snapshot.SnapshotColumns.UNIQUE_NAME;
import static com.google.android.gms.games.snapshot.SnapshotColumns.LAST_MODIFIED_TIMESTAMP;

import android.annotation.SuppressLint;
import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.GameRef;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerRef;

@SuppressLint("ParcelCreator")
public class SnapshotMetadataRef extends DataBufferRef implements SnapshotMetadata {

    private final Game game;
    private final Player player;

    public SnapshotMetadataRef(DataHolder var1, int var2) {
        super(var1, var2);
        this.game = new GameRef(var1, var2);
        this.player = new PlayerRef(var1, var2);
    }

    public final Game getGame() {
        return this.game;
    }

    public final Player getOwner() {
        return this.player;
    }

    public final String getSnapshotId() {
        return this.getString(EXTERNAL_SNAPSHOT_ID);
    }

    public final Uri getCoverImageUri() {
        return this.parseUri(COVER_ICON_IMAGE_URI);
    }

    public final String getCoverImageUrl() {
        return this.getString(COVER_ICON_IMAGE_URL);
    }

    public final float getCoverImageAspectRatio() {
        float var1 = this.getFloat(COVER_ICON_IMAGE_HEIGHT);
        float var2 = this.getFloat(COVER_ICON_IMAGE_WIDTH);
        return var1 == 0.0F ? 0.0F : var2 / var1;
    }

    public final String getUniqueName() {
        return this.getString(UNIQUE_NAME);
    }

    public final String getTitle() {
        return this.getString(TITLE);
    }

    public final String getDescription() {
        return this.getString(DESCRIPTION);
    }

    public final void getDescription(CharArrayBuffer var1) {
        this.copyToBuffer(DESCRIPTION, var1);
    }

    public final long getLastModifiedTimestamp() {
        return this.getLong(LAST_MODIFIED_TIMESTAMP);
    }

    public final long getPlayedTime() {
        return this.getLong(DURATION);
    }

    public final boolean hasChangePending() {
        return this.getInteger(PENDING_CHANGE_COUNT) > 0;
    }

    public final long getProgressValue() {
        return this.getLong(PROGRESS_VALUE);
    }

    public final String getDeviceName() {
        return this.getString(DEVICE_NAME);
    }

    public final int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        this.freeze().writeToParcel(dest, flags);
    }

    @Override
    public SnapshotMetadata freeze() {
        return new SnapshotMetadataEntity(this);
    }
}
