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

import android.graphics.*;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import com.google.android.gms.maps.internal.IOnMarkerClickListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import org.microg.gms.maps.bitmap.BitmapDescriptor;
import org.microg.gms.maps.bitmap.DefaultBitmapDescriptor;
import org.microg.gms.maps.GoogleMapImpl;

public class MarkerImpl extends IMarkerDelegate.Stub {
    private static final String TAG = MarkerImpl.class.getName();

    private float alpha;
    private boolean flat;
    private boolean draggable;
    private String id = Integer.toHexString(hashCode());
    private LatLng position;
    private float anchorU;
    private float anchorV;
    private float rotation;
    private String snippet;
    private String title;
    private boolean visible;

    private BitmapDescriptor icon;

    private GoogleMapImpl map;
    private Overlay overlay = new Overlay() {
        private Point point = new Point();

        @Override
        public boolean onTap(GeoPoint p, MapView mapView) {
            Point touchPoint = mapView.getProjection().toPixels(p, null);
            Bitmap bitmap = icon.getBitmap();
            if (bitmap == null) return false;
            mapView.getProjection().toPixels(position.toGeoPoint(), point);
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
                    // TODO info window
                }
                return true;
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!shadow) {
                Bitmap bitmap = icon.getBitmap();
                if (bitmap != null) {
                    mapView.getProjection().toPixels(position.toGeoPoint(), point);
                    float x = point.x - bitmap.getWidth() * anchorU;
                    float y = point.y - bitmap.getHeight() * anchorV;
                    Paint paint = new Paint();
                    paint.setAlpha((int) (alpha * 255));
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
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
            } else {
                // TODO: shadow based on flat
            }
        }
    };

    public MarkerImpl(MarkerOptions options, GoogleMapImpl map) {
        this.map = map;
        this.alpha = options.getAlpha();
        this.draggable = options.isDraggable();
        this.position = options.getPosition();
        if (position == null) position = new LatLng(0, 0);
        this.rotation = options.getRotation();
        this.anchorU = options.getAnchorU();
        this.anchorV = options.getAnchorV();
        this.snippet = options.getSnippet();
        this.title = options.getTitle();
        this.visible = options.isVisible();
        this.icon = options.getIcon();
        if (icon == null)
            icon = new BitmapDescriptor(new ObjectWrapper<DefaultBitmapDescriptor>(new DefaultBitmapDescriptor(0)));
        Log.d(TAG, "New: " + title + " @ " + position + ", " + icon);
    }

    @Override
    public void remove() throws RemoteException {
        map.remove(this);
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public void setPosition(LatLng pos) throws RemoteException {
        this.position = pos;
    }

    @Override
    public LatLng getPosition() throws RemoteException {
        return position;
    }

    @Override
    public void setTitle(String title) throws RemoteException {
        this.title = title;
    }

    @Override
    public String getTitle() throws RemoteException {
        return title;
    }

    @Override
    public void setSnippet(String snippet) throws RemoteException {
        this.snippet = snippet;
    }

    @Override
    public String getSnippet() throws RemoteException {
        return snippet;
    }

    @Override
    public void setDraggable(boolean drag) throws RemoteException {
        this.draggable = drag;
    }

    @Override
    public boolean isDraggable() throws RemoteException {
        return draggable;
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
        this.visible = visible;
    }

    @Override
    public boolean isVisible() throws RemoteException {
        return visible;
    }

    @Override
    public boolean equalsRemote(IMarkerDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return hashCode();
    }

    @Override
    public void setIcon(IObjectWrapper obj) throws RemoteException {
        icon = new BitmapDescriptor(obj);
        if (icon == null)
            icon = new BitmapDescriptor(new ObjectWrapper<DefaultBitmapDescriptor>(new DefaultBitmapDescriptor(0)));
        map.redraw();
    }

    @Override
    public void setAnchor(float x, float y) throws RemoteException {
        anchorU = x;
        anchorV = y;
        map.redraw();
    }

    @Override
    public void setFlat(boolean flat) throws RemoteException {
        map.redraw();
    }

    @Override
    public boolean isFlat() throws RemoteException {
        return false;
    }

    @Override
    public void setRotation(float rotation) throws RemoteException {
        this.rotation = rotation;
        map.redraw();
    }

    @Override
    public float getRotation() throws RemoteException {
        return rotation;
    }

    @Override
    public void setInfoWindowAnchor(float x, float y) throws RemoteException {

    }

    @Override
    public void setAlpha(float alpha) throws RemoteException {
        this.alpha = alpha;
        map.redraw();
    }

    @Override
    public float getAlpha() throws RemoteException {
        return alpha;
    }

    public Overlay getOverlay() {
        return overlay;
    }
}
