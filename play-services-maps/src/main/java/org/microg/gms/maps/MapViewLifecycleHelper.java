/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.gms.dynamic.DeferredLifecycleHelper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamic.OnDelegateCreatedListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.internal.IMapViewDelegate;

import java.util.ArrayList;
import java.util.List;

public class MapViewLifecycleHelper extends DeferredLifecycleHelper<MapViewDelegate> {
    private final ViewGroup container;
    private final Context context;
    private final GoogleMapOptions options;
    private final List<OnMapReadyCallback> pendingMapReadyCallbacks = new ArrayList<>();

    public MapViewLifecycleHelper(ViewGroup container, Context context, GoogleMapOptions options) {
        this.container = container;
        this.context = context;
        this.options = options;
    }

    public final void getMapAsync(OnMapReadyCallback callback) {
        if (getDelegate() != null) {
            getDelegate().getMapAsync(callback);
        } else {
            this.pendingMapReadyCallbacks.add(callback);
        }
    }

    @Override
    protected void createDelegate(@NonNull OnDelegateCreatedListener<MapViewDelegate> listener) {
        if (getDelegate() != null) return;
        try {
            MapsInitializer.initialize(context);
            IMapViewDelegate delegate = MapsContextLoader.getCreator(context, null).newMapViewDelegate(ObjectWrapper.wrap(context), options);
            listener.onDelegateCreated(new MapViewDelegate(container, delegate));
            for (OnMapReadyCallback callback : pendingMapReadyCallbacks) {
                getDelegate().getMapAsync(callback);
            }
            pendingMapReadyCallbacks.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
