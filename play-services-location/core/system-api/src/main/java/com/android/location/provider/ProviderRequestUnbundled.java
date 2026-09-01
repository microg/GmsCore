/*
 * SPDX-FileCopyrightText: 2012 The Android Open Source Project
 * SPDX-FileCopyrightText: 2014 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import android.os.Build;
import android.os.WorkSource;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.android.internal.location.ProviderRequest;

import java.util.List;

/**
 * This class is an interface to Provider Requests for unbundled applications.
 * <p/>
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 */
public final class ProviderRequestUnbundled {
    public static long INTERVAL_DISABLED;
    public ProviderRequestUnbundled(ProviderRequest request) {
    }

    /**
     * True if this is an active request with a valid location reporting interval, false if this
     * request is inactive and does not require any locations to be reported.
     */
    public boolean getReportLocation() {
        return false;
    }

    /**
     * The interval at which a provider should report location. Will return
     * {@link #INTERVAL_DISABLED} for an inactive request.
     */
    public long getInterval() {
        return 0;
    }

    /**
     * The quality hint for this location request. The quality hint informs the provider how it
     * should attempt to manage any accuracy vs power tradeoffs while attempting to satisfy this
     * provider request.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public int getQuality() {
        throw new UnsupportedOperationException();
    }

    /**
     * The maximum time any location update may be delayed, and thus grouped with following updates
     * to enable location batching. If the maximum update delay is equal to or greater than
     * twice the interval, then the provider may provide batched results if possible. The maximum
     * batch size a provider is allowed to return is the maximum update delay divided by the
     * interval.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public long getMaxUpdateDelayMillis() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether any applicable hardware low power modes should be used to satisfy this request.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isLowPower() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether the provider should ignore all location settings, user consents, power restrictions
     * or any other restricting factors and always satisfy this request to the best of their
     * ability. This should only be used in case of a user initiated emergency.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    public boolean isLocationSettingsIgnored() {
        throw new UnsupportedOperationException();
    }


    /**
     * The full list of location requests contributing to this provider request.
     *
     * @deprecated Do not use.
     */
    @Deprecated
    public @NonNull List<LocationRequestUnbundled> getLocationRequests() {
        throw new UnsupportedOperationException();
    }

    /**
     * The power blame for this provider request.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public @NonNull WorkSource getWorkSource() {
        throw new UnsupportedOperationException();
    }
}
