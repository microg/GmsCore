package com.google.android.gms.common.internal;

import com.google.android.gms.common.internal.GoogleCertificatesQuery;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IGoogleCertificatesApi {
    IObjectWrapper getGoogleCertificates();
    IObjectWrapper getGoogleReleaseCertificates();
    boolean isGoogleReleaseSigned(String packageName, IObjectWrapper certData);
    boolean isGoogleSigned(String packageName, IObjectWrapper certData);
    boolean isGoogleOrPlatformSigned(in GoogleCertificatesQuery query, IObjectWrapper packageManager);
}