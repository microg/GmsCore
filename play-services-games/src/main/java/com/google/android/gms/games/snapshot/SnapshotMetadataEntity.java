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
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.games.GameEntity;
import com.google.android.gms.games.PlayerEntity;
import org.microg.gms.common.Hide;

@SafeParcelable.Class
public class SnapshotMetadataEntity extends AbstractSafeParcelable implements SnapshotMetadata {

    @Field(value = 1, getterName = "getGame")
    private final GameEntity game;
    @Field(value = 2, getterName = "getOwner")
    private final PlayerEntity player;
    @Field(value = 3, getterName = "getSnapshotId")
    private final String snapshotId;
    @Field(value = 5, getterName = "getCoverImageUri")
    @Nullable
    private final Uri coverImageUri;
    @Field(value = 6, getterName = "getCoverImageUrl")
    @Nullable
    private final String coverImageUrl;
    @Field(value = 7, getterName = "getTitle")
    private final String title;
    @Field(value = 8, getterName = "getDescription")
    private final String description;
    @Field(value = 9, getterName = "getLastModifiedTimestamp")
    private final long lastModifiedTimestamp;
    @Field(value = 10, getterName = "getPlayedTime")
    private final long playedTime;
    @Field(value = 11, getterName = "getCoverImageAspectRatio")
    private final float coverImageAspectRatio;
    @Field(value = 12, getterName = "getUniqueName")
    private final String uniqueName;
    @Field(value = 13, getterName = "hasChangePending")
    private final boolean hasChangePending;
    @Field(value = 14, getterName = "getProgressValue")
    private final long progressValue;
    @Field(value = 15, getterName = "getDeviceName")
    @Nullable
    private final String deviceName;

    @Hide
    @Constructor
    public SnapshotMetadataEntity(@Param(value = 1) GameEntity gameEntity, @Param(value = 2) PlayerEntity playerEntity, @Param(value = 3) String snapshotId, @Param(value = 5) @Nullable Uri coverImageUri,@Param(value = 6) @Nullable String coverImageUrl, @Param(value = 7) String title, @Param(value = 8) String description, @Param(value = 9) long lastModifiedTimestamp, @Param(value = 10) long playedTime, @Param(value = 11) float coverImageAspectRatio, @Param(value = 12) String uniqueName, @Param(value = 13) boolean hasChangePending, @Param(value = 14) long progressValue, @Param(value = 15) @Nullable String deviceName) {
        this.game = gameEntity;
        this.player = playerEntity;
        this.snapshotId = snapshotId;
        this.coverImageUri = coverImageUri;
        this.coverImageUrl = coverImageUrl;
        this.coverImageAspectRatio = coverImageAspectRatio;
        this.title = title;
        this.description = description;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
        this.playedTime = playedTime;
        this.uniqueName = uniqueName;
        this.hasChangePending = hasChangePending;
        this.progressValue = progressValue;
        this.deviceName = deviceName;
    }

    @Override
    public SnapshotMetadata freeze() {
        return this;
    }

    /**
     * Retrieves the aspect ratio of the cover image for this snapshot, if any. This is the ratio of width to height, so a value > 1.0f indicates a
     * landscape image while a value < 1.0f indicates a portrait image. If the snapshot has no cover image, this will return 0.0f.
     *
     * @return The aspect ratio of the cover image, or 0.0f if no image is present.
     */
    @Override
    public float getCoverImageAspectRatio() {
        return this.coverImageAspectRatio;
    }

    /**
     * Retrieves an image URI that can be used to load the snapshot's cover image. Returns null if the snapshot has no cover image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load this snapshot's cover image, if one is present.
     */
    @Override
    @Nullable
    public Uri getCoverImageUri() {
        return this.coverImageUri;
    }

    @Override
    @Nullable
    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    /**
     * Retrieves the description of this snapshot.
     *
     * @return The description of this snapshot.
     */
    @Override
    @NonNull
    public String getDescription() {
        return this.description;
    }

    /**
     * Loads the snapshot description into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    @Override
    public void getDescription(@NonNull CharArrayBuffer dataOut) {
        copyStringToBuffer(this.description, dataOut);
    }

    /**
     * Retrieves the name of the device that wrote this snapshot, if known.
     *
     * @return The name of the device that wrote this snapshot, or null if not known.
     */
    @Override
    @Nullable
    public String getDeviceName() {
        return this.deviceName;
    }

    /**
     * Retrieves the game associated with this snapshot.
     *
     * @return The associated game.
     */
    @Override
    @NonNull
    public GameEntity getGame() {
        return this.game;
    }

    /**
     * Retrieves the last time this snapshot was modified, in millis since epoch.
     *
     * @return The last modification time of this snapshot.
     */
    @Override
    public long getLastModifiedTimestamp() {
        return this.lastModifiedTimestamp;
    }

    /**
     * Retrieves the player that owns this snapshot.
     *
     * @return The owning player.
     */
    @Override
    @NonNull
    public PlayerEntity getOwner() {
        return this.player;
    }

    /**
     * Retrieves the played time of this snapshot in milliseconds. This value is specified during the update operation. If not known, returns
     * {@link #PLAYED_TIME_UNKNOWN}.
     *
     * @return The played time of this snapshot in milliseconds, or {@link #PLAYED_TIME_UNKNOWN} if not known.
     */
    @Override
    public long getPlayedTime() {
        return this.playedTime;
    }

    /**
     * Retrieves the progress value for this snapshot. Can be used to provide automatic conflict resolution (see
     * {@link SnapshotsClient#RESOLUTION_POLICY_HIGHEST_PROGRESS}). If not known, returns {@link #PROGRESS_VALUE_UNKNOWN}.
     *
     * @return Progress value for this snapshot, or {@link #PROGRESS_VALUE_UNKNOWN} if not known.
     */
    @Override
    public long getProgressValue() {
        return this.progressValue;
    }

    /**
     * Retrieves the ID of this snapshot.
     *
     * @return The ID of this snapshot.
     */
    @Override
    @NonNull
    public String getSnapshotId() {
        return this.snapshotId;
    }

    /**
     * Retrieves the unique identifier of this snapshot. This value can be passed to {@link SnapshotsClient#open(SnapshotMetadata)} to open the
     * snapshot for modification.
     * <p>
     * This name should be unique within the scope of the application.
     *
     * @return Unique identifier of this snapshot.
     */
    @Override
    @NonNull
    public String getUniqueName() {
        return this.uniqueName;
    }

    /**
     * Indicates whether or not this snapshot has any changes pending that have not been uploaded to the server. Once all changes have been
     * flushed to the server, this will return false.
     *
     * @return Whether or not this snapshot has any outstanding changes.
     */
    @Override
    public boolean hasChangePending() {
        return this.hasChangePending;
    }

    String getTitle() {
        return this.title;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    private static void copyStringToBuffer(@Nullable String toCopy, @NonNull CharArrayBuffer dataOut) {
        if (toCopy == null || toCopy.isEmpty()) {
            dataOut.sizeCopied = 0;
            return;
        }
        if (dataOut.data == null || dataOut.data.length < toCopy.length()) {
            dataOut.data = toCopy.toCharArray();
        } else {
            toCopy.getChars(0, toCopy.length(), dataOut.data, 0);
        }
        dataOut.sizeCopied = toCopy.length();
    }

    public static final SafeParcelableCreatorAndWriter<SnapshotMetadataEntity> CREATOR = findCreator(SnapshotMetadataEntity.class);
}
