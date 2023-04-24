/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import android.os.WorkSource;
import androidx.annotation.Nullable;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ActivityRecognitionRequest extends AutoSafeParcelable {
    @Field(1)
    public long intervalMillis;
    @Field(2)
    public boolean triggerUpdate;
    @Field(3)
    @Nullable
    public WorkSource workSource;
    @Field(4)
    @Nullable
    public String tag;
    @Field(5)
    @Nullable
    public int[] nonDefaultActivities;
    @Field(6)
    public boolean requestSensorData;
    @Field(7)
    @Nullable
    public String accountName;
    @Field(8)
    public long maxReportLatencyMillis;
    @Field(9)
    @Nullable
    public String contextAttributionTag;

    public static final Creator<ActivityRecognitionRequest> CREATOR = new AutoCreator<>(ActivityRecognitionRequest.class);
}
