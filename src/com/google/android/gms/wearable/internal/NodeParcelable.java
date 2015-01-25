/*
 * Copyright 2014-2015 µg Project Team
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

import com.google.android.gms.wearable.Node;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Parcelable implementation of the {@link com.google.android.gms.wearable.Node} interface.
 */
public class NodeParcelable extends AutoSafeParcelable implements Node {
    @SafeParceled(1)
    private final int versionCode;
    @SafeParceled(2)
    private final String id;
    @SafeParceled(3)
    private final String displayName;

    private NodeParcelable() {
        versionCode = 1;
        id = displayName = null;
    }

    public NodeParcelable(String id, String displayName) {
        versionCode = 1;
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeParcelable that = (NodeParcelable) o;

        if (!id.equals(that.id)) return false;
        if (!displayName.equals(that.displayName)) return false;

        return true;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int result = 37 * 17 + id.hashCode();
        return 37 * result + displayName.hashCode();
    }

    @Override
    public String toString() {
        return "NodeParcelable{" + id + "," + displayName + "}";
    }
}
