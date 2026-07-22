/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.pay;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Objects;

/**
 * {@link Parcelable} representing an e-money readiness result.
 */
public class EmoneyReadiness extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getEmoneyReadinessStatus")
    private final @EmoneyReadinessStatus int emoneyReadinessStatus;

    public EmoneyReadiness(@Param(1) @EmoneyReadinessStatus int emoneyReadinessStatus) {
        this.emoneyReadinessStatus = emoneyReadinessStatus;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmoneyReadiness)) return false;

        EmoneyReadiness that = (EmoneyReadiness) o;
        return emoneyReadinessStatus == that.emoneyReadinessStatus;
    }

    /**
     * Gets the int value defined in {@link EmoneyReadinessStatus}.
     */
    public @EmoneyReadinessStatus int getEmoneyReadinessStatus() {
        return emoneyReadinessStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{emoneyReadinessStatus});
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<EmoneyReadiness> CREATOR = findCreator(EmoneyReadiness.class);
}
