/*
 * Copyright 2013-2015 microG Project Team
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

package org.microg.gms.maps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import com.google.android.gms.R;

import org.microg.gms.maps.data.SharedTileCache;
import org.microg.gms.maps.camera.CameraUpdate;
import org.microg.gms.maps.markup.ClearableVectorLayer;
import org.microg.gms.maps.markup.DrawableMarkup;
import org.microg.gms.maps.markup.MarkerItemMarkup;
import org.microg.gms.maps.markup.Markup;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.MapPosition;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class BackendMap implements ItemizedLayer.OnItemGestureListener<MarkerItem> {
    private final static String TAG = "GmsMapBackend";

    private final Context context;
    private final MapView mapView;
    private final LabelLayer labels;
    private final BuildingLayer buildings;
    private final VectorTileLayer baseLayer;
    private final OSciMap4TileSource tileSource;
    private final ITileCache cache;
    private final ItemizedLayer<MarkerItem> items;
    private java.util.Map<String, Markup> markupMap = new HashMap<String, Markup>();
    private List<DrawableMarkup> drawableMarkups = new ArrayList<DrawableMarkup>();
    private ClearableVectorLayer drawables;

    public BackendMap(Context context) {
        this.context = context;
        mapView = new MapView(new ContextContainer(context));
        cache = new SharedTileCache(context);
        cache.setCacheSize(512 * (1 << 10));
        tileSource = new OSciMap4TileSource();
        tileSource.setCache(cache);
        baseLayer = mapView.map().setBaseMap(tileSource);
        Layers layers = mapView.map().layers();
        layers.add(labels = new LabelLayer(mapView.map(), baseLayer));
        layers.add(drawables = new ClearableVectorLayer(mapView.map()));
        layers.add(buildings = new BuildingLayer(mapView.map(), baseLayer));
        layers.add(items = new ItemizedLayer<MarkerItem>(mapView.map(), new MarkerSymbol(new AndroidBitmap(BitmapFactory
                .decodeResource(ResourcesContainer.get(), R.drawable.nop)), 0.5F, 1)));
        items.setOnItemGestureListener(this);
        mapView.map().setTheme(VtmThemes.DEFAULT);
    }

    public void setInputListener(Map.InputListener listener) {
        mapView.map().input.bind(listener);
    }

    public Viewport getViewport() {
        return mapView.map().viewport();
    }

    public void destroy() {
        mapView.map().destroy();
    }

    public void onResume() {
        try {
            Method onResume = MapView.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            onResume.invoke(mapView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        try {
            Method onPause = MapView.class.getDeclaredMethod("onPause");
            onPause.setAccessible(true);
            onPause.invoke(mapView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MapPosition getMapPosition() {
        return mapView.map().getMapPosition();
    }

    public View getView() {
        return mapView;
    }

    public boolean hasBuilding() {
        return mapView.map().layers().contains(buildings);
    }

    public void setBuildings(boolean buildingsEnabled) {
        if (!hasBuilding() && buildingsEnabled) {
            mapView.map().layers().add(buildings);
        } else if (hasBuilding() && !buildingsEnabled) {
            mapView.map().layers().remove(buildings);
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
        drawables.clear();
        for (DrawableMarkup markup : drawableMarkups) {
            drawables.add(markup.getDrawable(mapView.map()));
        }
    }

    public synchronized <T extends MarkerItemMarkup> T add(T markup) {
        if (markup == null) return null;
        markupMap.put(markup.getId(), markup);
        items.addItem(markup.getMarkerItem(context));
        redraw();
        return markup;
    }

    public synchronized void clear() {
        markupMap.clear();
        items.removeAllItems();
        drawableMarkups.clear();
        drawables.clear();
        redraw();
    }

    public synchronized void remove(Markup markup) {
        if (markup instanceof MarkerItemMarkup) {
            markupMap.remove(markup.getId());
            items.removeItem(items.getByUid(markup.getId()));
            redraw();
        } else if (markup instanceof DrawableMarkup) {
            drawableMarkups.remove(markup);
            updateDrawableLayer();
            redraw();
        }
    }

    public synchronized void update(Markup markup) {
        if (markup == null) return;
        if (markup instanceof MarkerItemMarkup) {
            items.removeItem(items.getByUid(markup.getId()));
            items.addItem(((MarkerItemMarkup) markup).getMarkerItem(context));
        } else if (markup instanceof DrawableMarkup) {
            updateDrawableLayer();
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
        // TODO: start drag+drop
        Log.d(TAG, "onItemLongPress: " + markupMap.get(item.getUid()));
        return false;
    }
}
