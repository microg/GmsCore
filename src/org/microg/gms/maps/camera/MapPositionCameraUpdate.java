package org.microg.gms.maps.camera;

import org.oscim.core.MapPosition;
import org.oscim.map.Map;

public abstract class MapPositionCameraUpdate implements CameraUpdate {

    abstract MapPosition getMapPosition(Map map);

    @Override
    public void apply(Map map) {
        map.setMapPosition(getMapPosition(map));
    }

    @Override
    public void applyAnimated(Map map, int duration) {
        map.animator().animateTo(duration, getMapPosition(map));
    }
}
