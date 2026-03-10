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
public class TimelinePath extends AbstractSafeParcelable {
    @Field(1)
    @Deprecated
    List<SegmentPath> paths;
    @Field(2)
    public final Path path;

    @Constructor
    public TimelinePath(@Param(2) Path path) {
        this.path = path;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TimelinePath").value(path).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TimelinePath> CREATOR = findCreator(TimelinePath.class);

    public static class SegmentPath extends AbstractSafeParcelable {
        @Field(1)
        String s1;
        @Field(2)
        public final Path path;

        @Constructor
        public SegmentPath(@Param(2) Path path) {
            this.path = path;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<SegmentPath> CREATOR = findCreator(SegmentPath.class);
    }
}
