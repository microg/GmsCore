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

package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class GoogleCertificatesQuery extends AutoSafeParcelable {
    @SafeParceled(1)
    private String packageName;
    @SafeParceled(2)
    private IBinder certDataBinder;
    private CertData certData;
    @SafeParceled(3)
    private boolean allowNonRelease;

    public String getPackageName() {
        return packageName;
    }

    public CertData getCertData() {
        if (certData == null && certDataBinder != null) {
            ICertData iCertData = null;
            if (certDataBinder instanceof CertData) {
                certData = (CertData) certDataBinder;
            } else if (certDataBinder instanceof IObjectWrapper) {
                certData = ObjectWrapper.unwrapTyped((IObjectWrapper) certDataBinder, CertData.class);
                if (certData == null) {
                    byte[] bytes = ObjectWrapper.unwrapTyped((IObjectWrapper) certDataBinder, byte[].class);
                    if (bytes != null) {
                        certData = new CertData(bytes);
                    }
                }
                if (certData == null) {
                    iCertData = ObjectWrapper.unwrapTyped((IObjectWrapper) certDataBinder, ICertData.class);
                }
            } else if (certDataBinder instanceof ICertData) {
                iCertData = (ICertData) certDataBinder;
            }
            if (iCertData != null) {
                try {
                    byte[] bytes = ObjectWrapper.unwrapTyped(iCertData.getWrappedBytes(), byte[].class);
                    if (bytes != null) {
                        certData = new CertData(bytes);
                    }
                } catch (RemoteException e) {
                    // Ignore
                }
            }
        }
        return certData;
    }

    public static final Creator<GoogleCertificatesQuery> CREATOR = new AutoCreator<GoogleCertificatesQuery>(GoogleCertificatesQuery.class);
}
