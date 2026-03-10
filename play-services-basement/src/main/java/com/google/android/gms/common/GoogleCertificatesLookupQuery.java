/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.content.Context;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

@SafeParcelable.Class
public class GoogleCertificatesLookupQuery extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getCallingPackage")
    String callingPackage;
    @Field(2)
    boolean allowTestKeys;
    @Field(3)
    boolean ignoreTestKeysOverride;
    @Field(4)
    IObjectWrapper contextWrapper;
    private Context context;
    @Field(5)
    boolean isChimeraPackage;
    @Field(6)
    boolean includeHashesInErrorMessage;

    public String getCallingPackage() {
        return callingPackage;
    }

    public Context getContext() {
        if (context == null && contextWrapper != null) {
            context = ObjectWrapper.unwrapTyped(contextWrapper, Context.class);
        }
        return context;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleCertificatesLookupQuery> CREATOR = findCreator(GoogleCertificatesLookupQuery.class);
}
