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
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.SessionDataSet;
import com.google.android.gms.fitness.request.SessionReadRequest;
import org.microg.gms.common.Hide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of {@link SessionsApi#readSession(GoogleApiClient, SessionReadRequest)}.
 * Contains all Sessions and their corresponding data sets that matched the filters specified in the {@link SessionReadRequest}.
 * <p>
 * The method {@link #getStatus()} can be used to confirm if the request was successful.
 * <p>
 * In case the calling app is missing the required permissions, the returned status has status code set to
 * {@link FitnessStatusCodes#NEEDS_OAUTH_PERMISSIONS}. In this case the caller should use {@link Status#startResolutionForResult(Activity, int)}
 * to start an intent to get the necessary consent from the user before retrying the request.
 * <p>
 * The method {@link #getSessions()} returns all sessions that are returned for the request. The method {@link #getDataSet(Session, DataType)} returns
 * {@link DataSet} for a particular Session and {@link DataType} from the result.
 * <p>
 * In case the app tried to read data for a custom data type created by another app, the returned status has status code set to
 * {@link FitnessStatusCodes#INCONSISTENT_DATA_TYPE}.
 */
@SafeParcelable.Class
public class SessionReadResult extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSessions")
    @NonNull
    private final List<Session> sessions;
    @Field(value = 2, getterName = "getSessionDataSets")
    @NonNull
    private final List<SessionDataSet> sessionDataSets;
    @Field(value = 3, getterName = "getStatus")
    @NonNull
    private final Status status;

    @Constructor
    @Hide
    public SessionReadResult(@Param(1) @NonNull List<Session> sessions, @Param(2) @NonNull List<SessionDataSet> sessionDataSets, @Param(3) @NonNull Status status) {
        this.sessions = sessions;
        this.sessionDataSets = sessionDataSets;
        this.status = status;
    }

    /**
     * Returns the data sets for a given {@code session} and {@code dataType}. If a specific data source was requested for this data type in the read request, the
     * returned data set is from that source. Else, the default data source for this data type is used. Returns empty if no data for the requested data
     * type is found.
     *
     * @return Data sets for the given session and data type, empty if no data was found. Multiple data sets may be returned for a given type, based
     * on the read request
     * @throws IllegalArgumentException If the given session was not part of getSessions() output.
     */
    @NonNull
    public List<DataSet> getDataSet(@NonNull Session session, @NonNull DataType dataType) {
        if (!sessions.contains(session)) throw new IllegalArgumentException("Attempting to read data for session which was not returned");
        List<DataSet> dataSets = new ArrayList<>();
        for (SessionDataSet sessionDataSet : this.sessionDataSets) {
            if (session.equals(sessionDataSet.session) && dataType.equals(sessionDataSet.dataSet.getDataType())) {
                dataSets.add(sessionDataSet.dataSet);
            }
        }
        return dataSets;
    }

    /**
     * Returns the data sets for all data sources for a given {@code session}. If a specific data source was requested for a data type in the read request,
     * the returned data set is from that source. Else, the default data source for the requested data type is used.
     *
     * @return Data sets for the given session for all data sources, empty if no data was found. Multiple data sets may be returned for a given type,
     * based on the read request
     * @throws IllegalArgumentException If the given session was not part of getSessions() output
     */
    @NonNull
    public List<DataSet> getDataSet(@NonNull Session session) {
        if (!sessions.contains(session)) throw new IllegalArgumentException("Attempting to read data for session which was not returned");
        List<DataSet> dataSets = new ArrayList<>();
        for (SessionDataSet sessionDataSet : sessionDataSets) {
            if (session.equals(sessionDataSet.session)) {
                dataSets.add(sessionDataSet.dataSet);
            }
        }
        return dataSets;
    }

    /**
     * Returns all sessions that matched the requested filters.
     */
    @NonNull
    public List<Session> getSessions() {
        return this.sessions;
    }

    @NonNull
    public Status getStatus() {
        return this.status;
    }

    @NonNull
    List<SessionDataSet> getSessionDataSets() {
        return sessionDataSets;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionReadResult> CREATOR = findCreator(SessionReadResult.class);
}
