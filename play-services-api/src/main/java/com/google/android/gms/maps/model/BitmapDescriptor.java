/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import com.google.android.gms.dynamic.IObjectWrapper;

import org.microg.gms.common.PublicApi;

/**
 * Defines an image. For a marker, it can be used to set the image of the marker icon. For a ground
 * overlay, it can be used to set the image to place on the surface of the earth.
 */
@PublicApi
public class BitmapDescriptor {
    private final IObjectWrapper remoteObject;

    public BitmapDescriptor(IObjectWrapper remoteObject) {
        this.remoteObject = remoteObject;
    }

    public IObjectWrapper getRemoteObject() {
        return remoteObject;
    }
}
