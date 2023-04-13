/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class ProvideDiagnosisKeysParams extends AutoSafeParcelable {
    @Field(1)
    @Nullable
    public List<TemporaryExposureKey> keys;
    @Field(2)
    public IStatusCallback callback;
    @Field(3)
    @Nullable
    public List<ParcelFileDescriptor> keyFiles;
    @Field(4)
    @Nullable
    public ExposureConfiguration configuration;
    @Field(5)
    @Nullable
    public String token;
    @Field(6)
    @Nullable
    public IDiagnosisKeyFileSupplier keyFileSupplier;

    private ProvideDiagnosisKeysParams() {
    }

    public ProvideDiagnosisKeysParams(IStatusCallback callback, List<TemporaryExposureKey> keys, List<ParcelFileDescriptor> keyFiles, ExposureConfiguration configuration, String token) {
        this(callback, keyFiles, configuration, token);
        this.keys = keys;
    }

    public ProvideDiagnosisKeysParams(IStatusCallback callback, List<ParcelFileDescriptor> keyFiles, ExposureConfiguration configuration, String token) {
        this.callback = callback;
        this.keyFiles = keyFiles;
        this.configuration = configuration;
        this.token = token;
    }

    public ProvideDiagnosisKeysParams(IStatusCallback callback, IDiagnosisKeyFileSupplier keyFileSupplier) {
        this.callback = callback;
        this.keyFileSupplier = keyFileSupplier;
    }

    public static final Creator<ProvideDiagnosisKeysParams> CREATOR = new AutoCreator<>(ProvideDiagnosisKeysParams.class);
}
