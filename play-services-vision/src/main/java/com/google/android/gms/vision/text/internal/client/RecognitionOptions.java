/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.internal.client;

import android.graphics.Rect;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class RecognitionOptions extends AbstractSafeParcelable {
    @Field(2)
    public final Rect region;

    public RecognitionOptions() {
        this(new Rect());
    }

    @Constructor
    public RecognitionOptions(@Param(2) Rect region) {
        this.region = region;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RecognitionOptions> CREATOR = findCreator(RecognitionOptions.class);
}
