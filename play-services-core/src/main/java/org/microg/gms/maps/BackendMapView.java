/*
 * Copyright 2013-2016 microG Project Team
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
import android.util.AttributeSet;

import com.google.android.gms.R;

import org.microg.gms.maps.data.SharedTileCache;
import org.microg.gms.maps.markup.ClearableVectorLayer;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class BackendMapView extends MapView {
    private LabelLayer labels;
    private BuildingLayer buildings;
    private ItemizedLayer<MarkerItem> items;
    private ClearableVectorLayer drawables;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public BackendMapView(Context context) {
        super(context);
        initialize();
    }

    public BackendMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize();
    }

    ItemizedLayer<MarkerItem> items() {
        return items;
    }

    BuildingLayer buildings() {
        return buildings;
    }

    ClearableVectorLayer drawables() {
        return drawables;
    }

    private void initialize() {
        ITileCache cache = new SharedTileCache(getContext());
        cache.setCacheSize(512 * (1 << 10));
        OSciMap4TileSource tileSource = new OSciMap4TileSource();
        tileSource.setCache(cache);
        VectorTileLayer baseLayer = map().setBaseMap(tileSource);
        Layers layers = map().layers();
        layers.add(labels = new LabelLayer(map(), baseLayer));
        layers.add(drawables = new ClearableVectorLayer(map()));
        layers.add(buildings = new BuildingLayer(map(), baseLayer));
        layers.add(items = new ItemizedLayer<MarkerItem>(map(), new MarkerSymbol(
                new AndroidBitmap(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.nop)), 0.5F, 1)));
        map().setTheme(VtmThemes.DEFAULT);
    }
}
