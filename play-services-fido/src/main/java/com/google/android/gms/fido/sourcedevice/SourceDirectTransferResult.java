/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.PublicApi;

/**
 * Result returned from the UI activity in {@link Activity#onActivityResult(int, int, Intent)} after the direct transfer finishes.
 */
@PublicApi
@SafeParcelable.Class
public class SourceDirectTransferResult extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getStatus")
    private Status status;
    @Field(value = 2, getterName = "getAuthenticationType")
    private int authenticationType;

    private SourceDirectTransferResult() {
    }

    @Constructor
    public SourceDirectTransferResult(
            @Param(1) Status status,
            @Param(2) int authenticationType
    ) {
        this.status = status;
        this.authenticationType = authenticationType;
    }

    /**
     * Gets the {@link Status} from the returned {@link SourceDirectTransferResult}.
     */
    public Status getStatus() {
        return status;
    }

    public int getAuthenticationType() {
        return authenticationType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SourceDirectTransferResult> CREATOR = findCreator(SourceDirectTransferResult.class);
}
