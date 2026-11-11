/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.sharing.SharingClient;

import org.microg.gms.common.PublicApi;
import org.microg.gms.nearby.ExposureNotificationClientImpl;
import org.microg.gms.nearby.SharingClientImpl;

@PublicApi
public class Nearby {
    public static ExposureNotificationClient getExposureNotificationClient(Context context) {
        return new ExposureNotificationClientImpl(context);
    }

    public static SharingClient getSharingClient(Context context) {
        return new SharingClientImpl(context);
    }

    public static SharingClient getSharingClient(Activity activity) {
        return new SharingClientImpl(activity);
    }
}
