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
public class PeriodSummary extends AbstractSafeParcelable {
    @Field(1)
    public final List<Visit> visits;
    @Field(2)
    public final List<ActivityStatistics> activityStatistics;
    @Field(3)
    public final Date date;

    @Constructor
    public PeriodSummary(@Param(1) List<Visit> visits, @Param(2) List<ActivityStatistics> activityStatistics, @Param(3) Date date) {
        this.visits = visits;
        this.activityStatistics = activityStatistics;
        this.date = date;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PeriodSummary")
                .field("visits", visits)
                .field("activityStatistics", activityStatistics)
                .field("date", date)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PeriodSummary> CREATOR = findCreator(PeriodSummary.class);
}
