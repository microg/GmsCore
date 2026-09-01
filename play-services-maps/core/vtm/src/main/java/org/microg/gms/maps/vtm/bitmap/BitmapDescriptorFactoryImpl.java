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

package org.microg.gms.maps.vtm.bitmap;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

public class BitmapDescriptorFactoryImpl extends IBitmapDescriptorFactoryDelegate.Stub {
    private static final String TAG = "GmsBitmapDescFactory";

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
        return ObjectWrapper.wrap(DefaultBitmapDescriptor.DEFAULT_DESCRIPTOR);
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

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
