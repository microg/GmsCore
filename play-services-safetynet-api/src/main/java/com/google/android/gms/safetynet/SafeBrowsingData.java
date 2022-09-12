/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.safetynet;

import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.data.DataHolder;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.io.File;

public class SafeBrowsingData extends AutoSafeParcelable {
    @Field(1)
    public int versionCode = 1;
    @Field(2)
    public String status;
    @Field(3)
    public DataHolder data;
    @Field(4)
    public ParcelFileDescriptor fileDescriptor;
    public File file;
    public byte[] fileContents;
    @Field(5)
    public long field5;
    @Field(6)
    public byte[] field6;

    public static final Creator<SafeBrowsingData> CREATOR = new AutoCreator<SafeBrowsingData>(SafeBrowsingData.class);
}
