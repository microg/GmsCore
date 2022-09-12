/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.oss.licenses;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.oss.licenses.IOSSLicenseService;
import com.google.android.gms.oss.licenses.License;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

import java.util.List;

public class OssLicenseServiceApiClient extends GmsClient<IOSSLicenseService> {
    public OssLicenseServiceApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.OSS_LICENSES.ACTION);
        serviceId = GmsService.OSS_LICENSES.SERVICE_ID;
    }

    public String getLicenseLayoutPackage(String packageName) throws RemoteException {
        return getServiceInterface().getLicenseLayoutPackage(packageName);
    }

    public String getListLayoutPackage(String packageName) throws RemoteException {
        return getServiceInterface().getListLayoutPackage(packageName);
    }

    public String getLicenseDetail(License license) throws RemoteException {
        return getServiceInterface().getLicenseDetail(license.toString());
    }

    public List<License> getLicenseList(List<License> licenses) throws RemoteException {
        return getServiceInterface().getLicenseList(licenses);
    }

    @Override
    protected IOSSLicenseService interfaceFromBinder(IBinder binder) {
        return IOSSLicenseService.Stub.asInterface(binder);
    }
}
