package com.google.android.gms.constellation;

import java.util.List;
import com.google.android.gms.constellation.ImsiRequest;
import com.google.android.gms.constellation.IdTokenRequest;

parcelable GetPnvCapabilitiesRequest {
    // public final String a;
    String a;
    // public final List b; (ImsiRequest[])
    List<ImsiRequest> b;
    // public final List c; (IdTokenRequest[])
    List<IdTokenRequest> c;
}
