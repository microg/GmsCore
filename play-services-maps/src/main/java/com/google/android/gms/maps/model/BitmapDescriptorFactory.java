/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;
import org.microg.gms.common.Hide;

public class BitmapDescriptorFactory {
    private static IBitmapDescriptorFactoryDelegate delegate;
    @Hide
    public static void setDelegate(@NonNull IBitmapDescriptorFactoryDelegate delegate) {
        BitmapDescriptorFactory.delegate = delegate;
    }
    private static IBitmapDescriptorFactoryDelegate getDelegate() {
        if (delegate == null) throw new IllegalStateException("CameraUpdateFactory is not initialized");
        return delegate;
    }
}
