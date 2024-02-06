package com.google.android.gms.panorama.internal;

import com.google.android.gms.panorama.internal.IPanoramaCallbacks;
import android.os.Bundle;
import android.net.Uri;

interface IPanoramaService {
    void loadPanoramaInfoAndGrantAccess(IPanoramaCallbacks callback, in Uri uri, in Bundle bundle, boolean needGrantReadUriPermissions) = 0;
}