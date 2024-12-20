/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;

interface IOnStreetViewPanoramaClickListener {
    void onStreetViewPanoramaClick(in StreetViewPanoramaOrientation orientation);
}
