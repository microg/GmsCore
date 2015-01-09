package org.microg.gms.maps.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.model.BitmapDescriptor;

public class BitmapDescriptorImpl {
    private BitmapDescriptor descriptor;
    private boolean loadStarted = false;

    public BitmapDescriptorImpl(IObjectWrapper remoteObject) {
        this(new BitmapDescriptor(remoteObject));
    }

    public BitmapDescriptorImpl(BitmapDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public BitmapDescriptorImpl() {
        this(new ObjectWrapper<>(new DefaultBitmapDescriptor(0)));
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

    public synchronized void loadBitmapAsync(final Context context, final Runnable after) {
        if (loadStarted)
            return;
        loadStarted = true;
        if (getDescriptor() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("BitmapDescriptor", "Start loading " + getDescriptor());
                    if (getDescriptor().loadBitmap(context) != null) {
                        after.run();
                    }
                    Log.d("BitmapDescriptor", "Done loading " + getDescriptor());
                }
            }).start();
        }
    }
}
