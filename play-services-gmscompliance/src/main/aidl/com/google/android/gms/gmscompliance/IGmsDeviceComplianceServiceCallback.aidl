package com.google.android.gms.gmscompliance;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.gmscompliance.GmsDeviceComplianceResponse;

interface IGmsDeviceComplianceServiceCallback {
    oneway void onResponse(in Status status, in GmsDeviceComplianceResponse response);
}
