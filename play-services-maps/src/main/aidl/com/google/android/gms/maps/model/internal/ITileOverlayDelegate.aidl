package com.google.android.gms.maps.model.internal;

interface ITileOverlayDelegate {
    void remove() = 0;
    void clearTileCache() = 1;
    String getId() = 2;
    void setZIndex(float zIndex) = 3;
    float getZIndex() = 4;
    void setVisible(boolean visible) = 5;
    boolean isVisible() = 6;
    boolean equalsRemote(ITileOverlayDelegate other) = 7;
    int hashCodeRemote() = 8;
    void setFadeIn(boolean fadeIn) = 9;
    boolean getFadeIn() = 10;
    void setTransparency(float transparency) = 11;
    float getTransparency() = 12;
}
