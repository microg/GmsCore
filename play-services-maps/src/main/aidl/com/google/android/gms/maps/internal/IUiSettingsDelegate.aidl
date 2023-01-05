package com.google.android.gms.maps.internal;

interface IUiSettingsDelegate {
    void setZoomControlsEnabled(boolean zoom);
    void setCompassEnabled(boolean compass);
    void setMyLocationButtonEnabled(boolean locationButton);
    void setScrollGesturesEnabled(boolean scrollGestures);
    void setZoomGesturesEnabled(boolean zoomGestures);
    void setTiltGesturesEnabled(boolean tiltGestures);
    void setRotateGesturesEnabled(boolean rotateGestures);
    void setAllGesturesEnabled(boolean gestures);
    boolean isZoomControlsEnabled();
    boolean isCompassEnabled();
    boolean isMyLocationButtonEnabled();
    boolean isScrollGesturesEnabled();
    boolean isZoomGesturesEnabled();
    boolean isTiltGesturesEnabled();
    boolean isRotateGesturesEnabled();
    void setIndoorLevelPickerEnabled(boolean indoorLevelPicker);
    boolean isIndoorLevelPickerEnabled();
    void setMapToolbarEnabled(boolean mapToolbar);
    boolean isMapToolbarEnabled();
    void setScrollGesturesEnabledDuringRotateOrZoom(boolean scrollDuringZoom);
    boolean isScrollGesturesEnabledDuringRotateOrZoom();
}
