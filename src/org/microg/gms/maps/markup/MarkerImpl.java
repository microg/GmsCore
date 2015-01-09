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

package org.microg.gms.maps.markup;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import org.microg.gms.maps.GmsMapsTypeHelper;
import org.microg.gms.maps.bitmap.BitmapDescriptorImpl;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

public class MarkerImpl extends IMarkerDelegate.Stub implements Markup {
    private static final String TAG = MarkerImpl.class.getName();

    private final String id;

    private MarkerOptions options;
    private BitmapDescriptorImpl icon;

    private MarkupListener listener;
    /*private Overlay overlay = new Overlay() {
        private Point point = new Point();

        @Override
        public boolean onTap(GeoPoint p, MapView mapView) {
            Point touchPoint = mapView.getProjection().toPixels(p, null);
            Bitmap bitmap = icon.getBitmap();
            if (bitmap == null)
                return false;
            //mapView.getProjection().toPixels(position.toGeoPoint(), point);
            float xTest = bitmap.getWidth() * anchorU + touchPoint.x - point.x;
            float yTest = bitmap.getHeight() * anchorV + touchPoint.y - point.y;
            if (0 < xTest && xTest < bitmap.getWidth() && 0 < yTest && yTest < bitmap.getHeight()) {
                Log.d(TAG, "touched " + title);
                IOnMarkerClickListener markerClickListener = map.getMarkerClickListener();
                boolean result = false;
                if (markerClickListener != null) {
                    try {
                        result = markerClickListener.onMarkerClick(MarkerImpl.this);
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                }
                if (!result) {
                    mapView.getController().animateTo(position.toGeoPoint());
                    map.showInfoWindow(MarkerImpl.this);
                }
                return true;
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow && flat)
                return; // shadows are broken right now, we skip them
            Bitmap bitmap = icon.getBitmap();
            if (bitmap != null) {
                //mapView.getProjection().toPixels(position.toGeoPoint(), point);
                float x = point.x - bitmap.getWidth() * anchorU;
                float y = point.y - bitmap.getHeight() * anchorV;
                Paint paint = new Paint();
                paint.setAlpha((int) (alpha * 255));
                if (shadow) {
                    paint.setColorFilter(
                            new PorterDuffColorFilter(Color.argb((int) (128 * alpha), 0, 0, 0),
                                    PorterDuff.Mode.SRC_IN));
                }
                Matrix matrix = new Matrix();
                matrix.setRotate(rotation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                if (shadow) {
                    matrix.postSkew(-0.9F, 0);
                    matrix.postScale(1, 0.5F);
                }
                matrix.postTranslate(x, y);
                canvas.drawBitmap(bitmap, matrix, paint);
            } else {
                icon.loadBitmapAsync(map.getContext(), new Runnable() {
                    @Override
                    public void run() {
                        map.redraw();
                    }
                });
            }
        }
    };*/

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
    public MarkerItem getMarkerItem(Context context) {
        MarkerItem item = new MarkerItem(getId(), getTitle(), getSnippet(),
                GmsMapsTypeHelper.fromLatLng(getPosition()));
        if (icon != null) {
            if (icon.getBitmap() != null) {
                item.setMarker(
                        new MarkerSymbol(new AndroidBitmap(icon.getBitmap()), options.getAnchorU(),
                                options.getAnchorV(), isFlat()));
            } else {
                icon.loadBitmapAsync(context, new Runnable() {
                    @Override
                    public void run() {
                        listener.update(MarkerImpl.this);
                    }
                });
            }
        }
        return item;
    }
}
