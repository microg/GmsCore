package com.google.android.gms.phenotype.internal;

import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks;

interface IPhenotypeService {
    void register(IPhenotypeCallbacks callbacks, String p1, int p2, in String[] p3, in byte[] p4) = 0;
    void register2(IPhenotypeCallbacks callbacks, String p1, int p2, in String[] p3, in int[] p4, in byte[] p5) = 1;
    void getConfigurationSnapshot(IPhenotypeCallbacks callbacks, String p1, String p2, String p3) = 10;
}
