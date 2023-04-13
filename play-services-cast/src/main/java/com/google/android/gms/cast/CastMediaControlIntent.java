/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.cast;

import android.text.TextUtils;

import java.util.Collection;
import java.util.Locale;

/**
 * Intent constants for use with the Cast MediaRouteProvider. This class also contains utility methods for creating
 * a control category for discovering Cast media routes that support a specific app and/or set of namespaces, to be
 * used with MediaRouteSelector.
 */
public final class CastMediaControlIntent {
    @Deprecated
    public static final String CATEGORY_CAST = "com.google.android.gms.cast.CATEGORY_CAST";
    public static final String ACTION_SYNC_STATUS = "com.google.android.gms.cast.ACTION_SYNC_STATUS";

    /**
     * The application ID for the Cast Default Media Receiver.
     */
    public static final String DEFAULT_MEDIA_RECEIVER_APPLICATION_ID = "CC1AD845";

    /**
     * An error code indicating that a Cast request has failed.
     */
    public static final int ERROR_CODE_REQUEST_FAILED = 1;

    /**
     * An error code indicating that the request could not be processed because the session could not be started.
     */
    public static final int ERROR_CODE_SESSION_START_FAILED = 2;

    /**
     * An error code indicating that the connection to the Cast device has been lost, but the system is actively
     * trying to re-establish the connection.
     */
    public static final int ERROR_CODE_TEMPORARILY_DISCONNECTED = 3;

    /**
     * The extra that contains the ID of the application to launch for an
     * {@link android.support.v7.media.MediaContolIntent#ACTION_START_SESSION} request.
     * The value is expected to be a String.
     */
    public static final String EXTRA_CAST_APPLICATION_ID = "com.google.android.gms.cast.EXTRA_CAST_APPLICATION_ID";
    public static final String EXTRA_CAST_RELAUNCH_APPLICATION = "com.google.android.gms.cast.EXTRA_CAST_RELAUNCH_APPLICATION";
    public static final String EXTRA_CAST_LANGUAGE_CODE = "com.google.android.gms.cast.EXTRA_CAST_LANGUAGE_CODE";
    public static final String EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS = "com.google.android.gms.cast.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS";
    public static final String EXTRA_CUSTOM_DATA = "com.google.android.gms.cast.EXTRA_CUSTOM_DATA";

    /**
     * The extra that indicates whether debug logging should be enabled for the Cast session. The value is expected to be a boolean.
     */
    public static final String EXTRA_DEBUG_LOGGING_ENABLED = "com.google.android.gms.cast.EXTRA_DEBUG_LOGGING_ENABLED";

    /**
     * n error bundle extra for the error code. The value is an integer, and will be one of the {@code ERROR_CODE_*}
     * constants declared in this class.
     */
    public static final String EXTRA_ERROR_CODE = "com.google.android.gms.cast.EXTRA_ERROR_CODE";

    public static final String CATEGORY_CAST_REMOTE_PLAYBACK = "com.google.android.gms.cast.CATEGORY_CAST_REMOTE_PLAYBACK";

    private CastMediaControlIntent() {
    }

    /**
     * Returns a custom control category for discovering Cast devices that support running the specified app, independent of whether the app is running or not.
     *
     * @param applicationId The application ID of the receiver application.
     */
    public static String categoryForCast(String applicationId) {
        return CATEGORY_CAST + "/" + applicationId;
    }

    /**
     * Returns true if the given category is a custom control category for cast devices, specific to an application ID.
     *
     * @param applicationId The application ID of the receiver application.
     */
    public static boolean isCategoryForCast(String category) {
        if (category == null) {
            return false;
        }
        return category.startsWith(CATEGORY_CAST + "/");
    }

    /**
     * Returns a custom control category for discovering Cast devices meeting both application ID and namespace
     * restrictions. See {@link #categoryForCast(Collection)} and {@link #categoryForCast(String)} for more details.
     */
    public static String categoryForCast(String applicationId, Collection<String> namespaces) {
        return CATEGORY_CAST + "" + applicationId + "/" + TextUtils.join(",", namespaces);
    }

    /**
     * Returns a custom control category for discovering Cast devices currently running an application which supports the specified namespaces. Apps supporting additional namespaces beyond those specified here are still considered supported.
     */
    public static String categoryForCast(Collection<String> namespaces) {
        return CATEGORY_CAST + "//" + TextUtils.join(",", namespaces);
    }

    /**
     * Returns a custom control category for discovering Cast devices which support the default Android remote
     * playback actions using the specified Cast player. If the Default Media Receiver is desired, use
     * {@link #DEFAULT_MEDIA_RECEIVER_APPLICATION_ID} as the applicationId.
     *
     * @param applicationId The application ID of the receiver application.
     */
    public static String categoryForRemotePlayback(String applicationId) {
        return CATEGORY_CAST_REMOTE_PLAYBACK + "/" + applicationId;
    }

    /**
     * Returns a custom control category for discovering Cast devices which support the Default Media Receiver.
     */
    public static String categoryForRemotePlayback() {
        return CATEGORY_CAST_REMOTE_PLAYBACK;
    }

    /**
     * Returns an RFC-5646 language tag string fo the given locale.
     */
    public static String languageTagForLocale(Locale locale) {
        StringBuilder sb = new StringBuilder(locale.getLanguage());
        if (!TextUtils.isEmpty(locale.getCountry())) sb.append('-').append(locale.getCountry());
        if (!TextUtils.isEmpty(locale.getVariant())) sb.append('-').append(locale.getVariant());
        return sb.toString();
    }
}
