/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.common;

import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.android.gms.common.internal.GoogleCertificatesQuery;
import com.google.android.gms.common.internal.IGoogleCertificatesApi;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.PackageUtils;

@Keep
public class GoogleCertificatesImpl extends IGoogleCertificatesApi.Stub  {
    private static final String TAG = "GmsCertImpl";

    @Override
    public IObjectWrapper getGoogleCertficates() throws RemoteException {
        Log.d(TAG, "unimplemented Method: getGoogleCertficates");
        return null;
    }

    @Override
    public IObjectWrapper getGoogleReleaseCertificates() throws RemoteException {
        Log.d(TAG, "unimplemented Method: getGoogleReleaseCertificates");
        return null;
    }

    @Override
    public boolean isGoogleReleaseSigned(String packageName, IObjectWrapper certData) throws RemoteException {
        return PackageUtils.isGooglePackage(packageName, ObjectWrapper.unwrapTyped(certData, byte[].class));
    }

    @Override
    public boolean isGoogleSigned(String packageName, IObjectWrapper certData) throws RemoteException {
        return PackageUtils.isGooglePackage(packageName, ObjectWrapper.unwrapTyped(certData, byte[].class));
    }

    @Override
    public boolean isGoogleOrPlatformSigned(GoogleCertificatesQuery query, IObjectWrapper packageManager) throws RemoteException {
        PackageManager pm = ObjectWrapper.unwrapTyped(packageManager, PackageManager.class);
        if (query == null || query.getPackageName() == null) {
            return false;
        } else if (query.getCertData() == null) {
            if (pm == null) return false;
            return PackageUtils.isGooglePackage(pm, query.getPackageName());
        } else {
            return PackageUtils.isGooglePackage(query.getPackageName(), query.getCertData().getBytes());
        }
    }
}
