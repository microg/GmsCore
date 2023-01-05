package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import android.graphics.Bitmap;

interface ISnapshotReadyCallback {
    void onBitmapReady(in Bitmap bitmap);
    void onBitmapWrappedReady(IObjectWrapper wrapper);
}
