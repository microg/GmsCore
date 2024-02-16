/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.panorama.internal;

import android.content.Intent;

interface IPanoramaCallbacks {
    void onPanoramaResult(int statusCode, in Bundle statusExtras, int unknown, in Intent viewerIntent);
}