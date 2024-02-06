package com.google.android.gms.panorama.internal;

import android.content.Intent;

interface IPanoramaCallbacks {
    void getViewerIntent(int status, in Intent intent);
}