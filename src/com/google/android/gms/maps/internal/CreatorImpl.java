package com.google.android.gms.maps.internal;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.internal.BitmapDescriptorFactoryImpl;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

public class CreatorImpl extends ICreator.Stub {
	@Override
	public void init(IObjectWrapper resources) throws RemoteException {
		initV2(resources, 0);
	}

	@Override
	public IMapFragmentDelegate newMapFragmentDelegate(IObjectWrapper activity) throws RemoteException {
		return new MapFragmentImpl((Activity)ObjectWrapper.unwrap(activity));
	}

	@Override
	public IMapViewDelegate newMapViewDelegate(IObjectWrapper context, GoogleMapOptions options) throws RemoteException {
		return new MapViewImpl((Context)ObjectWrapper.unwrap(context), options);
	}

	@Override
	public ICameraUpdateFactoryDelegate newCameraUpdateFactoryDelegate() throws RemoteException {
		return new CameraUpdateFactoryImpl();
	}

	@Override
	public IBitmapDescriptorFactoryDelegate newBitmapDescriptorFactoryDelegate() throws RemoteException {
		return new BitmapDescriptorFactoryImpl();
	}

	@Override
	public void initV2(IObjectWrapper resources, int flags) throws RemoteException {
		ResourcesContainer.set((Resources) ObjectWrapper.unwrap(resources));
	}
}
