package com.google.android.gms.maps.model.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;

interface IMarkerDelegate {
    void remove();
    String getId();
    void setPosition(in LatLng pos);
    LatLng getPosition();
    void setTitle(String title);
    String getTitle();
    void setSnippet(String snippet);
    String getSnippet();
    void setDraggable(boolean drag);
    boolean isDraggable();
    void showInfoWindow();
    void hideInfoWindow();
    boolean isInfoWindowShown();
    void setVisible(boolean visible);
    boolean isVisible();
    boolean equalsRemote(IMarkerDelegate other);
    int hashCodeRemote();
    void setIcon(IObjectWrapper obj);
    void setAnchor(float x, float y);
    void setFlat(boolean flat);
    boolean isFlat();
    void setRotation(float rotation);
    float getRotation();
    void setInfoWindowAnchor(float x, float y);
    void setAlpha(float alpha);
    float getAlpha();
    void setZIndex(float zIndex);
    float getZIndex();
    void setTag(IObjectWrapper obj);
    IObjectWrapper getTag();
}
