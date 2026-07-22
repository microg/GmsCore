/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaCameraChangeListener;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaClickListener;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaLongClickListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;
import com.google.android.gms.maps.model.StreetViewSource;

interface IStreetViewPanoramaDelegate {
    void enableZoom(boolean enableZoom) = 0;
    void enablePanning(boolean enablePanning) = 1;
    void enableUserNavigation(boolean enableUserNavigation) = 2;
    void enableStreetNames(boolean enableStreetNames) = 3;
    boolean isZoomGesturesEnabled() = 4;
    boolean isPanningGesturesEnabled() = 5;
    boolean isUserNavigationEnabled() = 6;
    boolean isStreetNamesEnabled() = 7;
    void animateTo(in StreetViewPanoramaCamera streetViewPanoramaCamera, long duration) = 8;
    StreetViewPanoramaCamera getPanoramaCamera() = 9;
    void setPositionWithID(String panoramaId) = 10;
    void setPosition(in LatLng position) = 11;
    void setPositionWithRadius(in LatLng position, int radius) = 12;
    StreetViewPanoramaLocation getStreetViewPanoramaLocation() = 13;
    void setOnStreetViewPanoramaChangeListener(IOnStreetViewPanoramaChangeListener listener) = 14;
    void setOnStreetViewPanoramaCameraChangeListener(IOnStreetViewPanoramaCameraChangeListener listener) = 15;
    void setOnStreetViewPanoramaClickListener(IOnStreetViewPanoramaClickListener listener) = 16;
    StreetViewPanoramaOrientation pointToOrientation(in IObjectWrapper point) = 17;
    IObjectWrapper orientationToPoint(in StreetViewPanoramaOrientation orientation) = 18;
    void setOnStreetViewPanoramaLongClickListener(IOnStreetViewPanoramaLongClickListener listener) = 19;
    void setPositionWithSource(in LatLng position, in StreetViewSource source) = 20;
    void setPositionWithRadiusAndSource(in LatLng position, int radius, in StreetViewSource source) = 21;
}
