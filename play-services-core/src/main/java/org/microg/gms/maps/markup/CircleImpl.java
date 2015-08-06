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

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.internal.ICircleDelegate;

import org.microg.gms.maps.GmsMapsTypeHelper;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

//import org.oscim.backend.GL20;

public class CircleImpl extends ICircleDelegate.Stub implements Markup {

    private final String id;
    private final CircleOptions options;
    private final MarkupListener listener;
    private VectorLayer vectorLayer;
    private CircleDrawable circleDrawable;
    private boolean removed = false;

    public CircleImpl(String id, CircleOptions options, MarkupListener listener) {
        this.id = id;
        this.listener = listener;
        this.options = options == null ? new CircleOptions() : options;
    }

    @Override
    public void remove() throws RemoteException {
        listener.remove(this);
        removed = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setCenter(LatLng center) throws RemoteException {
        options.center(center);
        if (vectorLayer != null) update();
    }

    @Override
    public LatLng getCenter() throws RemoteException {
        return options.getCenter();
    }

    @Override
    public void setRadius(double radius) throws RemoteException {
        options.radius(radius);
        if (vectorLayer != null) update();
    }

    @Override
    public double getRadius() throws RemoteException {
        return options.getRadius();
    }

    @Override
    public void setStrokeWidth(float width) throws RemoteException {
        options.strokeWidth(width);
        if (vectorLayer != null) update();
    }

    @Override
    public float getStrokeWidth() throws RemoteException {
        return options.getStrokeWidth();
    }

    @Override
    public void setStrokeColor(int color) throws RemoteException {
        options.strokeColor(color);
        if (vectorLayer != null) update();
    }

    @Override
    public int getStrokeColor() throws RemoteException {
        return options.getStrokeColor();
    }

    @Override
    public void setFillColor(int color) throws RemoteException {
        options.fillColor(color);
        if (vectorLayer != null) update();
        listener.update(this);
    }

    @Override
    public int getFillColor() throws RemoteException {
        return options.getFillColor();
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {
        options.zIndex(zIndex);
    }

    @Override
    public float getZIndex() throws RemoteException {
        return options.getZIndex();
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {
        options.visible(visible);
        if (vectorLayer != null) update();
    }

    @Override
    public boolean isVisible() throws RemoteException {
        return options.isVisible();
    }

    @Override
    public boolean equalsRemote(ICircleDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }

    @Override
    public boolean onClick() {
        return false;
    }

    @Override
    public boolean isValid() {
        return !removed;
    }

    @Override
    public MarkerItem getMarkerItem(Context context) {
        return null;
    }

    @Override
    public Layer getLayer(Context context, Map map) {
        vectorLayer = new VectorLayer(map);
        update();
        return vectorLayer;
    }

    private void update() {
        if (circleDrawable != null) {
            vectorLayer.remove(circleDrawable);
        }
        if (options.isVisible()) {
            circleDrawable = new CircleDrawable(
                    GmsMapsTypeHelper.fromLatLng(options.getCenter()),
                    options.getRadius() / 1000.0,
                    Style.builder()
                            .strokeColor(options.getStrokeColor())
                            .fillColor(options.getFillColor())
                            .strokeWidth(options.getStrokeWidth()).build());
            vectorLayer.add(circleDrawable);
        }
        vectorLayer.update();
    }

    @Override
    public Type getType() {
        return Type.LAYER;
    }
}
