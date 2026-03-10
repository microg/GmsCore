/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SafeParcelable.Class
public class Bucket extends AbstractSafeParcelable {

    /**
     * Type constant denoting that bucketing by time is requested.
     */
    public static final int TYPE_TIME = 1;
    /**
     * Type constant denoting that bucketing by session is requested.
     */
    public static final int TYPE_SESSION = 2;
    /**
     * Type constant denoting that bucketing by activity type is requested.
     */
    public static final int TYPE_ACTIVITY_TYPE = 3;
    /**
     * Type constant denoting that bucketing by individual activity segment is requested.
     */
    public static final int TYPE_ACTIVITY_SEGMENT = 4;

    @Field(value = 1, getterName = "getStartTimeMillis")
    private final long startTimeMillis;
    @Field(value = 2, getterName = "getEndTimeMillis")
    private final long endTimeMillis;
    @Field(value = 3, getterName = "getSession")
    @Nullable
    private final Session session;
    @Field(value = 4, getterName = "getActivityType")
    private final int activityType;
    @Field(value = 5, getterName = "getDataSets")
    private final List<DataSet> dataSets;
    @Field(value = 6, getterName = "getBucketType")
    private final int bucketType;

    @Constructor
    public Bucket(@Param(1) long startTimeMillis, @Param(2) long endTimeMillis, @Nullable @Param(3) Session session, @Param(4) int activityType, @Param(5) List<DataSet> dataSets, @Param(6) int bucketType) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.session = session;
        this.activityType = activityType;
        this.dataSets = dataSets;
        this.bucketType = bucketType;
    }

    /**
     * Returns the activity of the bucket if bucketing by activity was requested, or {@link FitnessActivities#UNKNOWN} otherwise.
     */
    @NonNull
    public String getActivity() {
        // TODO
        return null;
    }

    /**
     * Returns the type of the bucket.
     */
    public int getBucketType() {
        return bucketType;
    }

    /**
     * Returns the data set of requested data type over the time interval of the bucket. Returns null, if data set for the requested type is not found.
     */
    public DataSet getDataSet(@NonNull DataType dataType) {
        for (DataSet dataSet : this.dataSets) {
            if (dataSet.getDataType().equals(dataType)) {
                return dataSet;
            }
        }
        return null;
    }

    /**
     * Returns the requested data sets over the time interval of the bucket.
     */
    public List<DataSet> getDataSets() {
        return dataSets;
    }

    /**
     * Returns the end time of the bucket, in the given time unit since epoch.
     */
    public long getEndTime(TimeUnit timeUnit) {
        return timeUnit.convert(this.endTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the session of the bucket if bucketing by session was requested, {@code null} otherwise.
     */
    @Nullable
    public Session getSession() {
        return session;
    }

    /**
     * Returns the start time of the bucket, in the given time unit since epoch.
     */
    public long getStartTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(this.startTimeMillis, TimeUnit.MILLISECONDS);
    }

    int getActivityType() {
        return activityType;
    }

    long getEndTimeMillis() {
        return endTimeMillis;
    }

    long getStartTimeMillis() {
        return startTimeMillis;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Bucket> CREATOR = findCreator(Bucket.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Bucket")
                .field("startTimeMillis", startTimeMillis)
                .field("endTimeMillis", endTimeMillis)
                .field("session", session)
                .field("activityType", activityType)
                .field("dataSets", dataSets)
                .field("bucketType", bucketType)
                .end();
    }
}
