package com.google.firebase.dynamiclinks.internal;


import com.google.firebase.dynamiclinks.internal.DynamicLinkData;
import com.google.firebase.dynamiclinks.internal.ShortDynamicLink;

import com.google.android.gms.common.api.Status;

interface IDynamicLinksCallbacks {
    void onStatusDynamicLinkData(in Status status, in DynamicLinkData dldata) = 0;
    void onStatusShortDynamicLink(in Status status, in ShortDynamicLink sdlink) = 1;
}
