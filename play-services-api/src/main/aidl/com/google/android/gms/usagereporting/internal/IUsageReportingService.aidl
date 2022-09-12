package com.google.android.gms.usagereporting.internal;

import com.google.android.gms.usagereporting.internal.IUsageReportingCallbacks;
import com.google.android.gms.usagereporting.internal.IUsageReportingOptInOptionsChangedListener;
import com.google.android.gms.usagereporting.UsageReportingOptInOptions;

interface IUsageReportingService {
    oneway void getOptInOptions(IUsageReportingCallbacks callbacks) = 1;
    oneway void setOptInOptions(in UsageReportingOptInOptions options, IUsageReportingCallbacks callbacks) = 2;
    oneway void addOptInOptionsChangedListener(IUsageReportingOptInOptionsChangedListener listener, IUsageReportingCallbacks callbacks) = 3;
    oneway void removeOptInOptionsChangedListener(IUsageReportingOptInOptionsChangedListener listener, IUsageReportingCallbacks callbacks) = 4;
}
