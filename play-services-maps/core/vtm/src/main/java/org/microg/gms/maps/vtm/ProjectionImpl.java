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

import android.os.RemoteException;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.internal.IProjectionDelegate;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

import org.oscim.core.Point;
import org.oscim.map.Viewport;

public class ProjectionImpl extends IProjectionDelegate.Stub {
    private Viewport viewport;
    private float[] extents = new float[8];

    public ProjectionImpl(Viewport viewport) {
        this.viewport = viewport;
    }

    @Override
    public LatLng fromScreenLocation(IObjectWrapper obj) throws RemoteException {
        Point point = GmsMapsTypeHelper
                .fromPoint((android.graphics.Point) ObjectWrapper.unwrap(obj));
        return GmsMapsTypeHelper
                .toLatLng(viewport.fromScreenPoint((float) point.x, (float) point.y));
    }

    @Override
    public IObjectWrapper toScreenLocation(LatLng latLng) throws RemoteException {
        Point point = new Point();
        viewport.toScreenPoint(GmsMapsTypeHelper.fromLatLng(latLng), point);
        return ObjectWrapper.wrap(GmsMapsTypeHelper.toPoint(point));
    }

    @Override
    public VisibleRegion getVisibleRegion() throws RemoteException {
        viewport.getMapExtents(extents, 0);
        // TODO: Support non-flat map extents
        return new VisibleRegion(GmsMapsTypeHelper.toLatLngBounds(viewport.getBBox(null, 0)));
    }
}
