package com.google.android.gms.phenotype.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.phenotype.Configurations;

interface IPhenotypeCallbacks {
    oneway void onRegister(in Status status) = 0;
    oneway void onConfigurations(in Status status, in Configurations configurations) = 3;
}
