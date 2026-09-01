/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.vtm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.internal.ISnapshotReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;

import org.microg.gms.maps.vtm.camera.CameraUpdate;
import org.microg.gms.maps.vtm.markup.DrawableMarkup;
import org.microg.gms.maps.vtm.markup.MarkerItemMarkup;
import org.microg.gms.maps.vtm.markup.Markup;
import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.event.Event;
import org.oscim.event.MotionEvent;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.map.Viewport;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

public class BackendMap implements ItemizedLayer.OnItemGestureListener<MarkerItem>, org.oscim.map.Map.InputListener, org.oscim.map.Map.UpdateListener {
    private final static String TAG = "GmsMapBackend";

    private final Context context;
    private final BackendMapView mapView;
    private final ContainerLayout container;
    private final CameraUpdateListener cameraUpdateListener;
    private final Map<String, Markup> markupMap = new HashMap<String, Markup>();
    private final List<DrawableMarkup> drawableMarkups = new ArrayList<DrawableMarkup>();
    private MarkerItemMarkup currentlyDraggedItem;
    private float dragLastX = -1;
    private float dragLastY = -1;

    public BackendMap(Context context, final CameraUpdateListener cameraUpdateListener) {
        this.context = context;
        this.cameraUpdateListener = cameraUpdateListener;
        mapView = new BackendMapView(context);
        mapView.items().setOnItemGestureListener(this);
        mapView.map().input.bind(this);
        mapView.map().events.bind(this);
        container = new ContainerLayout(context);
        container.addView(mapView);
    }

    public Viewport getViewport() {
        return mapView.map().viewport();
    }

    public void destroy() {
        mapView.map().destroy();
    }

    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public MapPosition getMapPosition() {
        return mapView.map().getMapPosition();
    }

    public View getView() {
        return container;
    }

    public boolean hasBuilding() {
        return mapView.map().layers().contains(mapView.buildings());
    }

    public void setBuildings(boolean buildingsEnabled) {
        if (!hasBuilding() && buildingsEnabled) {
            mapView.map().layers().add(mapView.buildings());
        } else if (hasBuilding() && !buildingsEnabled) {
            mapView.map().layers().remove(mapView.buildings());
        }
        redraw();
    }

    public void redraw() {
        mapView.map().updateMap(true);
    }

    public void applyCameraUpdate(CameraUpdate cameraUpdate) {
        cameraUpdate.apply(mapView.map());
    }

    public void applyCameraUpdateAnimated(CameraUpdate cameraUpdate, int durationMs) {
        cameraUpdate.applyAnimated(mapView.map(), durationMs);
    }

    public void stopAnimation() {
        mapView.map().animator().cancel();
    }

    public synchronized <T extends DrawableMarkup> T add(T markup) {
        if (markup == null) return null;
        drawableMarkups.add(markup);
        Collections.sort(drawableMarkups, new Comparator<DrawableMarkup>() {
            @Override
            public int compare(DrawableMarkup lhs, DrawableMarkup rhs) {
                return Float.compare(lhs.getZIndex(), rhs.getZIndex());
            }
        });
        updateDrawableLayer();
        redraw();
        return markup;
    }

    private synchronized void updateDrawableLayer() {
        mapView.drawables().clear();
        for (DrawableMarkup markup : drawableMarkups) {
            Drawable drawable = markup.getDrawable(mapView.map());
            if (drawable != null) {
                mapView.drawables().add(drawable);
            }
        }
    }

    public synchronized <T extends MarkerItemMarkup> T add(T markup) {
        if (markup == null) return null;
        markupMap.put(markup.getId(), markup);
        MarkerItem item = markup.getMarkerItem(context);
        mapView.items().addItem(item);
        redraw();
        return markup;
    }

    public synchronized void clear() {
        markupMap.clear();
        mapView.items().removeAllItems();
        drawableMarkups.clear();
        mapView.drawables().clear();
        redraw();
    }

    public synchronized void remove(Markup markup) {
        if (markup instanceof MarkerItemMarkup) {
            markupMap.remove(markup.getId());
            MarkerItem toRemove = getByUid(markup.getId());
            if (toRemove != null) mapView.items().removeItem(toRemove);
        } else if (markup instanceof DrawableMarkup) {
            drawableMarkups.remove(markup);
            updateDrawableLayer();
            mapView.drawables().update();
        }
        redraw();
    }

