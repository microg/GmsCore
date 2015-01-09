package org.microg.gms.maps;

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
        return new VisibleRegion(GmsMapsTypeHelper.toLatLngBounds(viewport.getBBox()));
    }
}
