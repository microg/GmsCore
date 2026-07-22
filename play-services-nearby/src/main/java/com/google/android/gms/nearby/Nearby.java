/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
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
