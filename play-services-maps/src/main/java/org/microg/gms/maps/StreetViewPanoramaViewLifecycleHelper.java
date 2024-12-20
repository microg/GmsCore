/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.gms.dynamic.DeferredLifecycleHelper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamic.OnDelegateCreatedListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.internal.IStreetViewPanoramaViewDelegate;

import java.util.ArrayList;
import java.util.List;

public class StreetViewPanoramaViewLifecycleHelper extends DeferredLifecycleHelper<StreetViewPanoramaViewDelegate> {
    private final ViewGroup container;
    private final Context context;
    private final StreetViewPanoramaOptions options;
    private final List<OnStreetViewPanoramaReadyCallback> pendingStreetViewReadyCallbacks = new ArrayList<>();

    public StreetViewPanoramaViewLifecycleHelper(ViewGroup container, Context context, StreetViewPanoramaOptions options) {
        this.container = container;
        this.context = context;
        this.options = options;
    }

    public final void getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback callback) {
        if (getDelegate() != null) {
            getDelegate().getStreetViewPanoramaAsync(callback);
        } else {
            this.pendingStreetViewReadyCallbacks.add(callback);
        }
    }

    @Override
    protected void createDelegate(@NonNull OnDelegateCreatedListener<StreetViewPanoramaViewDelegate> listener) {
        if (getDelegate() != null) return;
        try {
            MapsInitializer.initialize(context);
            IStreetViewPanoramaViewDelegate delegate = MapsContextLoader.getCreator(context, null).newStreetViewPanoramaViewDelegate(ObjectWrapper.wrap(context), options);
            listener.onDelegateCreated(new StreetViewPanoramaViewDelegate(container, delegate));
            for (OnStreetViewPanoramaReadyCallback callback : pendingStreetViewReadyCallbacks) {
                getDelegate().getStreetViewPanoramaAsync(callback);
            }
            pendingStreetViewReadyCallbacks.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
