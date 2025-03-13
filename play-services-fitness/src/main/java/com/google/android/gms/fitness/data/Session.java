/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;

import java.util.concurrent.TimeUnit;

/**
 * A Session represents a time interval with associated metadata. Sessions provide a mechanism to store user-visible groups of related
 * stream data in a useful and shareable manner, and allows for easy querying of the data in a detailed or aggregated fashion. The start and
 * end times for sessions will be controlled by applications, and can be used to represent user-friendly groupings of activities, such as "bike
 * ride", "marathon training run". Any data in Google Fit which falls within this time range is implicitly associated with the session.
 */
@SafeParcelable.Class
public class Session extends AbstractSafeParcelable {

    /**
     * Name for the parcelable intent extra containing a session. It can be extracted using {@link #extract(Intent)}.
     */
    @NonNull
    public static final String EXTRA_SESSION = "vnd.google.fitness.session";

    /**
     * The common prefix for session MIME types. The MIME type for a particular session will be this prefix followed by the session's activity
     * name.
     * <p>
     * The session's activity type is returned by {@link #getActivity()}. The MIME type can be computed from the activity using {@link #getMimeType(String)}
     */
    @NonNull
    public static final String MIME_TYPE_PREFIX = "vnd.google.fitness.session/";

    @Field(value = 1, getterName = "getStartTimeMillis")
    private final long startTimeMillis;
    @Field(value = 2, getterName = "getEndTimeMillis")
    private final long endTimeMillis;
    @Field(value = 3, getterName = "getName")
    @Nullable
    private final String name;
    @Field(value = 4, getterName = "getIdentifier")
    @NonNull
    private final String identifier;
    @Field(value = 5, getterName = "getDescription")
    @NonNull
    private final String description;
    @Field(value = 7, getterName = "getActivityType")
    private final int activityType;
    @Field(value = 8, getterName = "getApplication")
    private final Application application;
    @Field(value = 9, getterName = "getActiveTimeMillis")
    @Nullable
    private final Long activeTimeMillis;

    @Constructor
    Session(@Param(1) long startTimeMillis, @Param(2) long endTimeMillis, @Param(3) @Nullable String name, @Param(4) @NonNull String identifier, @Param(5) @NonNull String description, @Param(7) int activityType, @Param(8) Application application, @Param(9) @Nullable Long activeTimeMillis) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.name = name;
        this.identifier = identifier;
        this.description = description;
        this.activityType = activityType;
        this.application = application;
        this.activeTimeMillis = activeTimeMillis;
    }

    /**
     * Returns the active time period of the session.
     * <p>
     * Make sure to use {@link #hasActiveTime()} before using this method.
     *
     * @throws IllegalStateException {@link #hasActiveTime()} returns false.
     */
    public long getActiveTime(@NonNull TimeUnit timeUnit) {
        if (activeTimeMillis == null) throw new IllegalStateException("Active time is not set");
        return timeUnit.convert(activeTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the activity associated with this session, if set. Else returns {@link FitnessActivities#UNKNOWN}.
     */
    @NonNull
    public String getActivity() {
        return null; // TODO
    }

    /**
     * Returns the package name for the application responsible for adding the session. or {@code null} if unset/unknown. The {@link PackageManager} can be
     * used to query relevant data on the application, such as the name, icon, or logo.
     */
    @Nullable
    public String getAppPackageName() {
        if (application == null) return null;
        return application.getPackageName();
    }

    /**
     * Returns the description for this session.
     */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Returns the end time for the session, in the given unit since epoch. If the session is ongoing (it hasn't ended yet), this will return 0.
     */
    public long getEndTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(endTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the identifier for this session.
     */
    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the name for this session, if set.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Returns the start time for the session, in the given time unit since epoch. A valid start time is always set.
     */
    public long getStartTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(startTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns whether the session active time is set.
     */
    public boolean hasActiveTime() {
        return activeTimeMillis != null;
    }

    /**
     * Returns whether the session is ongoing. If the session has ended, this will return false.
     */
    public boolean isOngoing() {
        return endTimeMillis == 0;
    }

    Application getApplication() {
        return application;
    }

    int getActivityType() {
        return activityType;
    }

    long getStartTimeMillis() {
        return startTimeMillis;
    }

    long getEndTimeMillis() {
        return endTimeMillis;
    }

    @Nullable
    Long getActiveTimeMillis() {
        return activeTimeMillis;
    }

    /**
     * Extracts the session extra from the given intent, such as a callback intent received after registering to session start/end notifications, or an intent to view a session.
     *
     * @param intent The extracted Session, or {@code null} if the given intent does not contain a Session.
     */
    @Nullable
    public static Session extract(@NonNull Intent intent) {
        return SafeParcelableSerializer.deserializeFromBytes(intent.getByteArrayExtra(EXTRA_SESSION), CREATOR);
    }

    /**
     * Returns the MIME type which describes a Session for a particular activity. The MIME type is used in intents such as the session view
     * intent.
     *
     * @param activity One of the activities in {@link FitnessActivities}.
     */
    @NonNull
    public static String getMimeType(@NonNull String activity) {
        return MIME_TYPE_PREFIX + activity;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Session> CREATOR = findCreator(Session.class);
}
