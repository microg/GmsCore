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
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.internal.ICircleDelegate;

import org.microg.gms.maps.vtm.GmsMapsTypeHelper;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

import java.util.List;

public class CircleImpl extends ICircleDelegate.Stub implements DrawableMarkup {

    private static final String TAG = "GmsMapCircle";

    private final String id;
    private final CircleOptions options;
    private final MarkupListener listener;
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
        listener.update(this);
    }

    @Override
    public LatLng getCenter() throws RemoteException {
        return options.getCenter();
    }

    @Override
    public void setRadius(double radius) throws RemoteException {
        options.radius(radius);
        listener.update(this);
    }

    @Override
    public double getRadius() throws RemoteException {
        return options.getRadius();
    }

    @Override
    public void setStrokeWidth(float width) throws RemoteException {
        options.strokeWidth(width);
        listener.update(this);
    }

    @Override
    public float getStrokeWidth() throws RemoteException {
        return options.getStrokeWidth();
    }

    @Override
    public void setStrokeColor(int color) throws RemoteException {
        options.strokeColor(color);
        listener.update(this);
    }

    @Override
    public int getStrokeColor() throws RemoteException {
        return options.getStrokeColor();
    }

    @Override
    public void setFillColor(int color) throws RemoteException {
        options.fillColor(color);
        listener.update(this);
    }

    @Override
    public int getFillColor() throws RemoteException {
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
    public void setVisible(boolean visible) throws RemoteException {
        options.visible(visible);
        listener.update(this);
    }

    @Override
    public boolean isVisible() {
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
    public void setClickable(boolean clickable) throws RemoteException {
        Log.d(TAG, "unimplemented method: setClickable");
    }

    @Override
    public boolean isClickable() throws RemoteException {
        return false;
    }

    @Override
    public void setStrokePattern(List<PatternItem> object) throws RemoteException {
        Log.d(TAG, "unimplemented method: setStrokePattern");
    }

    @Override
    public List<PatternItem> getStrokePattern() throws RemoteException {
        Log.d(TAG, "unimplemented method: getStrokePattern");
        return null;
    }

    @Override
    public void setTag(IObjectWrapper object) throws RemoteException {
        Log.d(TAG, "unimplemented method: setTag");
    }

    @Override
    public IObjectWrapper getTag() throws RemoteException {
        Log.d(TAG, "unimplemented method: getTag");
        return ObjectWrapper.wrap(null);
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
    public Drawable getDrawable(Map map) {
        if (!isVisible() || removed) return null;
        return new CircleDrawable(
                GmsMapsTypeHelper.fromLatLng(options.getCenter()),
                options.getRadius() / 1000.0,
                Style.builder()
                        .strokeColor(options.getStrokeColor())
                        .fillAlpha(1)
                        .fillColor(options.getFillColor())
                        .strokeWidth(options.getStrokeWidth()).build());
    }
}
