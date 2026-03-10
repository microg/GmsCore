/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.result;

import android.app.Activity;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.Session;
import org.microg.gms.common.Hide;

import java.util.List;

/**
 * Result of {@link SessionsApi#stopSession(GoogleApiClient, String)}.
 * <p>
 * The method {@link #getStatus()} can be used to confirm if the request was successful.
 * <p>
 * In case the calling app is missing the required permissions, the returned status has status code set to
 * {@link FitnessStatusCodes#NEEDS_OAUTH_PERMISSIONS}. In this case the caller should use {@link Status#startResolutionForResult(Activity, int)}
 * to start an intent to get the necessary consent from the user before retrying the request.
 */
@SafeParcelable.Class
public class SessionStopResult extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getStatus")
    @NonNull
    private final Status status;
    @Field(value = 3, getterName = "getSessions")
    @NonNull
    private final List<Session> sessions;

    @Constructor
    @Hide
    public SessionStopResult(@Param(2) @NonNull Status status, @Param(3) @NonNull List<Session> sessions) {
        this.status = status;
        this.sessions = sessions;
    }

    /**
     * Returns the list of sessions that were stopped by the request. Returns an empty list if no active session was stopped.
     */
    @NonNull
    public List<Session> getSessions() {
        return sessions;
    }

    /**
     * Returns the status of the call to Google Fit. {@link Status#isSuccess()} can be used to determine whether the call succeeded. In the case of
     * failure, you can inspect the status to determine the reason.
     */
    @NonNull
    public Status getStatus() {
        return status;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionStopResult> CREATOR = findCreator(SessionStopResult.class);
}
