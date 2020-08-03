/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby;

import android.content.Context;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;

import org.microg.gms.common.PublicApi;
import org.microg.gms.nearby.ExposureNotificationClientImpl;

@PublicApi
public class Nearby {
    public static ExposureNotificationClient getExposureNotificationClient(Context context) {
        return new ExposureNotificationClientImpl(context);
    }
}
