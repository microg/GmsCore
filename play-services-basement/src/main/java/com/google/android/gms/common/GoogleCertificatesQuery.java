/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.common.internal.CertData;
import com.google.android.gms.common.internal.ICertData;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class GoogleCertificatesQuery extends AutoSafeParcelable {
    @Field(1)
    private String callingPackage;
    @Field(2)
    private IBinder certDataBinder;
    private CertData certData;
    @Field(3)
    private boolean allowTestKeys;
    @Field(4)
    private boolean ignoreTestKeysOverride;

    public String getCallingPackage() {
        return callingPackage;
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
