/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import android.app.ApplicationErrorReport;
import android.graphics.Bitmap;
import android.os.Bundle;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class FeedbackOptions extends AutoSafeParcelable {
    @Field(3)
    public Bundle unknownBundle3;
    @Field(6)
    public ApplicationErrorReport applicationErrorReport;
    @Field(9)
    public String packageName;
    @Field(10)
    public List<FileTeleporter> files;
    @Field(11)
    public boolean unknownBool11;
    @Field(12)
    public ThemeSettings themeSettings;
    @Field(13)
    public LogOptions logOptions;
    @Field(15)
    public Bitmap screenshot;
    @Field(17)
    public boolean unknownBool17;
    @Field(18)
    public long unknownLong18;
    @Field(19)
    public boolean unknownBool19;
    public static final Creator<FeedbackOptions> CREATOR = findCreator(FeedbackOptions.class);
}
