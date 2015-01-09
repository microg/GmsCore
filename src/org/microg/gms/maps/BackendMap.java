package org.microg.gms.maps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import com.google.android.gms.R;
import org.microg.gms.maps.bitmap.DefaultBitmapDescriptor;
import org.microg.gms.maps.camera.CameraUpdate;
import org.microg.gms.maps.markup.Markup;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
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
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class BackendMap {
    private final Context context;
    private final MapView mapView;
    private final BuildingLayer buildings;
    private final VectorTileLayer baseLayer;
    private final OSciMap4TileSource tileSource;
    private final TileCache cache;
    private final ItemizedLayer<MarkerItem> items;

    public BackendMap(Context context) {
        this.context = context;
        mapView = new MapView(new ContextContainer(context));
        // TODO: Use a shared tile cache (provider?)
        cache = new TileCache(context, null, "tile.db");
        cache.setCacheSize(512 * (1 << 10));
        tileSource = new OSciMap4TileSource();
        tileSource.setCache(cache);
        baseLayer = mapView.map().setBaseMap(tileSource);
        Layers layers = mapView.map().layers();
        layers.add(buildings = new BuildingLayer(mapView.map(), baseLayer));
        layers.add(new LabelLayer(mapView.map(), baseLayer));
        layers.add(items = new ItemizedLayer<>(mapView.map(), new MarkerSymbol(new AndroidBitmap(BitmapFactory
                .decodeResource(ResourcesContainer.get(), R.drawable.maps_default_marker)), 0.5F, 1)));
        mapView.map().setTheme(VtmThemes.DEFAULT);
    }

    public void setInputListener(Map.InputListener listener) {
        mapView.map().input.bind(listener);
    }

    public Viewport getViewport() {
        return mapView.map().viewport();
    }

    public void destroy() {
        //mapView.map().destroy();
    }

    public void onResume() {
        /*try {
            Method onResume = MapView.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            onResume.invoke(mapView);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public void onPause() {
        /*try {
            Method onPause = MapView.class.getDeclaredMethod("onPause");
            onPause.setAccessible(true);
            onPause.invoke(mapView);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
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
        mapView.map().render();
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

    public <T extends Markup> T add(T markup) {
        items.addItem(markup.getMarkerItem(context));
        redraw();
        return markup;
    }

    public void clear() {
        items.removeAllItems();
        redraw();
    }

    public void remove(Markup markup) {
        items.removeItem(items.getByUid(markup.getId()));
        redraw();
    }

    public void update(Markup markup) {
        // TODO: keep order
        items.removeItem(items.getByUid(markup.getId()));
        items.addItem(markup.getMarkerItem(context));
        redraw();
    }
}
