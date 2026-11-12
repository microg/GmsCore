package com.google.android.gms.cast.framework.media.internal;

import android.graphics.Bitmap;
import android.net.Uri;

interface IFetchBitmapTask {
    Bitmap doFetch(in Uri uri) = 0;
}
