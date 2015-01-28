/*
 * Copyright 2014-2015 µg Project Team
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

package org.microg.gms.maps.markup;

import android.os.RemoteException;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.internal.ICircleDelegate;

public class CircleImpl extends ICircleDelegate.Stub {
    private LatLng center;
    private double radius;
    private float zIndex;
    private boolean visible;
    private String id;
    private float strokeWidth;
    private int strokeColor;
    private int fillColor;

    public CircleImpl(CircleOptions options) {
    }

    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public void setCenter(LatLng center) throws RemoteException {
        this.center = center;
    }

    @Override
    public LatLng getCenter() throws RemoteException {
        return center;
    }

    @Override
    public void setRadius(double radius) throws RemoteException {
        this.radius = radius;
    }

    @Override
    public double getRadius() throws RemoteException {
        return radius;
    }

    @Override
    public void setStrokeWidth(float width) throws RemoteException {
        this.strokeWidth = width;
    }

    @Override
    public float getStrokeWidth() throws RemoteException {
        return strokeWidth;
    }

    @Override
    public void setStrokeColor(int color) throws RemoteException {
        this.strokeColor = color;
    }

    @Override
    public int getStrokeColor() throws RemoteException {
        return strokeColor;
    }

    @Override
    public void setFillColor(int color) throws RemoteException {
        this.fillColor = color;
    }

    @Override
    public int getFillColor() throws RemoteException {
        return fillColor;
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
    public boolean equalsRemote(ICircleDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }
}
