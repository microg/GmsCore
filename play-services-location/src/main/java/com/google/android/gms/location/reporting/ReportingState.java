/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.reporting;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class ReportingState extends AbstractSafeParcelable {
    @Field(1)
    @Deprecated
    int versionCode = 2;
    @Field(2)
    public final int reportingEnabled;
    @Field(3)
    public final int historyEnabled;
    @Field(4)
    public final boolean allowed;
    @Field(5)
    public final boolean active;
    @Field(6)
    @Deprecated
    boolean defer;
    @Field(7)
    public final int expectedOptInResult;
    @Field(8)
    public final Integer deviceTag;
    @Field(9)
    public final int expectedOptInResultAssumingLocationEnabled;
    @Field(10)
    public final boolean canAccessSettings;
    @Field(11)
    public final boolean hasMigratedToOdlh;

    @Constructor
    public ReportingState(@Param(2) int reportingEnabled, @Param(3) int historyEnabled, @Param(4) boolean allowed, @Param(5) boolean active, @Param(7) int expectedOptInResult, @Param(9) int expectedOptInResultAssumingLocationEnabled, @Param(8) Integer deviceTag, @Param(10) boolean canAccessSettings, @Param(11) boolean hasMigratedToOdlh) {
        this.reportingEnabled = reportingEnabled;
        this.historyEnabled = historyEnabled;
        this.allowed = allowed;
        this.active = active;
        this.expectedOptInResult = expectedOptInResult;
        this.expectedOptInResultAssumingLocationEnabled = expectedOptInResultAssumingLocationEnabled;
        this.deviceTag = deviceTag;
        this.canAccessSettings = canAccessSettings;
        this.hasMigratedToOdlh = hasMigratedToOdlh;
    }

    public int getDeviceTag() throws SecurityException {
        if (this.deviceTag == null) throw new SecurityException("Device tag restricted to approved apps");
        return deviceTag;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ReportingState")
                .field("reportingEnabled", reportingEnabled)
                .field("historyEnabled", historyEnabled)
                .field("allowed", allowed)
                .field("active", active)
                .field("expectedOptInResult", expectedOptInResult)
                .field("deviceTag", deviceTag == null ? "(hidden-from-unauthorized-caller)" : deviceTag.intValue())
                .field("expectedOptInResultAssumingLocationEnabled", expectedOptInResultAssumingLocationEnabled)
                .field("canAccessSettings", canAccessSettings)
                .field("hasMigratedToOdlh", hasMigratedToOdlh)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<ReportingState> CREATOR = findCreator(ReportingState.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
