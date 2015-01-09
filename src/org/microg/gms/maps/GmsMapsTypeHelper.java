package org.microg.gms.maps;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;

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

    public static LatLngBounds toLatLngBounds(BoundingBox box) {
        return new LatLngBounds(new LatLng(box.getMinLatitude(), box.getMinLongitude()),
                new LatLng(box.getMaxLatitude(), box.getMaxLongitude()));
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
        return new BoundingBox(bounds.southWest.latitude, bounds.southWest.longitude,
                bounds.northEast.latitude, bounds.northEast.longitude);
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
