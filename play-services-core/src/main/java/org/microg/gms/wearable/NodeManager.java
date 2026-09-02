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

package org.microg.gms.wearable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.google.android.gms.wearable.internal.NodeParcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NodeManager {
    private static final String TAG = "WearableNodeManager";
    private static String localNodeId;

    public static synchronized NodeParcelable getLocalNode(Context context) {
        if (localNodeId == null) {
            localNodeId = UUID.randomUUID().toString();
        }
        String name = Build.MODEL != null ? Build.MODEL : "Android Phone";
        return new NodeParcelable(localNodeId, name, true);
    }

    public static List<NodeParcelable> getConnectedNodes(Context context) {
        List<NodeParcelable> connectedNodes = new ArrayList<>();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                if (bondedDevices != null) {
                    for (BluetoothDevice device : bondedDevices) {
                        int deviceClass = device.getBluetoothClass() != null ? device.getBluetoothClass().getMajorDeviceClass() : 0;
                        if (deviceClass == 0x0700 || (device.getName() != null && (device.getName().toLowerCase().contains("watch") || device.getName().toLowerCase().contains("wear")))) {
                            connectedNodes.add(new NodeParcelable(device.getAddress(), device.getName() != null ? device.getName() : "Wear OS Watch", true));
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission not granted for node scanning", e);
        }
        return connectedNodes;
    }
}