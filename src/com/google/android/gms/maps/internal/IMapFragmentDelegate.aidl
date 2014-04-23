package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IMapFragmentDelegate {
	IGoogleMapDelegate getMap();
	void onInflate(IObjectWrapper activity, in GoogleMapOptions options, in Bundle savedInstanceState);
	void onCreate(in Bundle savedInstanceState);
	IObjectWrapper onCreateView(IObjectWrapper layoutInflate, IObjectWrapper container, in Bundle savedInstanceState);
	void onResume();
	void onPause();
	void onDestroyView();
	void onDestroy();
	void onLowMemory();
	void onSaveInstanceState(inout Bundle outState);
	boolean isReady();
}
