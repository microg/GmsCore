/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import android.app.ApplicationErrorReport;
import android.graphics.Bitmap;
import org.microg.safeparcel.AutoSafeParcelable;

public class ErrorReport extends AutoSafeParcelable {
    @Field(2)
    public ApplicationErrorReport applicationErrorReport;
    @Field(4)
    public int unknownInt4;
    @Field(12)
    public int unknownInt12;
    @Field(24)
    public int unknownInt24;
    @Field(25)
    public int unknownInt25;
    @Field(26)
    public int unknownInt26;
    @Field(27)
    public int unknownInt27;
    @Field(32)
    public boolean unknownBool32;
    @Field(33)
    public int unknownInt33;
    @Field(34)
    public int unknownInt34;
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
    @Field(50)
    public boolean unknownBool50;
    @Field(51)
    public String unknownString51;
    @Field(52)
    public ThemeSettings themeSettings;
    @Field(53)
    public LogOptions logOptions;
    @Field(55)
    public boolean unknownBool55;
    @Field(58)
    public boolean unknownBool58;
    @Field(59)
    public Bitmap unknownBitmap59;
    @Field(60)
    public String unknownString60;
    @Field(62)
    public int unknownInt62;
    @Field(63)
    public int unknownInt63;
    @Field(67)
    public boolean unknownBool67;
    @Field(68)
    public boolean unknownBool68;
    public static final Creator<ErrorReport> CREATOR = findCreator(ErrorReport.class);
}
