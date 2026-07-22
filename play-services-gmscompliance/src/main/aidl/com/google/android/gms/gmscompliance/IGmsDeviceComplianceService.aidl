package com.google.android.gms.gmscompliance;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.gmscompliance.IGmsDeviceComplianceServiceCallback;

interface IGmsDeviceComplianceService {
    void getDeviceCompliance(IGmsDeviceComplianceServiceCallback callback);
}
