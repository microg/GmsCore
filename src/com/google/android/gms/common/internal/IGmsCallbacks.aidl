package com.google.android.gms.common.internal;

interface IGmsCallbacks {
	void onPostInitComplete(int code, IBinder binder, in Bundle params);
}
