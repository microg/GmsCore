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

package com.google.android.gms.appdatasearch;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class QuerySpecification extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 2;
    @SafeParceled(1)
    public final boolean b;
    //@SafeParceled(value = 2, subType = "TODO")
    public final List c;
    //@SafeParceled(value = 3, subType = "TODO")
    public final List d;
    @SafeParceled(4)
    public final boolean e;
    @SafeParceled(5)
    public final int f;
    @SafeParceled(6)
    public final int g;
    @SafeParceled(7)
    public final boolean h;
    @SafeParceled(8)
    public final int i;

    private QuerySpecification() {
        b = false;
        c = null;
        d = null;
        e = false;
        f = 0;
        g = 0;
        h = false;
        i = 0;
    }

    @Override
    public String toString() {
        return "QuerySpecification{" +
                "versionCode=" + versionCode +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                ", f=" + f +
                ", g=" + g +
                ", h=" + h +
                ", i=" + i +
                '}';
    }

    public static final Creator<QuerySpecification> CREATOR = new AutoCreator<QuerySpecification>(QuerySpecification.class);
}
