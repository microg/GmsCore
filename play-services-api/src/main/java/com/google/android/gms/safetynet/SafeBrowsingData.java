/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