    private MarkerItem getByUid(String uid) {
        for (MarkerItem markerItem : mapView.items().getItemList()) {
            if (markerItem.getUid().equals(uid)) {
                return markerItem;
            }
        }
        return null;
    }

    public synchronized void update(Markup markup) {
        if (markup == null) return;
        if (markup instanceof MarkerItemMarkup) {
            MarkerItem item = getByUid(markup.getId());
            if (item != null) {
                mapView.items().removeItem(item);
            }
            item = ((MarkerItemMarkup) markup).getMarkerItem(context);
            if (item != null) {
                mapView.items().addItem(item);
            }
        } else if (markup instanceof DrawableMarkup) {
            updateDrawableLayer();
            mapView.drawables().update();
        }
        redraw();
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        Markup markup = markupMap.get(item.getUid());
        if (markup != null) {
            if (markup.onClick()) return true;
        }
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        Markup markup = markupMap.get(item.getUid());
        if (((MarkerItemMarkup) markup).isDraggable()) {
            currentlyDraggedItem = (MarkerItemMarkup) markup;
            currentlyDraggedItem.onDragStart();
            return false;
        } else {
            Log.d(TAG, "onItemLongPress: " + markup);
            return false;
        }
    }

    @Override
    public void onInputEvent(Event event, MotionEvent motionEvent) {
        if ((motionEvent.getAction() == MotionEvent.ACTION_CANCEL || motionEvent.getAction() == MotionEvent.ACTION_UP) && currentlyDraggedItem != null) {
            currentlyDraggedItem.onDragStop();
            currentlyDraggedItem = null;
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            dragLastX = motionEvent.getX();
            dragLastY = motionEvent.getY();
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && currentlyDraggedItem != null) {
            Point out = new Point();
            mapView.map().viewport().toScreenPoint(GmsMapsTypeHelper.fromLatLng(currentlyDraggedItem.getPosition()), out);
            out.x += mapView.getWidth() / 2;
            out.y += mapView.getHeight() / 2;
            float mx = motionEvent.getX() - dragLastX;
            float my = motionEvent.getY() - dragLastY;
            currentlyDraggedItem.setPosition(GmsMapsTypeHelper.toLatLng(mapView.map().viewport().fromScreenPoint((float) out.getX() + mx, (float) out.getY() + my)));
            currentlyDraggedItem.onDragProgress();
            dragLastX += mx;
            dragLastY += my;
        }
    }

    public void snapshot(final Bitmap bitmap, final ISnapshotReadyCallback callback) {
        mapView.queueEvent(new Runnable() {
            @Override
            public void run() {
                Bitmap surface = createBitmapFromGLSurface(0, 0, mapView.getWidth(), mapView.getHeight(), GLAdapter.gl);
                final Bitmap result;
                if (bitmap != null) {
                    Canvas c = new Canvas(bitmap);
                    c.drawBitmap(surface, 0, 0, new Paint());
                    result = bitmap;
                } else {
                    result = surface;
                }
                mapView.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "snapshot result: " + result);
                        try {
                            callback.onBitmapReady(result);
                        } catch (RemoteException e) {
                            Log.w(TAG, e);
                        }
                    }
                });
            }
        });
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL gl) {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.readPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "createBitmapFromGLSurface: " + e.getMessage(), e);
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    public void setZoomGesturesEnabled(boolean enabled) {
        mapView.map().getEventLayer().enableZoom(enabled);
    }

    public void setScrollGesturesEnabled(boolean enabled) {
        mapView.map().getEventLayer().enableMove(enabled);
    }

    public void setRotateGesturesEnabled(boolean enabled) {
        mapView.map().getEventLayer().enableRotation(enabled);
    }

    public void setTiltGesturesEnabled(boolean enabled) {
        mapView.map().getEventLayer().enableTilt(enabled);
    }

    @Override
    public void onMapEvent(Event event, MapPosition mapPosition) {
        if (event == org.oscim.map.Map.ANIM_END || event == org.oscim.map.Map.POSITION_EVENT || event == org.oscim.map.Map.MOVE_EVENT)
            cameraUpdateListener.onCameraUpdate(GmsMapsTypeHelper.toCameraPosition(mapPosition));
    }

    public interface CameraUpdateListener {
        void onCameraUpdate(CameraPosition cameraPosition);
    }
}
