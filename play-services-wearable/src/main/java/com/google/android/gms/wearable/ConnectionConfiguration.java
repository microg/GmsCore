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

import java.util.List;

public class ConnectionConfiguration extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public String name;
    @SafeParceled(3)
    public String address;
    @SafeParceled(4)
    public int type;
    @SafeParceled(5)
    public int role;
    @SafeParceled(6)
    public boolean enabled;
    @SafeParceled(7)
    public boolean connected = false;
    @SafeParceled(8)
    public String peerNodeId;
    @SafeParceled(9)
    public boolean btlePriority = true;
    @SafeParceled(10)
    public String nodeId;
    @SafeParceled(11)
    public String packageName;
    @SafeParceled(12)
    public int connectionRetryStrategy;
    @SafeParceled(13)
    public List<String> allowedConfigPackages;
    @SafeParceled(14)
    public boolean migrating;
    @SafeParceled(15)
    public boolean dataItemSyncEnabled;
    @SafeParceled(16)
    public ConnectionRestrictions connectionRestrictions;
    @SafeParceled(17)
    public boolean removeConnectionWhenBondRemovedByUser;
    @SafeParceled(18)
    public ConnectionDelayFilters connectionDelayFilters;
    @SafeParceled(19)
    public int maxSupportedRemoteAndroidSdkVersion;

    private ConnectionConfiguration() {
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled) {
        this(name, address, type, role, enabled, false, null, false, null, null, 0, null, false, false, null, false, null, 0);
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled, String nodeId) {
        this(name, address, type, role, enabled, false, null, false, nodeId, null, 0, null, false, false, null, false, null, 0);
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled,
                                   boolean connected, String peerNodeId, boolean btlePriority,
                                   String nodeId, String packageName, int connectionRetryStrategy,
                                   List<String> allowedConfigPackages, boolean migrating,
                                   boolean dataItemSyncEnabled, ConnectionRestrictions connectionRestrictions,
                                   boolean removeConnectionWhenBondRemovedByUser,
                                   ConnectionDelayFilters connectionDelayFilters,
                                   int maxSupportedRemoteAndroidSdkVersion) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.role = role;
        this.enabled = enabled;
        this.connected = connected;
        this.peerNodeId = peerNodeId;
        this.btlePriority = btlePriority;
        this.nodeId = nodeId;
        this.packageName = packageName;
        this.connectionRetryStrategy = connectionRetryStrategy;
        this.allowedConfigPackages = allowedConfigPackages;
        this.migrating = migrating;
        this.dataItemSyncEnabled = dataItemSyncEnabled;
        this.connectionRestrictions = connectionRestrictions;
        this.removeConnectionWhenBondRemovedByUser = removeConnectionWhenBondRemovedByUser;
        this.connectionDelayFilters = connectionDelayFilters;
        this.maxSupportedRemoteAndroidSdkVersion = maxSupportedRemoteAndroidSdkVersion;
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
        sb.append(", packageName='").append(packageName).append('\'');
        sb.append(", connectionRetryStrategy='").append(connectionRetryStrategy).append('\'');
        sb.append(", allowedConfigPackages='").append(allowedConfigPackages).append('\'');
        sb.append(", migrating='").append(migrating).append('\'');
        sb.append(", dataItemSyncEnabled='").append(dataItemSyncEnabled).append('\'');
        sb.append(", connectionRestrictions='").append(connectionRestrictions).append('\'');
        sb.append(", removeConnectionWhenBondRemovedByUser='").append(removeConnectionWhenBondRemovedByUser).append('\'');
        sb.append(", maxSupportedRemoteAndroidSdkVersion='").append(maxSupportedRemoteAndroidSdkVersion).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static final Creator<ConnectionConfiguration> CREATOR = new AutoCreator<ConnectionConfiguration>(ConnectionConfiguration.class);
}
