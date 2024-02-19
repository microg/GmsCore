/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import android.app.ApplicationErrorReport;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.data.BitmapTeleporter;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class ErrorReport extends AbstractSafeParcelable {
    @Field(2)
    public ApplicationErrorReport applicationErrorReport;
    @Field(3)
    public String feedbackMsg;
    @Field(4)
    public int versionCode;
    @Field(5)
    public String versionName;
    @Field(6)
    public String mobileDevice;
    @Field(7)
    public String mobileDisplay;
    @Field(8)
    public String mobileType;
    @Field(9)
    public String mobileModel;
    @Field(10)
    public String mobileProduct;
    @Field(11)
    public String mobileFingerprint;
    @Field(12)
    public int mobileSdkInt;
    @Field(13)
    public String mobileRelease;
    @Field(14)
    public String mobileIncremental;
    @Field(15)
    public String mobileCodeName;
    @Field(16)
    public String mobileBoard;
    @Field(17)
    public String mobileBrand;
    @Field(18)
    public String[] unknownStringArray18;
    @Field(19)
    public String[] unknownStringArray19;
    @Field(20)
    public String[] unknownStringArray20;
    @Field(21)
    public String unknownString21;
    @Field(22)
    public String screenshotImgSrc;
    @Field(23)
    public byte[] screenshotImgData;
    @Field(24)
    public int screenshotHeight;
    @Field(25)
    public int screenshotWidth;
    @Field(26)
    public int phoneType;
    @Field(27)
    public int networkType;
    @Field(28)
    public String networkOperatorName;
    @Field(29)
    public String email;
    @Field(30)
    public String languageTag;
    @Field(31)
    public Bundle bundle;
    @Field(32)
    public boolean isFixedUri;
    @Field(33)
    public int mobileCountryCode;
    @Field(34)
    public int mobileNetworkCode;
    @Field(35)
    public boolean unknownBool35;
    @Field(36)
    public String exceptionClassName;
    @Field(37)
    public String throwFileName;
    @Field(38)
    public int throwLineNumber;
    @Field(39)
    public String throwClassName;
    @Field(40)
    public String throwMethodName;
    @Field(41)
    public String stackTrace;
    @Field(42)
    public String exceptionMessage;
    @Field(43)
    public String unknownString43;
    @Field(44)
    public String unknownString44;
    @Field(45)
    public String packageName;
    @Field(46)
    public BitmapTeleporter bitmapTeleporter;
    @Field(47)
    public String unknownString47;
    @Field(48)
    public FileTeleporter[] files;
    @Field(49)
    public String[] unknownByteArray49;
    @Field(50)
    public boolean unknownBool50;
    @Field(51)
    public String unknownString51;
    @Field(52)
    public ThemeSettings themeSettings;
    @Field(53)
    public LogOptions logOptions;
    @Field(54)
    public String unknownString54;
    @Field(55)
    public boolean unknownBool55;
    @Field(56)
    public Bundle bundleText;
    @Field(57)
    public List<RectF> rectFS;
    @Field(58)
    public boolean unknownBool58;
    @Field(59)
    public Bitmap bitmap;
    @Field(60)
    public String unknownString60;
    @Field(61)
    public List<String> camList;
    @Field(62)
    public int unknownInt62;
    @Field(63)
    public int unknownInt63;
    @Field(64)
    public String[] unknownStringArray64;
    @Field(65)
    public String[] unknownStringArray65;
    @Field(66)
    public String[] unknownStringArray66;

    public static final SafeParcelableCreatorAndWriter<ErrorReport> CREATOR = findCreator(ErrorReport.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
