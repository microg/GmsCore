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

import androidx.annotation.Nullable;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.Arrays;

public class CertData extends ICertData.Stub {
    private final byte[] bytes;
    private final int hashCode;

    public CertData(byte[] bytes) {
        this.bytes = bytes;
        if (bytes.length < 25) throw new RuntimeException("CertData to small");
        hashCode = Arrays.hashCode(Arrays.copyOfRange(bytes, 0, 25));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ICertData)) return false;
        ICertData cert = (ICertData) obj;
        try {
            if (cert.remoteHashCode() != hashCode()) return false;
            return Arrays.equals(ObjectWrapper.unwrapTyped(cert.getWrappedBytes(), byte[].class), getBytes());
        } catch (RemoteException e) {
            return false;
        }
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public IObjectWrapper getWrappedBytes() throws RemoteException {
        return ObjectWrapper.wrap(getBytes());
    }

    @Override
    public int remoteHashCode() throws RemoteException {
        return hashCode();
    }

    @Nullable
    public static CertData unwrap(IBinder certDataBinder) {
        if (certDataBinder instanceof CertData) {
            return (CertData) certDataBinder;
        } else if (certDataBinder instanceof IObjectWrapper) {
            return unwrap((IObjectWrapper) certDataBinder);
        } else if (certDataBinder instanceof ICertData) {
            return unwrap((ICertData) certDataBinder);
        }
        return null;
    }

    public static CertData unwrap(IObjectWrapper certDataWrapper) {
        CertData certData = ObjectWrapper.unwrapTyped(certDataWrapper, CertData.class);
        if (certData != null) return certData;
        byte[] bytes = ObjectWrapper.unwrapTyped(certDataWrapper, byte[].class);
        if (bytes != null) return new CertData(bytes);
        ICertData iCertData = ObjectWrapper.unwrapTyped(certDataWrapper, ICertData.class);
        if (iCertData != null) return unwrap(iCertData);
        return null;
    }

    public static CertData unwrap(ICertData iCertData) {
        if (iCertData == null) return null;
        if (iCertData instanceof CertData) return (CertData) iCertData;
        try {
            byte[] bytes = ObjectWrapper.unwrapTyped(iCertData.getWrappedBytes(), byte[].class);
            if (bytes != null) {
                return new CertData(bytes);
            }
        } catch (RemoteException e) {
            // Ignore
        }
        return null;
    }
}
