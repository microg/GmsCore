/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class ProvideDiagnosisKeysParams extends AutoSafeParcelable {
    @Field(1)
    public List<TemporaryExposureKey> keys;
    @Field(2)
    public IStatusCallback callback;
    @Field(3)
    public List<ParcelFileDescriptor> keyFiles;
    @Field(4)
    public ExposureConfiguration configuration;
    @Field(5)
    public String token;

    public static final Creator<ProvideDiagnosisKeysParams> CREATOR = new AutoCreator<>(ProvideDiagnosisKeysParams.class);
}
