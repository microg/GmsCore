/*
 * Copyright 2013-2015 µg Project Team
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

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.internal.IPolygonDelegate;

import org.microg.gms.maps.GoogleMapImpl;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

import java.util.List;

public class PolygonImpl extends IPolygonDelegate.Stub implements Markup {
    private List<LatLng> points;
    private List holes;
    private float zIndex;
    private boolean geodesic;
    private boolean visible;
    private String id;
    private float strokeWidth;
    private int strokeColor;
    private int fillColor;

    public PolygonImpl(String id, PolygonOptions options, MarkupListener listener) {
        this.id = id;
    }

    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public MarkerItem getMarkerItem(Context context) {
        return null;
    }

    @Override
    public Layer getLayer(Context context, Map map) {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean onClick() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void setPoints(List<LatLng> points) throws RemoteException {
        this.points = points;
    }

    @Override
    public List<LatLng> getPoints() throws RemoteException {
        return points;
    }

    @Override
    public void setHoles(List holes) throws RemoteException {
        this.holes = holes;
    }

    @Override
    public List getHoles() throws RemoteException {
        return holes;
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
    public void setGeodesic(boolean geod) throws RemoteException {
        this.geodesic = geod;
    }

    @Override
    public boolean isGeodesic() throws RemoteException {
        return geodesic;
    }

    @Override
    public boolean equalsRemote(IPolygonDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }
}
