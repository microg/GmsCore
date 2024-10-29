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
import com.google.android.gms.fitness.data.SessionDataSet;

import java.util.Collections;
import java.util.List;

@SafeParcelable.Class
public class SessionReadResult extends AbstractSafeParcelable {
    @Field(1)
    public List<Session> sessions;
    @Field(2)
    public List<SessionDataSet> sessionDataSets;
    @Field(3)
    public Status status;

    @Constructor
    public SessionReadResult() {
    }

    public SessionReadResult(@Param(1) List<Session> sessions, @Param(2) List<SessionDataSet> sessionDataSets,
                             Status status) {
        this.sessions = sessions;
        this.sessionDataSets = sessionDataSets;
        this.status = status;
    }

    public static SessionReadResult create(Status status) {
        SessionReadResult result = new SessionReadResult();
        result.sessions = Collections.emptyList();
        result.sessionDataSets = Collections.emptyList();
        result.status = status;
        return result;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionReadResult> CREATOR = findCreator(SessionReadResult.class);
}
