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

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.oscim.core.BoundingBox;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;

public class GmsMapsTypeHelper {
    public static android.graphics.Point toPoint(org.oscim.core.Point in) {
        return new android.graphics.Point((int) in.getX(), (int) in.getY());
    }

    public static GeoPoint fromLatLng(LatLng latLng) {
        return new GeoPoint(latLng.latitude, latLng.longitude);
    }

    public static LatLng toLatLng(GeoPoint geoPoint) {
        return new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    public static LatLngBounds toLatLngBounds(Box box) {
        double minLon = MercatorProjection.toLongitude(box.xmin);
        double maxLon = MercatorProjection.toLongitude(box.xmax);
        double minLat = MercatorProjection.toLatitude(box.ymax);
        double maxLat = MercatorProjection.toLatitude(box.ymin);
        if (Double.isNaN(minLon) || Double.isNaN(maxLon) || Double.isNaN(minLat) || Double.isNaN(maxLat))
            minLon = maxLon = minLat = maxLat = 0;
        return new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
    }

    public static org.oscim.core.Point fromPoint(android.graphics.Point point) {
        return new org.oscim.core.Point(point.x, point.y);
    }

    public static CameraPosition toCameraPosition(MapPosition mapPosition) {
        return new CameraPosition(new LatLng(mapPosition.getLatitude(), mapPosition.getLongitude()),
                toZoom(mapPosition.getScale()), mapPosition.getTilt(),
                toBearing(mapPosition.getBearing()));
    }

    public static MapPosition fromCameraPosition(CameraPosition cameraPosition) {
        MapPosition mapPosition = new MapPosition(cameraPosition.target.latitude,
                cameraPosition.target.longitude, fromZoom(cameraPosition.zoom));
        mapPosition.setTilt(cameraPosition.tilt);
        mapPosition.setBearing(fromBearing(cameraPosition.bearing));
        return mapPosition;
    }

    public static BoundingBox fromLatLngBounds(LatLngBounds bounds) {
        return new BoundingBox(bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude);
    }

    public static float fromBearing(float bearing) {
        return -bearing;
    }

    public static float toBearing(float bearing) {
        return -bearing;
    }

    public static double fromZoom(float zoom) {
        return Math.pow(2, zoom);
    }

    public static float toZoom(double scale) {
        return (float) (Math.log(scale) / Math.log(2));
    }
}
