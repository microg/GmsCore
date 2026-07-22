/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import android.os.RemoteException;
import androidx.annotation.NonNull;

/**
 * A RuntimeException wrapper for RemoteException. Thrown when normally there is something seriously wrong and there is no way to recover.
 */
public class RuntimeRemoteException extends RuntimeException {
    public RuntimeRemoteException(@NonNull RemoteException e) {
        super(e);
    }
}
