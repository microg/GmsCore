/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import android.content.Context;
import android.graphics.Bitmap;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.internal.AbstractBitmapDescriptor;

public class BitmapDescriptor {
	private final IObjectWrapper remoteObject;

	public BitmapDescriptor(IObjectWrapper remoteObject) {
		this.remoteObject = remoteObject;
	}

	public IObjectWrapper getRemoteObject() {
		return remoteObject;
	}

    public AbstractBitmapDescriptor getDescriptor() {
        if (remoteObject == null) return null;
        Object unwrap = ObjectWrapper.unwrap(remoteObject);
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

    @Override
    public String toString() {
        return "BitmapDescriptor{" +
                "remote=" + getDescriptor() +
                '}';
    }

    public void loadBitmapAsync(final Context context, final Runnable after) {
        if (getDescriptor() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (getDescriptor().loadBitmap(context) != null) {
                        after.run();
                    }
                }
            }).start();
        }
    }
}
