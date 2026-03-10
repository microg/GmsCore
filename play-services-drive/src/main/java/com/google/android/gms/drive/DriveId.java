/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.drive;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * A canonical identifier for a Drive resource. The identifier can be converted to a String representation for storage using {@link #encodeToString()}
 * and then later converted back to the ID representation using {@link #decodeFromString(String)}. {@link #equals(Object)} can be used to see if two
 * different identifiers refer to the same resource.
 */
@SafeParcelable.Class
public class DriveId extends AbstractSafeParcelable {
    /**
     * An unknown resource type, meaning the {@link DriveId} corresponds to either a file or a folder.
     */
    public static final int RESOURCE_TYPE_UNKNOWN = -1;
    /**
     * A file resource type, meaning the {@link DriveId} corresponds to a file.
     */
    public static final int RESOURCE_TYPE_FILE = 0;
    /**
     * A folder resource type, meaning the {@link DriveId} corresponds to a folder.
     */
    public static final int RESOURCE_TYPE_FOLDER = 1;

    @Field(value = 2, getterName = "getResourceId")
    @Nullable
    private final String resourceId;
    @Field(value = 3)
    private final long unknown3;
    @Field(value = 4)
    private final long unknown4;
    @Field(value = 5, getterName = "getResourceType", defaultValue = "com.google.android.gms.drive.DriveId.RESOURCE_TYPE_UNKNOWN")
    private final int resourceType;

    @Constructor
    @Hide
    public DriveId(@Param(value = 2) @Nullable String resourceId, @Param(value = 3) long unknown3, @Param(value = 4) long unknown4, @Param(value = 5) int resourceType) {
        this.resourceId = resourceId;
        this.unknown3 = unknown3;
        this.unknown4 = unknown4;
        this.resourceType = resourceType;
    }

    /**
     * Returns a String representation of this {@link DriveId} that can be safely persisted, and from which an equivalent {@link DriveId} can later be
     * reconstructed using {@link #decodeFromString(String)}.
     * <p>
     * The String representation is not guaranteed to be stable over time for a given resource so should never be compared for equality. Always
     * use {@link #decodeFromString(String)} and then {@link #equals(Object)} to compare two identifiers to see if they refer to the same resource.
     * Otherwise, {@link #toInvariantString()} is stable and can be safely used for {@link DriveId} comparison.
     */
    public final String encodeToString() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the remote Drive resource id associated with the resource. May be {@code null} for local resources that have not yet been synchronized to
     * the Drive service.
     */
    @Nullable
    public String getResourceId() {
        return this.resourceId;
    }

    /**
     * Returns the resource type corresponding to this {@link DriveId}. Possible values are {@link #RESOURCE_TYPE_FILE}, {@link #RESOURCE_TYPE_FOLDER} or
     * {@link #RESOURCE_TYPE_UNKNOWN}.
     * <p>
     * The {@link #RESOURCE_TYPE_UNKNOWN} will only be returned if the {@link DriveId} instance has been created using {@link #decodeFromString(String)}, with an
     * old string that was generated and persisted by the client with an old version of Google Play Services. If the client is not encoding, persisting
     * and decoding {@link DriveId}s, this method will always return either {@link #RESOURCE_TYPE_FILE} or {@link #RESOURCE_TYPE_FOLDER}.
     */
    public int getResourceType() {
        return this.resourceType;
    }

    /**
     * Returns an invariant string for this {@link DriveId}. This is stable over time, so for a given {@link DriveId}, this value will always remain the same, and is
     * guaranteed to be unique for each {@link DriveId}. The client can use it directly to compare equality of {@link DriveId}s, since two {@link DriveId}s are equal
     * if and only if its invariant string is equal.
     * <p>
     * Note: This value cannot be used to {@link #decodeFromString(String)}, since it's not meant to encode a {@link DriveId}, but can be useful for client-side
     * string-based {@link DriveId} comparison, or for logging purposes.
     */
    public final String toInvariantString() {
        throw new UnsupportedOperationException();
    }

    /**
     * Decodes the result of {@link #encodeToString()} back into a {@link DriveId}.
     * @throws IllegalArgumentException if the argument is not a valid result of {@link #encodeToString()}.
     */
    public static DriveId decodeFromString (String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DriveId> CREATOR = findCreator(DriveId.class);
}

