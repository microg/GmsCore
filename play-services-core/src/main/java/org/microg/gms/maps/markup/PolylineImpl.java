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
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;

import org.microg.gms.maps.GmsMapsTypeHelper;
import org.oscim.layers.Layer;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

import java.util.List;

/**
 * TODO
 */
public class PolylineImpl extends IPolylineDelegate.Stub implements Markup {
    private static final String TAG = "GmsMapsPolylineImpl";

    private final String id;
    private final PolylineOptions options;
    private final MarkupListener listener;
    private boolean removed = false;
    private PathLayer pathLayer;

    public PolylineImpl(String id, PolylineOptions options, MarkupListener listener) {
        this.id = id;
        this.options = options == null ? new PolylineOptions() : options;
        this.listener = listener;
    }

    @Override
    public void remove() throws RemoteException {
        listener.remove(this);
        removed = true;
    }

    @Override
    public MarkerItem getMarkerItem(Context context) {
        return null;
    }

    @Override
    public Layer getLayer(Context context, Map map) {
        pathLayer = new PathLayer(map, options.getColor(), options.getWidth());
        for (LatLng point : options.getPoints()) {
            pathLayer.addPoint(GmsMapsTypeHelper.fromLatLng(point));
        }
        return pathLayer;
    }

    @Override
    public Type getType() {
        return Type.LAYER;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean onClick() {
        return listener.onClick(this);
    }

    @Override
    public boolean isValid() {
        return !removed;
    }

    @Override
    public void setPoints(List<LatLng> points) throws RemoteException {
        options.getPoints().clear();
        options.getPoints().addAll(points);
        if (pathLayer != null) {
            pathLayer.clearPath();
            for (LatLng point : points) {
                pathLayer.addPoint(GmsMapsTypeHelper.fromLatLng(point));
            }
            pathLayer.update();
        }
        listener.update(this);
    }

    @Override
    public List<LatLng> getPoints() throws RemoteException {
        return options.getPoints();
    }

    @Override
    public void setWidth(float width) throws RemoteException {
        options.width(width);
        if (pathLayer != null) {
            pathLayer.setStyle(options.getColor(), options.getWidth());
            pathLayer.update();
        }
        listener.update(this);
    }

    @Override
    public float getWidth() throws RemoteException {
        return options.getWidth();
    }

    @Override
    public void setColor(int color) throws RemoteException {
        this.options.color(color);
        if (pathLayer != null) {
            pathLayer.setStyle(options.getColor(), options.getWidth());
            pathLayer.update();
        }
        listener.update(this);
    }

    @Override
    public int getColor() throws RemoteException {
        return options.getColor();
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {
        options.zIndex(zIndex);
        listener.update(this);
    }

    @Override
    public float getZIndex() throws RemoteException {
        return options.getZIndex();
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {
        options.visible(visible);
        if (pathLayer != null) pathLayer.setEnabled(visible);
        listener.update(this);
    }

    @Override
    public boolean isVisible() throws RemoteException {
        return options.isVisible();
    }

    @Override
    public void setGeodesic(boolean geod) throws RemoteException {
        options.geodesic(geod);
        listener.update(this);
    }

    @Override
    public boolean isGeodesic() throws RemoteException {
        return options.isGeodesic();
    }

    @Override
    public boolean equalsRemote(IPolylineDelegate other) throws RemoteException {
        Log.d(TAG, "equalsRemote");
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        Log.d(TAG, "hashcodeRemote");
        return id.hashCode();
    }
}
