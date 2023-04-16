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
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;

import org.microg.gms.maps.vtm.GmsMapsTypeHelper;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

public class PolylineImpl extends IPolylineDelegate.Stub implements DrawableMarkup {
    private static final String TAG = "GmsMapsPolylineImpl";

    private final String id;
    private final PolylineOptions options;
    private final MarkupListener listener;
    private boolean removed = false;

    public PolylineImpl(String id, PolylineOptions options, MarkupListener listener) {
        this.id = id;
        this.options = options == null ? new PolylineOptions() : options;
        this.listener = listener;
    }

    @Override
    public void remove() {
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
    public void setPoints(List<LatLng> points) {
        options.getPoints().clear();
        options.getPoints().addAll(points);
        listener.update(this);
    }

    @Override
    public List<LatLng> getPoints() {
        return options.getPoints();
    }

    @Override
    public void setWidth(float width) {
        options.width(width);
        listener.update(this);
    }

    @Override
    public float getWidth() {
        return options.getWidth();
    }

    @Override
    public void setColor(int color) {
        this.options.color(color);
        listener.update(this);
    }

    @Override
    public int getColor() {
        return options.getColor();
    }

    @Override
    public void setZIndex(float zIndex) {
        options.zIndex(zIndex);
        listener.update(this);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    @Override
    public void setVisible(boolean visible) {
        options.visible(visible);
        listener.update(this);
    }

    @Override
    public boolean isVisible() {
        return options.isVisible();
    }

    @Override
    public void setGeodesic(boolean geod) {
        options.geodesic(geod);
        listener.update(this);
    }

    @Override
    public boolean isGeodesic() {
        return options.isGeodesic();
    }

    @Override
    public boolean equalsRemote(IPolylineDelegate other) throws RemoteException {
        Log.d(TAG, "equalsRemote");
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() {
        Log.d(TAG, "hashcodeRemote");
        return id.hashCode();
    }

    // Not implemented
    @Override
    public void setClickable(boolean clickable) throws RemoteException {

    }

    @Override
    public boolean isClickable() throws RemoteException {
        return false;
    }

    @Override
    public void setJointType(int jointType) throws RemoteException {

    }

    @Override
    public int getJointType() throws RemoteException {
        return 0;
    }

    @Override
    public void setPattern(List<PatternItem> pattern) throws RemoteException {

    }

    @Override
    public List<PatternItem> getPattern() throws RemoteException {
        return null;
    }

    @Override
    public void setTag(IObjectWrapper tag) throws RemoteException {

    }

    @Override
    public IObjectWrapper getTag() throws RemoteException {
        return null;
    }

    @Override
    public Drawable getDrawable(Map map) {
        if (!isVisible() || removed) return null;
        if (options.getPoints().size() < 2) {
            // You hardly draw a line with less than two points
            return null;
        }
        List<GeoPoint> points = new ArrayList<GeoPoint>();
        for (LatLng point : options.getPoints()) {
            points.add(GmsMapsTypeHelper.fromLatLng(point));
        }
        return new LineDrawable(points, Style.builder().strokeColor(getColor()).strokeWidth(getWidth()).build());
    }
}
