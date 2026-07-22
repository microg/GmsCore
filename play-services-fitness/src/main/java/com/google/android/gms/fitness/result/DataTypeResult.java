/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.result;

import android.app.Activity;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataType;
import org.microg.gms.common.Hide;

/**
 * Result of {@link ConfigApi#readDataType(GoogleApiClient, String)}.
 * <p>
 * The method {@link #getStatus()} can be used to confirm if the request was successful. On success, the returned data type can be accessed
 * via {@link #getDataType()}.
 * <p>
 * In case the calling app is missing the required permissions, the returned status has status code set to
 * {@link FitnessStatusCodes#NEEDS_OAUTH_PERMISSIONS}. In this case the caller should use {@link Status#startResolutionForResult(Activity, int)}
 * to start an intent to get the necessary consent from the user before retrying the request.
 * <p>
 * In case the app attempts to read a custom data type created by other app, the returned status has status code set to
 * {@link FitnessStatusCodes#INCONSISTENT_DATA_TYPE}.
 *
 * @deprecated No replacement.
 */
@Deprecated
@SafeParcelable.Class
public class DataTypeResult extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getStatus")
    private final Status status;
    @Field(value = 3, getterName = "getDataType")
    @Nullable
    private final DataType dataType;

    @Constructor
    @Hide
    public DataTypeResult(@Param(1) Status status, @Param(3) @Nullable DataType dataType) {
        this.status = status;
        this.dataType = dataType;
    }

    /**
     * Returns the new custom data type inserted, or {@code null} if the request failed.
     */
    @Nullable
    public DataType getDataType() {
        return dataType;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataTypeResult> CREATOR = findCreator(DataTypeResult.class);

}
