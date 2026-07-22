/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.panorama.internal;

import com.google.android.gms.panorama.internal.IPanoramaCallbacks;
import android.os.Bundle;
import android.net.Uri;

interface IPanoramaService {
    void loadPanoramaInfo(IPanoramaCallbacks callback, in Uri uri, in Bundle bundle, boolean needGrantReadUriPermissions) = 0;
}