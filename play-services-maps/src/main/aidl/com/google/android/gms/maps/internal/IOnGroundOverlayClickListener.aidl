package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate;

interface IOnGroundOverlayClickListener {
    void onGroundOverlayClick(IGroundOverlayDelegate groundOverlay);
}