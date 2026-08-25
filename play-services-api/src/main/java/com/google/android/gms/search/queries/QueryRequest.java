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

package com.google.android.gms.search.queries;

import com.google.android.gms.appdatasearch.QuerySpecification;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

public class QueryRequest extends AutoSafeParcelable {

    @SafeParceled(1000)
    public int versionCode = 1;
    @SafeParceled(1)
    public String searchString;
    @SafeParceled(2)
    public String packageName;
    @SafeParceled(3)
    public String[] corpora;
    @SafeParceled(4)
    public int d;
    @SafeParceled(5)
    public int e;
    @SafeParceled(6)
    public QuerySpecification spec;

    @Override
    public String toString() {
        return "QueryRequest{" +
                "versionCode=" + versionCode +
                ", searchString='" + searchString + '\'' +
                ", packageName='" + packageName + '\'' +
                ", corpora=" + Arrays.toString(corpora) +
                ", d=" + d +
                ", e=" + e +
                ", spec=" + spec +
                '}';
    }

    public static Creator<QueryRequest> CREATOR = new AutoCreator<QueryRequest>(QueryRequest.class);
}
