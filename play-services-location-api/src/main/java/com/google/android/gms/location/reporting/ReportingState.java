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

package com.google.android.gms.location.reporting;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ReportingState extends AutoSafeParcelable {
    @Field(1)
    @Deprecated
    private int versionCode = 2;
    @Field(2)
    public int reportingEnabled;
    @Field(3)
    public int historyEnabled;
    @Field(4)
    public boolean allowed;
    @Field(5)
    public boolean active;
    @Field(6)
    public boolean defer;
    @Field(7)
    public int expectedOptInResult;
    @Field(8)
    public Integer deviceTag;
    @Field(9)
    public int expectedOptInResultAssumingLocationEnabled;
    @Field(10)
    public boolean canAccessSettings;

    public static final Creator<ReportingState> CREATOR = new AutoCreator<ReportingState>(ReportingState.class);
}
