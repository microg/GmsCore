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
    @SafeParceled(1)
    public int versionCode = 2;
    @SafeParceled(2)
    public int reportingEnabled;
    @SafeParceled(3)
    public int historyEnabled;
    @SafeParceled(4)
    public boolean allowed;
    @SafeParceled(5)
    public boolean active;
    @SafeParceled(6)
    public boolean defer;
    @SafeParceled(7)
    public int expectedOptInResult;
    @SafeParceled(8)
    public Integer deviceTag;
    @SafeParceled(9)
    public int expectedOptInResultAssumingLocationEnabled;

    public static final Creator<ReportingState> CREATOR = new AutoCreator<ReportingState>(ReportingState.class);
}
