/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.plus.plusone;

import android.content.Context;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.plus.internal.IPlusOneButtonCreator;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.plus.PlusOneButtonImpl;

public class PlusOneButtonCreatorImpl  extends IPlusOneButtonCreator.Stub {
	@Override
	public IObjectWrapper create(IObjectWrapper context, int size, int annotation, String url, int activityRequestCode) throws RemoteException {
		Context ctx = (Context) ObjectWrapper.unwrap(context);
		return ObjectWrapper.wrap(new PlusOneButtonImpl(ctx, size, annotation, url, AuthConstants.DEFAULT_ACCOUNT));
	}

	@Override
	public IObjectWrapper createForAccount(IObjectWrapper context, int size, int annotation, String url, String account) throws RemoteException {
		Context ctx = (Context) ObjectWrapper.unwrap(context);
		return ObjectWrapper.wrap(new PlusOneButtonImpl(ctx, size, annotation, url, account));
	}
}
