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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;

import org.microg.gms.maps.vtm.GmsMapsTypeHelper;
import org.microg.gms.maps.vtm.bitmap.BitmapDescriptorImpl;
import org.microg.gms.maps.vtm.bitmap.DefaultBitmapDescriptor;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

public class MarkerImpl extends IMarkerDelegate.Stub implements MarkerItemMarkup {
    private static final String TAG = "GmsMapMarkerImpl";

    private final String id;
    private final MarkerOptions options;
    private final MarkupListener listener;
    private BitmapDescriptorImpl icon;
    private AndroidBitmap oldBitmap;
    private boolean removed = false;
    private IObjectWrapper tag = null;

    public MarkerImpl(String id, MarkerOptions options, MarkupListener listener) {
        this.id = id;
        this.listener = listener;
        this.options = options == null ? new MarkerOptions() : options;
        if (options.getPosition() == null) {
            options.position(new LatLng(0, 0));
        }
        icon = options.getIcon() == null ? null : new BitmapDescriptorImpl(options.getIcon());
        Log.d(TAG, "New marker " + id + " with title " + options.getTitle() + " @ " +
                options.getPosition());
    }

    @Override
    public void remove() {
        listener.remove(this);
        removed = true;
        icon = null;
        oldBitmap = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setPosition(LatLng pos) {
        options.position(pos);
        listener.update(this);
    }

    @Override
    public LatLng getPosition() {
        return options.getPosition();
    }

    @Override
    public void setTitle(String title) {
        options.title(title);
        listener.update(this);
    }

    @Override
    public String getTitle() {
        return options.getTitle();
    }

    @Override
    public void setSnippet(String snippet) {
        options.snippet(snippet);
    }

    @Override
    public String getSnippet() {
        return options.getSnippet();
    }

    @Override
    public void setDraggable(boolean drag) {
        options.draggable(drag);
    }

    @Override
    public boolean isDraggable() {
        return options.isDraggable();
    }

    @Override
    public void showInfoWindow() {

    }

    @Override
    public void hideInfoWindow() {

    }

    @Override
    public boolean isInfoWindowShown() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
        options.visible(visible);
    }

    @Override
    public boolean isVisible() {
        return options.isVisible();
    }

    @Override
    public boolean equalsRemote(IMarkerDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() {
        return hashCode();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void setIcon(IObjectWrapper obj) {
        if (obj == null) {
            icon = new BitmapDescriptorImpl();
        } else {
            icon = new BitmapDescriptorImpl(obj);
        }
        listener.update(this);
    }

    @Override
    public void setAnchor(float x, float y) {
        options.anchor(x, y);
        listener.update(this);
    }

    @Override
    public void setFlat(boolean flat) {
        options.flat(flat);
        listener.update(this);
    }

    @Override
    public boolean isFlat() {
        return options.isFlat();
    }

    @Override
    public void setRotation(float rotation) {
        options.rotation(rotation);
        listener.update(this);
    }

    @Override
    public float getRotation() {
        return options.getRotation();
    }

    @Override
    public void setInfoWindowAnchor(float x, float y) {
        options.infoWindowAnchor(x, y);
    }

    @Override
    public void setAlpha(float alpha) {
        options.alpha(alpha);
        listener.update(this);
    }

    @Override
    public float getAlpha() {
        return options.getAlpha();
    }

    public int getHeight() {
        Bitmap bitmap = icon.getBitmap();
        if (bitmap == null)
            return -1;
        return bitmap.getHeight();
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
    public MarkerItem getMarkerItem(Context context) {
        if (removed) return null;
        MarkerItem item = new MarkerItem(getId(), getTitle(), getSnippet(),
                GmsMapsTypeHelper.fromLatLng(getPosition()));
        BitmapDescriptorImpl icon = this.icon;
        if (icon == null)
            icon = DefaultBitmapDescriptor.DEFAULT_DESCRIPTOR_IMPL;
        if (icon.getBitmap() != null) {
            oldBitmap = new AndroidBitmap(icon.getBitmap());
            prepareMarkerIcon(item);
        } else {
            if (!icon.loadBitmapAsync(context, new Runnable() {
                @Override
                public void run() {
                    listener.update(MarkerImpl.this);
                }
            })) {
                // Was loaded since last check...
                oldBitmap = new AndroidBitmap(icon.getBitmap());
                prepareMarkerIcon(item);
            }
            // Keep old icon while loading new
            if (oldBitmap != null) {
                prepareMarkerIcon(item);
            }
        }
        return item;
    }

    private void prepareMarkerIcon(MarkerItem item) {
        item.setMarker(new MarkerSymbol(oldBitmap, options.getAnchorU(), options.getAnchorV(), !options.isFlat()));
    }

    @Override
    public void setZIndex(float zIndex) {
        options.zIndex(zIndex);
    }

    @Override
    public float getZIndex() {
        return options.getZIndex();
    }

    @Override
    public void setTag(IObjectWrapper obj) {
        this.tag = obj;
    }

    @Override
    public IObjectWrapper getTag() {
        return this.tag == null ? ObjectWrapper.wrap(null) : this.tag;
    }
}
