/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;

import java.util.HashMap;
import java.util.Map;

@PublicApi
@SafeParcelable.Class
public class PrfExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getEntries")
    private final Bundle entries;

    @Constructor
    public PrfExtension(@Param(1) Bundle entries) {
        this.entries = entries;
    }

    @NonNull
    public Bundle getEntries() {
        return entries;
    }

    public PrfExtension(Map<String, byte[][]> entries) {
        Bundle bundle = new Bundle();
        for (String key : entries.keySet()) {
            bundle.putSerializable(key, entries.get(key));
        }
        this.entries = bundle;
    }

    @NonNull
    public Map<String, byte[][]> getEntriesMap() {
        Map<String, byte[][]> map = new HashMap<String, byte[][]>();
        for (String key : entries.keySet()) {
            map.put(key, (byte[][]) entries.getSerializable(key));
        }
        return map;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PrfExtension> CREATOR =
            AbstractSafeParcelable.findCreator(PrfExtension.class);

    public static final String DEFAULT = "DEFAULT";
}
