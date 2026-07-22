package com.google.android.gms.common.moduleinstall.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener;

interface IModuleInstallService {
    void areModulesAvailable(IModuleInstallCallbacks callbacks, in ApiFeatureRequest request) = 0;
    void installModules(IModuleInstallCallbacks callbacks, in ApiFeatureRequest request, IModuleInstallStatusListener listener) = 1;
    void getInstallModulesIntent(IModuleInstallCallbacks callbacks, in ApiFeatureRequest request) = 2;
    void releaseModules(IStatusCallback callback, in ApiFeatureRequest request) = 3;
    void unregisterListener(IStatusCallback callback, IModuleInstallStatusListener listener) = 5;
}
