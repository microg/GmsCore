/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Request for importing credentials from another credential provider.
 */
@SafeParcelable.Class
public class ImportCredentialsRequest extends AbstractSafeParcelable {
    @NonNull
    public static final String REQUEST_TYPE = "androidx.identitycredentials.TYPE_CREDENTIALS_SYNC";
    @Field(value = 1, getterName = "getRequestJson")
    @NonNull
    private final String requestJson;
    @Field(value = 2, getterName = "getUri")
    @NonNull
    private final Uri uri;

    /**
     * @param requestJson the request in JSON format, based on the CXF prototcol
     * @param uri         the file URI responsible for the export transport
     */
    @Constructor
    public ImportCredentialsRequest(@NonNull @Param(1) String requestJson, @NonNull @Param(2) Uri uri) {
        this.requestJson = requestJson;
        this.uri = uri;
    }

    /**
     * the request in JSON format, based on the CXF prototcol
     */
    @NonNull
    public final String getRequestJson() {
        return this.requestJson;
    }

    /**
     * the file URI responsible for the export transport
     */
    @NonNull
    public final Uri getUri() {
        return this.uri;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImportCredentialsRequest> CREATOR = findCreator(ImportCredentialsRequest.class);
}
