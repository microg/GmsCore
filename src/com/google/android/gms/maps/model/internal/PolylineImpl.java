/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model.internal;

import android.os.RemoteException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Collections;
import java.util.List;

/**
 * TODO
 */
public class PolylineImpl extends IPolylineDelegate.Stub {
    private List<LatLng> points;
    private float zIndex;
    private boolean geodesic;
    private boolean visible;
    private String id;
    private float width;
    private int color;

    public PolylineImpl(PolylineOptions options) {

    }

    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public void setPoints(List<LatLng> points) throws RemoteException {
        this.points = points;
    }

    @Override
    public List<LatLng> getPoints() throws RemoteException {
        return points == null ? Collections.<LatLng>emptyList() : points;
    }

    @Override
    public void setWidth(float width) throws RemoteException {
        this.width = width;
    }

    @Override
    public float getWidth() throws RemoteException {
        return width;
    }

    @Override
    public void setColor(int color) throws RemoteException {
        this.color = color;
    }

    @Override
    public int getColor() throws RemoteException {
        return color;
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
    public void setGeodesic(boolean geod) throws RemoteException {
        this.geodesic = geod;
    }

    @Override
    public boolean isGeodesic() throws RemoteException {
        return geodesic;
    }

    @Override
    public boolean equalsRemote(IPolylineDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }
}
