/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import org.microg.gms.maps.mapbox.GoogleMapImpl

class TileOverlayImpl(private val map: GoogleMapImpl, private val id: String, options: TileOverlayOptions) : ITileOverlayDelegate.Stub() {

}
