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
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MarkerImpl extends IMarkerDelegate.Stub {
    public MarkerImpl(MarkerOptions options) {
    }

    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public String getId() throws RemoteException {
        return null;
    }

    @Override
    public void setPosition(LatLng pos) throws RemoteException {

    }

    @Override
    public LatLng getPosition() throws RemoteException {
        return null;
    }

    @Override
    public void setTitle(String title) throws RemoteException {

    }

    @Override
    public String getTitle() throws RemoteException {
        return null;
    }

    @Override
    public void setSnippet(String title) throws RemoteException {

    }

    @Override
    public String getSnippet() throws RemoteException {
        return null;
    }

    @Override
    public void setDraggable(boolean drag) throws RemoteException {

    }

    @Override
    public boolean isDraggable() throws RemoteException {
        return false;
    }

    @Override
    public void showInfoWindow() throws RemoteException {

    }

    @Override
    public void hideInfoWindow() throws RemoteException {

    }

    @Override
    public boolean isInfoWindowShown() throws RemoteException {
        return false;
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {

    }

    @Override
    public boolean isVisible() throws RemoteException {
        return false;
    }

    @Override
    public boolean equalsRemote(IMarkerDelegate other) throws RemoteException {
        return false;
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return 0;
    }

    @Override
    public void todo(IObjectWrapper obj) throws RemoteException {

    }

    @Override
    public void setAnchor(float x, float y) throws RemoteException {

    }

    @Override
    public void setFlat(boolean flat) throws RemoteException {

    }

    @Override
    public boolean isFlat() throws RemoteException {
        return false;
    }

    @Override
    public void setRotation(float rotation) throws RemoteException {

    }

    @Override
    public float getRotation() throws RemoteException {
        return 0;
    }

    @Override
    public void setInfoWindowAnchor(float x, float y) throws RemoteException {

    }

    @Override
    public void setAlpha(float alpha) throws RemoteException {

    }

    @Override
    public float getAlpha() throws RemoteException {
        return 0;
    }
}
