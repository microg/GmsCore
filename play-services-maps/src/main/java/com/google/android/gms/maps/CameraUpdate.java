/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import com.google.android.gms.dynamic.IObjectWrapper;
import org.microg.gms.common.PublicApi;

/**
 * Defines a camera move. An object of this type can be used to modify a map's camera by calling {@link GoogleMap#animateCamera(CameraUpdate)},
 * {@link GoogleMap#animateCamera(CameraUpdate, GoogleMap.CancelableCallback)} or {@link GoogleMap#moveCamera(CameraUpdate)}.
 * <p>
 * To obtain a {@link CameraUpdate} use the factory class {@link CameraUpdateFactory}.
 */
@PublicApi
public class CameraUpdate {
    IObjectWrapper wrapped;

    CameraUpdate(IObjectWrapper wrapped) {
        this.wrapped = wrapped;
    }
}
