package com.google.android.gms.maps.model.internal;

import android.graphics.Bitmap;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class BitmapDescriptorFactoryImpl extends IBitmapDescriptorFactoryDelegate.Stub {

	@Override
	public IObjectWrapper fromResource(int resourceId) throws RemoteException {
		return ObjectWrapper.wrap(new ResourceBitmapDescriptor(resourceId));
	}

	@Override
	public IObjectWrapper fromAsset(String assetName) throws RemoteException {
		return ObjectWrapper.wrap(new AssetBitmapDescriptor(assetName));
	}

	@Override
	public IObjectWrapper fromFile(String fileName) throws RemoteException {
		return ObjectWrapper.wrap(new FileBitmapDescriptor(fileName));
	}

	@Override
	public IObjectWrapper defaultMarker() throws RemoteException {
		return ObjectWrapper.wrap(new DefaultBitmapDescriptor(0));
	}

	@Override
	public IObjectWrapper defaultMarkerWithHue(float hue) throws RemoteException {
		return ObjectWrapper.wrap(new DefaultBitmapDescriptor(hue));
	}

	@Override
	public IObjectWrapper fromBitmap(Bitmap bitmap) throws RemoteException {
		return ObjectWrapper.wrap(new BitmapBitmapDescriptor(bitmap));
	}

	@Override
	public IObjectWrapper fromPath(String absolutePath) throws RemoteException {
		return ObjectWrapper.wrap(new PathBitmapDescriptor(absolutePath));
	}
}
