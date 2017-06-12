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

package com.google.android.gms.wearable.internal;

import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CapabilityInfoParcelable extends AutoSafeParcelable implements CapabilityInfo {
    @SafeParceled(1)
    private int versionCode = 1;

    @SafeParceled(2)
    private String name;

    @SafeParceled(value = 3, subClass = NodeParcelable.class)
    private List<NodeParcelable> nodeParcelables;

    private Set<Node> nodes;
    private Object lock = new Object();

    private CapabilityInfoParcelable() {
    }

    public CapabilityInfoParcelable(String name, List<NodeParcelable> nodeParcelables) {
        this.name = name;
        this.nodeParcelables = nodeParcelables;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized Set<Node> getNodes() {
        if (nodes == null) {
            nodes = new HashSet<Node>(nodeParcelables);
        }
        return nodes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        CapabilityInfoParcelable that = (CapabilityInfoParcelable) other;

        if (versionCode != that.versionCode) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return nodeParcelables != null ? nodeParcelables.equals(that.nodeParcelables) : that.nodeParcelables == null;
    }

    @Override
    public int hashCode() {
        int result = versionCode;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (nodeParcelables != null ? nodeParcelables.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CapabilityInfo{" + name + ", " + nodeParcelables + "}";
    }

    public static final Creator<CapabilityInfoParcelable> CREATOR = new AutoCreator<CapabilityInfoParcelable>(CapabilityInfoParcelable.class);

}
