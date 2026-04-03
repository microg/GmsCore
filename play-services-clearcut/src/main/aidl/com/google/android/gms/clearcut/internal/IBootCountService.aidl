package com.google.android.gms.clearcut.internal;

import com.google.android.gms.clearcut.internal.IBootCountCallbacks;

interface IBootCountService {
    void responseBootCount(IBootCountCallbacks call) = 0;
}
