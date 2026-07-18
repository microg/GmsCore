package com.google.android.gms.asterism.internal;

import android.os.Bundle;

interface IAsterismService {
    Bundle getConsent(Bundle params);
    Bundle setConsent(Bundle params);
}
