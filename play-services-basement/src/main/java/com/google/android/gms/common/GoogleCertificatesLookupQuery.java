/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.content.Context;
import android.os.IBinder;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import org.microg.safeparcel.AutoSafeParcelable;

public class GoogleCertificatesLookupQuery extends AutoSafeParcelable {
    @Field(1)
    private String callingPackage;
    @Field(2)
    private boolean allowTestKeys;
    @Field(3)
    private boolean ignoreTestKeysOverride;
    @Field(4)
    private IObjectWrapper contextWrapper;
    private Context context;
    @Field(5)
    private boolean isChimeraPackage;
    @Field(6)
    private boolean includeHashesInErrorMessage;

    public String getCallingPackage() {
        return callingPackage;
    }

    public Context getContext() {
        if (context == null && contextWrapper != null) {
            context = ObjectWrapper.unwrapTyped(contextWrapper, Context.class);
        }
        return context;
    }

    public static final Creator<GoogleCertificatesLookupQuery> CREATOR = findCreator(GoogleCertificatesLookupQuery.class);
}
