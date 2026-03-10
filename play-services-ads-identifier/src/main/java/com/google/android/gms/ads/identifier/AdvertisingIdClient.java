/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.ads.identifier;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;

/**
 * Helper library for retrieval of advertising ID and related information such as the limit ad tracking setting.
 * <p>
 * It is intended that the advertising ID completely replace existing usage of other identifiers for ads purposes (such as use
 * of {@code ANDROID_ID} in {@link Settings.Secure}) when Google Play Services is available. Cases where Google Play Services is
 * unavailable are indicated by a {@link GooglePlayServicesNotAvailableException} being thrown by getAdvertisingIdInfo().
 */
public class AdvertisingIdClient {
    /**
     * Retrieves the user's advertising ID and limit ad tracking preference.
     * <p>
     * This method cannot be called in the main thread as it may block leading to ANRs. An {@code IllegalStateException} will be
     * thrown if this is called on the main thread.
     *
     * @param context Current {@link Context} (such as the current {@link Activity}).
     * @return AdvertisingIdClient.Info with user's advertising ID and limit ad tracking preference.
     * @throws IOException                             signaling connection to Google Play Services failed.
     * @throws IllegalStateException                   indicating this method was called on the main thread.
     * @throws GooglePlayServicesNotAvailableException indicating that Google Play is not installed on this device.
     * @throws GooglePlayServicesRepairableException   indicating that there was a recoverable error connecting to Google Play Services.
     */
    public static Info getAdvertisingIdInfo(Context context) {
        // We don't actually implement this functionality, but always claim that ad tracking was limited by user preference
        return new Info("00000000-0000-0000-0000-000000000000", true);
    }

    /**
     * Includes both the advertising ID as well as the limit ad tracking setting.
     */
    public static class Info {
        private final String advertisingId;
        private final boolean limitAdTrackingEnabled;

        /**
         * Constructs an {@code Info} Object with the specified advertising Id and limit ad tracking setting.
         *
         * @param advertisingId          The advertising ID.
         * @param limitAdTrackingEnabled The limit ad tracking setting. It is true if the user has limit ad tracking enabled. False, otherwise.
         */
        public Info(String advertisingId, boolean limitAdTrackingEnabled) {
            this.advertisingId = advertisingId;
            this.limitAdTrackingEnabled = limitAdTrackingEnabled;
        }

        /**
         * Retrieves the advertising ID.
         */
        public String getId() {
            return advertisingId;
        }

        /**
         * Retrieves whether the user has limit ad tracking enabled or not.
         * <p>
         * When the returned value is true, the returned value of {@link #getId()} will always be
         * {@code 00000000-0000-0000-0000-000000000000} starting with Android 12.
         */
        public boolean isLimitAdTrackingEnabled() {
            return limitAdTrackingEnabled;
        }
    }
}
