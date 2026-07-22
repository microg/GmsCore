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

package org.microg.gms.maps.vtm.markup;

import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate;

public class GroundOverlayImpl extends IGroundOverlayDelegate.Stub {
    private LatLng position;
    private float transparency;
    private float zIndex;
    private boolean visible;
    private String id;
    private float width;
    private float height;
    private float bearing;

    public GroundOverlayImpl(GroundOverlayOptions options) {
        
    }

    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public void setPosition(LatLng pos) throws RemoteException {
        this.position = pos;
    }

    @Override
    public LatLng getPosition() throws RemoteException {
        return position;
    }

    @Override
    public void setDimension(float dimension) throws RemoteException {
        setDimensions(dimension, dimension);
    }

    @Override
    public void setDimensions(float width, float height) throws RemoteException {
        this.width = width;
        this.height = height;
    }

    @Override
    public float getWidth() throws RemoteException {
        return width;
    }

    @Override
    public float getHeight() throws RemoteException {
        return height;
    }

    @Override
    public void setPositionFromBounds(LatLngBounds bounds) throws RemoteException {

    }

    @Override
    public LatLngBounds getBounds() throws RemoteException {
        return null;
    }

    @Override
    public void setBearing(float bearing) throws RemoteException {
        this.bearing = bearing;
    }

    @Override
    public float getBearing() throws RemoteException {
        return bearing;
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {
        this.zIndex = zIndex;
    }

    @Override
    public float getZIndex() throws RemoteException {
        return zIndex;
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() throws RemoteException {
        return visible;
    }

    @Override
    public void setTransparency(float transparency) throws RemoteException {
        this.transparency = transparency;
    }

    @Override
    public float getTransparency() throws RemoteException {
        return transparency;
    }

    @Override
    public boolean equalsRemote(IGroundOverlayDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }

    @Override
    public void setImage(IObjectWrapper img) throws RemoteException {

    }

    @Override
    public void setClickable(boolean clickable) throws RemoteException {

    }

    @Override
    public boolean isClickable() throws RemoteException {
        return false;
    }

    @Override
    public void setTag(IObjectWrapper obj) throws RemoteException {

    }

    @Override
    public IObjectWrapper getTag() throws RemoteException {
        return null;
    }
}
