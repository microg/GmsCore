package com.google.android.gms.maps.model;

import com.google.android.gms.dynamic.IObjectWrapper;

public class BitmapDescriptor {
	private final IObjectWrapper remoteObject;

	public BitmapDescriptor(IObjectWrapper remoteObject) {
		this.remoteObject = remoteObject;
	}

	public IObjectWrapper getRemoteObject() {
		return remoteObject;
	}
}
