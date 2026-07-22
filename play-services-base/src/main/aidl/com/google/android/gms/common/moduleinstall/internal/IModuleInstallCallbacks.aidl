package com.google.android.gms.common.moduleinstall.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse;

interface IModuleInstallCallbacks {
    void onModuleAvailabilityResponse(in Status status, in ModuleAvailabilityResponse response) = 0;
    void onModuleInstallResponse(in Status status, in ModuleInstallResponse response) = 1;
    void onModuleInstallIntentResponse(in Status status, in ModuleInstallIntentResponse response) = 2;
    void onStatus(in Status status) = 3;
}
