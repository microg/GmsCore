/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.result;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.Session;

import java.util.List;

@SafeParcelable.Class
public class SessionStopResult extends AbstractSafeParcelable {
    @Field(2)
    public Status status;
    @Field(3)
    public List<Session> sessions;

    @Constructor
    public SessionStopResult(@Param(2) Status status, @Param(3) List list) {
        this.status = status;
        this.sessions = list;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionStopResult> CREATOR = findCreator(SessionStopResult.class);
}
