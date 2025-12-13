/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class AdditionalActivityCandidates extends AbstractSafeParcelable {
    @Field(1)
    public final List<ActivityCandidate> candidates;

    @Constructor
    public AdditionalActivityCandidates(@Param(1) List<ActivityCandidate> candidates) {
        this.candidates = candidates;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AdditionalActivityCandidates").value(candidates).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AdditionalActivityCandidates> CREATOR = findCreator(AdditionalActivityCandidates.class);
}
