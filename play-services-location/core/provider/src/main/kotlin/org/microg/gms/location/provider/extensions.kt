/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.content.Context
import android.location.GeocoderParams
import android.os.Build.VERSION.SDK_INT
import com.google.android.gms.location.internal.ClientIdentity

const val TAG = "LocationProvider"

val GeocoderParams.clientIdentity: ClientIdentity?
    get() = clientPackage?.let {
        ClientIdentity(it).apply {
            if (SDK_INT >= 33) {
                uid = clientUid
                attributionTag = clientAttributionTag
            }
        }
    }

val Context.versionName: String
    get() = packageManager.getPackageInfo(packageName, 0).versionName