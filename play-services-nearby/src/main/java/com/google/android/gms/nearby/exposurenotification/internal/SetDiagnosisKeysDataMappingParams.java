/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping;

import org.microg.safeparcel.AutoSafeParcelable;

public class SetDiagnosisKeysDataMappingParams extends AutoSafeParcelable {
    @Field(1)
    public IStatusCallback callback;
    @Field(2)
    public DiagnosisKeysDataMapping mapping;

    private SetDiagnosisKeysDataMappingParams() {}

    public SetDiagnosisKeysDataMappingParams(IStatusCallback callback, DiagnosisKeysDataMapping mapping) {
        this.callback = callback;
        this.mapping = mapping;
    }

    public static final Creator<SetDiagnosisKeysDataMappingParams> CREATOR = new AutoCreator<>(SetDiagnosisKeysDataMappingParams.class);
}
