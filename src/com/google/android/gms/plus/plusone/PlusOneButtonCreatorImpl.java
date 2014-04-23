package com.google.android.gms.plus.plusone;

import android.content.Context;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.plus.internal.IPlusOneButtonCreator;

public class PlusOneButtonCreatorImpl  extends IPlusOneButtonCreator.Stub {
	@Override
	public IObjectWrapper create(IObjectWrapper context, int size, int annotation, String url, int activityRequestCode) throws RemoteException {
		Context ctx = (Context) ObjectWrapper.unwrap(context);
		return ObjectWrapper.wrap(new PlusOneButtonImpl(ctx, size, annotation, url, "<< default account >>"));
	}

	@Override
	public IObjectWrapper createForAccount(IObjectWrapper context, int size, int annotation, String url, String account) throws RemoteException {
		Context ctx = (Context) ObjectWrapper.unwrap(context);
		return ObjectWrapper.wrap(new PlusOneButtonImpl(ctx, size, annotation, url, account));
	}
}
