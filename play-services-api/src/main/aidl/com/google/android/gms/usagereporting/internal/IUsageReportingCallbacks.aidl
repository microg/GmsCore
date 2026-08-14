package com.google.android.gms.usagereporting.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.usagereporting.UsageReportingOptInOptions;

interface IUsageReportingCallbacks {
    oneway void onOptInOptions(in Status status, in UsageReportingOptInOptions options) = 1;
    oneway void onOptInOptionsSet(in Status status) = 2;
    oneway void onOptInOptionsChangedListenerAdded(in Status status) = 3;
    oneway void onOptInOptionsChangedListenerRemoved(in Status status) = 4;
}
