/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.ads.formats;

import android.os.Parcel;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Options to configure Google Ad Manager banner requests using an {@link AdLoader}.
 */
@SafeParcelable.Class
public final class AdManagerAdViewOptions extends AbstractSafeParcelable {
    @Field(1)
    public final boolean manualImpressionsEnabled;

    @Constructor
    AdManagerAdViewOptions(@Param(1) boolean manualImpressionsEnabled) {
        this.manualImpressionsEnabled = manualImpressionsEnabled;
    }

    /**
     * Returns {@code true} if manual impression reporting is enabled.
     */
    public boolean getManualImpressionsEnabled() {
        return manualImpressionsEnabled;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AdManagerAdViewOptions> CREATOR = findCreator(AdManagerAdViewOptions.class);
}
