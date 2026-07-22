package com.google.android.gms.appdatasearch.internal;

import com.google.android.gms.appdatasearch.internal.ILightweightAppDataSearchCallbacks;
import com.google.android.gms.appdatasearch.UsageInfo;

interface ILightweightAppDataSearch {
    void view(ILightweightAppDataSearchCallbacks callbacks, String packageName, in UsageInfo[] usageInfos);
}
