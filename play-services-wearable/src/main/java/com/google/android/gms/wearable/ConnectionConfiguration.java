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

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;
import java.util.List;

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
    @SafeParceled(11)
    public String packageName;
    @SafeParceled(12)
    public int connectionRetryStrategy;
    @SafeParceled(value = 13, subClass = String.class)
    public List<String> allowedConfigPackages;
    @SafeParceled(14)
    public boolean migrating;
    @SafeParceled(15)
    public boolean dataItemSyncEnabled = true;
    @SafeParceled(17)
    public boolean removeConnectionWhenBondRemovedByUser = true;
    @SafeParceled(19)
    public int maxSupportedRemoteAndroidSdkVersion;
    @SafeParceled(20)
    public int runtimeType;
    @SafeParceled(21)
    public boolean skipConnectingIfNotBonded;

    // Retain the presence of unsupported policies instead of silently dropping them.
    public boolean hasUnsupportedConnectionPolicies;

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

    public static final Creator<ConnectionConfiguration> CREATOR = new AutoCreator<ConnectionConfiguration>(ConnectionConfiguration.class) {
        @Override
        public ConnectionConfiguration createFromParcel(Parcel source) {
            int start = source.dataPosition();
            ConnectionConfiguration result = super.createFromParcel(source);
            int end = source.dataPosition();
            source.setDataPosition(start);
            int objectEnd = SafeParcelReader.readObjectHeader(source);
            while (source.dataPosition() < objectEnd) {
                int header = SafeParcelReader.readHeader(source);
                int field = SafeParcelReader.getFieldId(header);
                int size = (header & 0xffff0000) == 0xffff0000 ? source.readInt() : (header >>> 16);
                int next = source.dataPosition() + size;
                if (size < 0 || next < source.dataPosition() || next > objectEnd) {
                    throw new IllegalArgumentException("Invalid connection configuration field");
                }
                if ((field == 16 || field == 18) && size != 0) result.hasUnsupportedConnectionPolicies = true;
                source.setDataPosition(next);
            }
            source.setDataPosition(end);
            return result;
        }
    };
}
