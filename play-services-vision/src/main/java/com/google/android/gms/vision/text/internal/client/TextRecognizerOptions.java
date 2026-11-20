/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.internal.client;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class TextRecognizerOptions extends AbstractSafeParcelable {
    @Field(2)
    public final String language;

    public TextRecognizerOptions() {
        this(null);
    }

    @Constructor
    public TextRecognizerOptions(@Param(2) String language) {
        this.language = language;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TextRecognizerOptions> CREATOR = findCreator(TextRecognizerOptions.class);
}
