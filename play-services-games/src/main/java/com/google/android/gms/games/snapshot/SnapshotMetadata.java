/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games.snapshot;

import android.database.CharArrayBuffer;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Player;
import org.microg.gms.common.Hide;

/**
 * Data interface for the metadata of a saved game.
 */
public interface SnapshotMetadata extends Freezable<SnapshotMetadata> {
    /**
     * Constant indicating that the played time of a snapshot is unknown.
     */
    long PLAYED_TIME_UNKNOWN = -1;
    /**
     * Constant indicating that the progress value of a snapshot is unknown.
     */
    long PROGRESS_VALUE_UNKNOWN = -1;

    /**
     * Retrieves the aspect ratio of the cover image for this snapshot, if any. This is the ratio of width to height, so a value > 1.0f indicates a
     * landscape image while a value < 1.0f indicates a portrait image. If the snapshot has no cover image, this will return 0.0f.
     *
     * @return The aspect ratio of the cover image, or 0.0f if no image is present.
     */
    float getCoverImageAspectRatio();

    /**
     * Retrieves an image URI that can be used to load the snapshot's cover image. Returns null if the snapshot has no cover image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load this snapshot's cover image, if one is present.
     */
    @Nullable
    Uri getCoverImageUri();

    @Hide
    @Deprecated
    String getCoverImageUrl();

    /**
     * Retrieves the description of this snapshot.
     *
     * @return The description of this snapshot.
     */
    @NonNull
    String getDescription();

    /**
     * Loads the snapshot description into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDescription(@NonNull CharArrayBuffer dataOut);

    /**
     * Retrieves the name of the device that wrote this snapshot, if known.
     *
     * @return The name of the device that wrote this snapshot, or null if not known.
     */
    @Nullable
    String getDeviceName();

    /**
     * Retrieves the game associated with this snapshot.
     *
     * @return The associated game.
     */
    @NonNull
    Game getGame();

    /**
     * Retrieves the last time this snapshot was modified, in millis since epoch.
     *
     * @return The last modification time of this snapshot.
     */
    long getLastModifiedTimestamp();

    /**
     * Retrieves the player that owns this snapshot.
     *
     * @return The owning player.
     */
    @NonNull
    Player getOwner();

    /**
     * Retrieves the played time of this snapshot in milliseconds. This value is specified during the update operation. If not known, returns
     * {@link #PLAYED_TIME_UNKNOWN}.
     *
     * @return The played time of this snapshot in milliseconds, or {@link #PLAYED_TIME_UNKNOWN} if not known.
     */
    long getPlayedTime();

    /**
     * Retrieves the progress value for this snapshot. Can be used to provide automatic conflict resolution (see
     * {@link SnapshotsClient#RESOLUTION_POLICY_HIGHEST_PROGRESS}). If not known, returns {@link #PROGRESS_VALUE_UNKNOWN}.
     *
     * @return Progress value for this snapshot, or {@link #PROGRESS_VALUE_UNKNOWN} if not known.
     */
    long getProgressValue();

    /**
     * Retrieves the ID of this snapshot.
     *
     * @return The ID of this snapshot.
     */
    @NonNull
    String getSnapshotId();

    /**
     * Retrieves the unique identifier of this snapshot. This value can be passed to {@link SnapshotsClient#open(SnapshotMetadata)} to open the
     * snapshot for modification.
     * <p>
     * This name should be unique within the scope of the application.
     *
     * @return Unique identifier of this snapshot.
     */
    @NonNull
    String getUniqueName();

    /**
     * Indicates whether or not this snapshot has any changes pending that have not been uploaded to the server. Once all changes have been
     * flushed to the server, this will return false.
     *
     * @return Whether or not this snapshot has any outstanding changes.
     */
    boolean hasChangePending();
}
