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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.BitmapDescriptor;

import java.util.HashSet;
import java.util.Set;

public class BitmapDescriptorImpl {
    private BitmapDescriptor descriptor;
    private boolean loadStarted = false;
    private Set<Runnable> waitingForLoad = new HashSet<Runnable>();

    public BitmapDescriptorImpl(IObjectWrapper remoteObject) {
        this(new BitmapDescriptor(remoteObject));
    }

    public BitmapDescriptorImpl(BitmapDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public BitmapDescriptorImpl() {
        this(new ObjectWrapper<DefaultBitmapDescriptor>(new DefaultBitmapDescriptor(0)));
    }

    public AbstractBitmapDescriptor getDescriptor() {
        if (descriptor.getRemoteObject() == null)
            return null;
        Object unwrap = ObjectWrapper.unwrap(descriptor.getRemoteObject());
        if (unwrap instanceof AbstractBitmapDescriptor) {
            return ((AbstractBitmapDescriptor) unwrap);
        } else {
            return null;
        }
    }

    public Bitmap getBitmap() {
        if (getDescriptor() != null) {
            return getDescriptor().getBitmap();
        }
        return null;
    }

    public synchronized boolean loadBitmapAsync(final Context context, Runnable after) {
        if (getBitmap() != null) {
            return false;
        }
        waitingForLoad.add(after);
        if (loadStarted)
            return true;
        loadStarted = true;
        if (getDescriptor() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("BitmapDescriptor", "Start loading " + getDescriptor());
                    if (getDescriptor().loadBitmap(context) != null) {
                        Set<Runnable> waitingForLoad;
                        synchronized (BitmapDescriptorImpl.this) {
                            waitingForLoad = BitmapDescriptorImpl.this.waitingForLoad;
                        }
                        for (Runnable after : waitingForLoad) {
                            after.run();
                        }
                    }
                    Log.d("BitmapDescriptor", "Done loading " + getDescriptor());
                }
            }).start();
        }
        return true;
    }
}
