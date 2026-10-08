package com.google.android.gms.cast.framework.media.internal;

import android.graphics.Bitmap;
import android.net.Uri;

interface IFetchBitmapTask {
    Bitmap fetchBitmap(in Uri uri) = 0;
}
