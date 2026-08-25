/*
 * SPDX-FileCopyrightText: 2012, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import com.android.internal.location.ProviderProperties;

/**
 * This class is an interface to Provider Properties for unbundled applications.
 * <p/>
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 */
public final class ProviderPropertiesUnbundled {
    public static ProviderPropertiesUnbundled create(boolean requiresNetwork,
            boolean requiresSatellite, boolean requiresCell, boolean hasMonetaryCost,
            boolean supportsAltitude, boolean supportsSpeed, boolean supportsBearing,
            int powerRequirement, int accuracy) {
        return null;
    }

    public ProviderProperties getProviderProperties() {
        return null;
    }
}
