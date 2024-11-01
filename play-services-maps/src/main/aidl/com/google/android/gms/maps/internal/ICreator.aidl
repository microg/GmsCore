package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.internal.IMapFragmentDelegate;
import com.google.android.gms.maps.internal.IMapViewDelegate;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.internal.IStreetViewPanoramaFragmentDelegate;
import com.google.android.gms.maps.internal.IStreetViewPanoramaViewDelegate;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

interface ICreator {
    void init(IObjectWrapper resources) = 0;
    IMapFragmentDelegate newMapFragmentDelegate(IObjectWrapper activity) = 1;
    IMapViewDelegate newMapViewDelegate(IObjectWrapper context, in GoogleMapOptions options) = 2;
    ICameraUpdateFactoryDelegate newCameraUpdateFactoryDelegate() = 3;
    IBitmapDescriptorFactoryDelegate newBitmapDescriptorFactoryDelegate() = 4;
    void initV2(IObjectWrapper resources, int versionCode) = 5;
    IStreetViewPanoramaViewDelegate newStreetViewPanoramaViewDelegate(IObjectWrapper context, in StreetViewPanoramaOptions options) = 6;
    IStreetViewPanoramaFragmentDelegate newStreetViewPanoramaFragmentDelegate(IObjectWrapper activity) = 7;
    int getRendererType() = 8;
    void logInitialization(IObjectWrapper context, int preferredRenderer) = 9;
}
