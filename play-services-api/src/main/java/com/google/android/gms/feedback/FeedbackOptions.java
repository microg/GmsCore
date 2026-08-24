/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import android.app.ApplicationErrorReport;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.data.BitmapTeleporter;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class FeedbackOptions extends AbstractSafeParcelable {

    public static final SafeParcelableCreatorAndWriter<FeedbackOptions> CREATOR = findCreator(FeedbackOptions.class);

    @Field(2)
    public String unknownString2;
    @Field(3)
    public Bundle unknownBundle3;
    @Field(5)
    public String unknownString3;
    @Field(6)
    public ApplicationErrorReport applicationErrorReport;
    @Field(7)
    public String unknownString7;
    @Field(8)
    public BitmapTeleporter bitmapTeleporter;
    @Field(9)
    public String packageName;
    @Field(10)
    public List<FileTeleporter> files;
    @Field(11)
    public boolean unknownBoolean11;
    @Field(12)
    public ThemeSettings themeSettings;
    @Field(13)
    public LogOptions logOptions;
    @Field(14)
    public boolean unknownBoolean14;
    @Field(15)
    public Bitmap screenshot;
    @Field(16)
    public String unknownString16;
    @Field(17)
    public boolean unknownBoolean17;
    @Field(18)
    public long unknownLong18;
    @Field(19)
    public boolean unknownBool19;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
