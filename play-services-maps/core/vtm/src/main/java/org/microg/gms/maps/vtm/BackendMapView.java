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
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;

import org.microg.gms.maps.vtm.data.SharedTileCache;
import org.microg.gms.maps.vtm.markup.ClearableVectorLayer;
import org.microg.gms.maps.vtm.R;
import org.oscim.android.AndroidAssets;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.theme.MicrogThemes;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BackendMapView extends MapView {
    private static final String TAG = "GmsMapView";

    private static boolean nativeLibLoaded = false;
    private LabelLayer labels;
    private BuildingLayer buildings;
    private ItemizedLayer<MarkerItem> items;
    private ClearableVectorLayer drawables;

    static synchronized Context loadNativeLib(Context context) {
        try {
            if (nativeLibLoaded) return context;
            ApplicationInfo otherAppInfo = context.getPackageManager().getApplicationInfo(context.getApplicationContext().getPackageName(), 0);

            String primaryCpuAbi = (String) ApplicationInfo.class.getField("primaryCpuAbi").get(otherAppInfo);
            if (primaryCpuAbi != null) {
                String path = "lib/" + primaryCpuAbi + "/libvtm-jni.so";
                File cacheFile = new File(context.getApplicationContext().getCacheDir().getAbsolutePath() + "/.gmscore/" + path);
                cacheFile.getParentFile().mkdirs();
                File apkFile = new File(context.getPackageCodePath());
                if (!cacheFile.exists() || cacheFile.lastModified() < apkFile.lastModified()) {
                    ZipFile zipFile = new ZipFile(apkFile);
                    ZipEntry entry = zipFile.getEntry(path);
                    if (entry != null) {
                        copyInputStream(zipFile.getInputStream(entry), new FileOutputStream(cacheFile));
                    } else {
                        Log.d(TAG, "Can't load native library: " + path + " does not exist in " + apkFile);
                    }
                }
                Log.d(TAG, "Loading vtm-jni from " + cacheFile.getPath());
                System.load(cacheFile.getAbsolutePath());
                nativeLibLoaded = true;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        if (!nativeLibLoaded) {
            Log.d(TAG, "Loading native vtm-jni");
            System.loadLibrary("vtm-jni");
            nativeLibLoaded = true;
        }
        return context;
    }

    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public BackendMapView(Context context) {
        super(loadNativeLib(context));
        if (context instanceof ContextWrapper) {
            AndroidAssets.init(ApplicationContextWrapper.matchingApplicationContext(((ContextWrapper) context).getBaseContext()));
        }
        initialize();
    }

    public BackendMapView(Context context, AttributeSet attributeSet) {
        super(loadNativeLib(context), attributeSet);
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
        layers.add(drawables = new ClearableVectorLayer(map()));
        layers.add(labels = new LabelLayer(map(), baseLayer));
        layers.add(buildings = new BuildingLayer(map(), baseLayer));
        layers.add(items = new ItemizedLayer<MarkerItem>(map(), new MarkerSymbol(
                new AndroidBitmap(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.nop)), 0.5F, 1)));
        map().setTheme(MicrogThemes.DEFAULT);
    }
}
