package com.google.android.gms.search.global.internal;

import com.google.android.gms.search.global.GetCurrentExperimentIdsRequest;
import com.google.android.gms.search.global.GetGlobalSearchSourcesRequest;
import com.google.android.gms.search.global.GetPendingExperimentIdsRequest;
import com.google.android.gms.search.global.SetExperimentIdsRequest;
import com.google.android.gms.search.global.SetIncludeInGlobalSearchRequest;
import com.google.android.gms.search.global.internal.IGlobalSearchAdminCallbacks;

interface IGlobalSearchAdminService {
    void getGlobalSearchSources(in GetGlobalSearchSourcesRequest request, IGlobalSearchAdminCallbacks callbacks) = 1;
    void setExperimentIds(in SetExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) = 2;
    void getCurrentExperimentIds(in GetCurrentExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) = 3;
    void getPendingExperimentIds(in GetPendingExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) = 4;

    void setIncludeInGlobalSearch(in SetIncludeInGlobalSearchRequest request, IGlobalSearchAdminCallbacks callbacks) = 7;
}
