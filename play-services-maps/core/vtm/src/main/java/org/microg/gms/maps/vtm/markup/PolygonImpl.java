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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.internal.IPolygonDelegate;

import org.microg.gms.maps.vtm.GmsMapsTypeHelper;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

public class PolygonImpl extends IPolygonDelegate.Stub implements DrawableMarkup {
    private static final String TAG = "GmsMapsPolygonImpl";

    private final String id;
    private final PolygonOptions options;
    private final MarkupListener listener;
    private boolean removed = false;

    public PolygonImpl(String id, PolygonOptions options, MarkupListener listener) {
        this.id = id;
        this.options = options;
        this.listener = listener;
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
    public boolean onClick() {
        return listener.onClick(this);
    }

    @Override
    public void onDragStart() {
        listener.onDragStart(this);
    }

    @Override
    public void onDragStop() {
        listener.onDragStop(this);
    }

    @Override
    public void onDragProgress() {
        listener.onDragProgress(this);
    }

    @Override
    public void setPoints(List<LatLng> points) throws RemoteException {
        options.getPoints().clear();
        options.getPoints().addAll(points);
        listener.update(this);
    }

    @Override
    public List<LatLng> getPoints() throws RemoteException {
        return options.getPoints();
    }

    @Override
    public void setHoles(List holes) throws RemoteException {
        options.getHoles().clear();
        options.getHoles().addAll(holes);
        listener.update(this);
    }

    @Override
    public List getHoles() throws RemoteException {
        return options.getHoles();
    }

    @Override
    public void setStrokeWidth(float width) throws RemoteException {
        options.strokeWidth(width);
        listener.update(this);
    }

    @Override
    public float getStrokeWidth() {
        return options.getStrokeWidth();
    }

    @Override
    public void setStrokeColor(int color) throws RemoteException {
        options.strokeColor(color);
        listener.update(this);
    }

    @Override
    public int getStrokeColor() {
        return options.getStrokeColor();
    }

    @Override
    public void setFillColor(int color) throws RemoteException {
        options.fillColor(color);
        listener.update(this);
    }

    @Override
    public int getFillColor() {
        return options.getFillColor();
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {
        options.zIndex(zIndex);
        listener.update(this);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    @Override
    public Drawable getDrawable(Map map) {
        if (!isVisible() || removed) return null;
        List<GeoPoint> points = new ArrayList<GeoPoint>();
        for (LatLng point : options.getPoints()) {
            points.add(GmsMapsTypeHelper.fromLatLng(point));
        }
        if (points.size() < 3 || (points.size() == 3 && points.get(2).equals(points.get(0)))) {
            // Need at least 3 distinguished points to draw a polygon
            return null;
        }
        // TODO: holes
        return new PolygonDrawable(points, Style.builder()
                .fillAlpha(1)
                .strokeColor(getStrokeColor())
                .strokeWidth(getStrokeWidth())
                .fillColor(getFillColor())
                .build());
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {
        options.visible(visible);
        listener.update(this);
    }

    @Override
    public boolean isVisible() {
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
    public boolean equalsRemote(IPolygonDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }

    // Not implemented
    @Override
    public void setClickable(boolean click) throws RemoteException {

    }

    @Override
    public boolean isClickable() throws RemoteException {
        return false;
    }

    @Override
    public void setStrokeJointType(int type) throws RemoteException {

    }

    @Override
    public int getStrokeJointType() throws RemoteException {
        return 0;
    }

    @Override
    public void setStrokePattern(List<PatternItem> items) throws RemoteException {

    }

    @Override
    public List<PatternItem> getStrokePattern() throws RemoteException {
        return null;
    }

    @Override
    public void setTag(IObjectWrapper obj) throws RemoteException {

    }

    @Override
    public IObjectWrapper getTag() throws RemoteException {
        return null;
    }
}
