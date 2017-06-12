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

package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConnectionConfiguration extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final String name;
    @SafeParceled(3)
    public final String address;
    @SafeParceled(4)
    public final int type;
    @SafeParceled(5)
    public final int role;
    @SafeParceled(6)
    public final boolean enabled;
    @SafeParceled(7)
    public boolean connected = false;
    @SafeParceled(8)
    public String peerNodeId;
    @SafeParceled(9)
    public boolean btlePriority = true;
    @SafeParceled(10)
    public String nodeId;

    private ConnectionConfiguration() {
        name = address = null;
        type = role = 0;
        enabled = false;
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.role = role;
        this.enabled = enabled;
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled, String nodeId) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.role = role;
        this.enabled = enabled;
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConnectionConfiguration{");
        sb.append("name='").append(name).append('\'');
        sb.append(", address='").append(address).append('\'');
        sb.append(", type=").append(type);
        sb.append(", role=").append(role);
        sb.append(", enabled=").append(enabled);
        sb.append(", connected=").append(connected);
        sb.append(", peerNodeId='").append(peerNodeId).append('\'');
        sb.append(", btlePriority=").append(btlePriority);
        sb.append(", nodeId='").append(nodeId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static final Creator<ConnectionConfiguration> CREATOR = new AutoCreator<ConnectionConfiguration>(ConnectionConfiguration.class);
}
