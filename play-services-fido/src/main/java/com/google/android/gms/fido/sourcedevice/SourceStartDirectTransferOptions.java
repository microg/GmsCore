/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Customized options to start direct transfer.
 */
public class SourceStartDirectTransferOptions extends AbstractSafeParcelable {
    /**
     * Value of the callerType if the caller is unknown.
     */
    public static final int CALLER_TYPE_UNKNOWN = 0;
    /**
     * Value of the callerType if the caller is browser.
     */
    public static final int CALLER_TYPE_BROWSER = 2;

    @Field(value = 1, getterName = "getCallerType")
    private int callerType;

    private SourceStartDirectTransferOptions() {
    }

    /**
     * Constructor for the {@link SourceStartDirectTransferOptions}.
     */
    public SourceStartDirectTransferOptions(@Param(1) int callerType) {
        this.callerType = callerType;
    }

    public int getCallerType() {
        return callerType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SourceStartDirectTransferOptions> CREATOR = findCreator(SourceStartDirectTransferOptions.class);
}
