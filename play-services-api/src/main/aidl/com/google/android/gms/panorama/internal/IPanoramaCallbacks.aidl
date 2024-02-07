/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.panorama.internal;

import android.content.Intent;

interface IPanoramaCallbacks {
    void getViewerIntent(int status, in Intent intent);
}