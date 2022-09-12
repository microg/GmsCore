package com.google.android.gms.oss.licenses;

import com.google.android.gms.oss.licenses.License;

interface IOSSLicenseService {
    String getListLayoutPackage(String packageName) = 1;
    String getLicenseLayoutPackage(String packageName) = 2;
    String getLicenseDetail(String license) = 3;
    List<License> getLicenseList(in List<License> list) = 4;
}
