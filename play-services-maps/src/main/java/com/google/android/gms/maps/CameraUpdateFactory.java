/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.os.RemoteException;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.CameraPosition;
import org.microg.gms.common.Hide;

/**
 * A class containing methods for creating {@link CameraUpdate} objects that change a map's camera. To modify the map's camera, call
 * {@link GoogleMap#animateCamera(CameraUpdate)}, {@link GoogleMap#animateCamera(CameraUpdate, GoogleMap.CancelableCallback)} or
 * {@link GoogleMap#moveCamera(CameraUpdate)}, using a {@link CameraUpdate} object created with this class.
 */
public class CameraUpdateFactory {
    private static ICameraUpdateFactoryDelegate delegate;
    @Hide
    public static void setDelegate(@NonNull ICameraUpdateFactoryDelegate delegate) {
        CameraUpdateFactory.delegate = delegate;
    }
    private static ICameraUpdateFactoryDelegate getDelegate() {
        if (delegate == null) throw new IllegalStateException("CameraUpdateFactory is not initialized");
        return delegate;
    }

    /**
     * Returns a {@link CameraUpdate} that moves the camera to a specified {@link CameraPosition}. In effect, this creates a transformation from the
     * {@link CameraPosition} object's latitude, longitude, zoom level, bearing and tilt.
     *
     * @param cameraPosition The requested position. Must not be {@code null}.
     * @return a {@link CameraUpdate} containing the transformation.
     */
    public static CameraUpdate newCameraPosition(@NonNull CameraPosition cameraPosition) {
        try {
            return new CameraUpdate(getDelegate().newCameraPosition(cameraPosition));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

}
