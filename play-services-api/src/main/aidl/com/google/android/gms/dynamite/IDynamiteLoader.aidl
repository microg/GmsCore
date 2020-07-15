package com.google.android.gms.dynamite;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IDynamiteLoader {
    int getModuleVersion(IObjectWrapper context, String moduleId) = 0;
    int getModuleVersion2(IObjectWrapper context, String moduleId, boolean updateConfigIfRequired) = 2;

    IObjectWrapper createModuleContext(IObjectWrapper context, String moduleId, int minVersion) = 1;
}