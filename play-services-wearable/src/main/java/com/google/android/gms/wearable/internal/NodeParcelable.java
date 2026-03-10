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

import com.google.android.gms.wearable.Node;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Parcelable implementation of the {@link com.google.android.gms.wearable.Node} interface.
 */
public class NodeParcelable extends AutoSafeParcelable implements Node {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private final String nodeId;
    @SafeParceled(3)
    private final String displayName;
    @SafeParceled(4)
    private final int hops;
    @SafeParceled(5)
    private final boolean isNearby;

    private NodeParcelable() {
        nodeId = displayName = null;
        hops = 0;
        isNearby = false;
    }

    public NodeParcelable(String nodeId, String displayName, int hops, boolean isNearby) {
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.hops = hops;
        this.isNearby = isNearby;
    }

    public NodeParcelable(String nodeId, String displayName) {
        this(nodeId, displayName, 0, false);
    }

    public NodeParcelable(Node node) {
        this(node.getId(), node.getDisplayName(), 0, node.isNearby());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeParcelable that = (NodeParcelable) o;

        if (!nodeId.equals(that.nodeId)) return false;
        if (!displayName.equals(that.displayName)) return false;

        return true;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getId() {
        return nodeId;
    }

    @Override
    public boolean isNearby() {
        return isNearby;
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode();
    }

    @Override
    public String toString() {
        return "NodeParcelable{" + displayName + ", id=" + displayName + ", hops=" + hops + ", isNearby=" + isNearby + "}";
    }

    public static final Creator<NodeParcelable> CREATOR = new AutoCreator<NodeParcelable>(NodeParcelable.class);
}
