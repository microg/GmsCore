/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.CertData;
import com.google.android.gms.common.internal.ICertData;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GoogleCertificatesQuery extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getCallingPackage")
    String callingPackage;
    @Field(2)
    IBinder certDataBinder;
    private CertData certData;
    @Field(3)
    boolean allowTestKeys;
    @Field(4)
    boolean ignoreTestKeysOverride;

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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleCertificatesQuery> CREATOR = findCreator(GoogleCertificatesQuery.class);
}
