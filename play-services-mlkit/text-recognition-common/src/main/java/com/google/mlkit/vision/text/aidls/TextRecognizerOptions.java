/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.aidls;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class TextRecognizerOptions extends AbstractSafeParcelable {
    @Field(1)
    public String configLabel = "";

    @Field(2)
    public String libraryName = "";

    @Field(3)
    public String customModel = "";

    @Field(4)
    public boolean optionFlag = false;

    @Field(5)
    public int languageOption = 1;

    @Field(6)
    public String languageHint = "";

    @Field(7)
    public boolean enableOption = false;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TextRecognizerOptions> CREATOR = findCreator(TextRecognizerOptions.class);
}
