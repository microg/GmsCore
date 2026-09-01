/*
 * Copyright (C) 2013-2026 microG Project Team
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

package com.google.android.gms.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.wearable.Node;
import org.microg.safeparcel.AutoSafeParcelable;

public class NodeParcelable extends AutoSafeParcelable implements Node {
    @Field(1)
    private int versionCode = 1;

    @Field(2)
    private String id;

    @Field(3)
    private String displayName;

    @Field(4)
    private int hopCount;

    @Field(5)
    private boolean isNearby;

    public NodeParcelable() {}

    public NodeParcelable(String id, String displayName, boolean isNearby) {
        this.id = id;
        this.displayName = displayName;
        this.isNearby = isNearby;
        this.hopCount = 1;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isNearby() {
        return isNearby;
    }

    public static final Parcelable.Creator<NodeParcelable> CREATOR = new AutoCreator<>(NodeParcelable.class);
}