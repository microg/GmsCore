package com.google.android.gms.dynamite;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IDynamiteLoader {
    int getModuleVersion(IObjectWrapper wrappedContext, String moduleId) = 0;
    int getModuleVersion2(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) = 2;
    int getModuleVersionV2(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) = 4;
    IObjectWrapper getModuleVersionV3(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired, long requestStartTime) = 6;

    IObjectWrapper createModuleContext(IObjectWrapper wrappedContext, String moduleId, int minVersion) = 1;
    IObjectWrapper createModuleContextV2(IObjectWrapper wrappedContext, String moduleId, int minVersion) = 3;
    IObjectWrapper createModuleContextV3(IObjectWrapper wrappedContext, String moduleId, int minVersion, IObjectWrapper cursorWrapped) = 7;

    int getIDynamiteLoaderVersion() = 5;
}
