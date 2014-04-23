package com.google.android.location.internal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.google.android.gms.common.internal.AbstractGmsServiceBroker;

public class GoogleLocationManagerService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return new Broker().asBinder();
	}

	private class Broker extends AbstractGmsServiceBroker {
	}
}
