package com.google.android.gms.common.moduleinstall.internal;

import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;

interface IModuleInstallStatusListener {
    void onModuleInstallStatusUpdate(in ModuleInstallStatusUpdate statusUpdate) = 0;
}
