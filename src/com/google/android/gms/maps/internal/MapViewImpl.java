package com.google.android.gms.maps.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;

public class MapViewImpl extends IMapViewDelegate.Stub {

	private GoogleMapImpl map;
	private GoogleMapOptions options;
	private Context context;

	public MapViewImpl(Context context, GoogleMapOptions options) {
		this.context = context;
		this.options = options;
	}

	private GoogleMapImpl myMap() {
		if (map == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			map = new GoogleMapImpl(inflater, options);
		}
		return map;
	}

	@Override
	public IGoogleMapDelegate getMap() throws RemoteException {
		return myMap().getDelegate();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) throws RemoteException {
		myMap().onCreate(savedInstanceState);
	}

	@Override
	public void onResume() throws RemoteException {

	}

	@Override
	public void onPause() throws RemoteException {

	}

	@Override
	public void onDestroy() throws RemoteException {

	}

	@Override
	public void onLowMemory() throws RemoteException {

	}

	@Override
	public void onSaveInstanceState(Bundle outState) throws RemoteException {

	}

	@Override
	public IObjectWrapper getView() throws RemoteException {
		return ObjectWrapper.wrap(myMap().getView());
	}
}
