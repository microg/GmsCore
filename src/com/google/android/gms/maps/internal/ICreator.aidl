package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.internal.IMapFragmentDelegate;
import com.google.android.gms.maps.internal.IMapViewDelegate;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

interface ICreator {
    void init(IObjectWrapper resources);
    IMapFragmentDelegate newMapFragmentDelegate(IObjectWrapper activity);
    IMapViewDelegate newMapViewDelegate(IObjectWrapper context, in GoogleMapOptions options);
    ICameraUpdateFactoryDelegate newCameraUpdateFactoryDelegate();
    IBitmapDescriptorFactoryDelegate newBitmapDescriptorFactoryDelegate();
    void initV2(IObjectWrapper resources, int flags);
}
