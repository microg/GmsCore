/*
 * Copyright 2013-2016 microG Project Team
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.internal.ClientIdentity;
import com.google.android.gms.location.internal.LocationRequestInternal;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WearableLocationListener extends IWearableListener.Stub {
    public static final String LOCATION_REQUESTS = "com/google/android/location/fused/wearable/LOCATION_REQUESTS";
    public static final String CAPABILITY_QUERY = "com/google/android/location/fused/wearable/CAPABILITY_QUERY";

    private static final String TAG = "GmsWearLocListener";

    private WearableLocationService locationService;

    public WearableLocationListener(WearableLocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public void onDataChanged(DataHolder data) throws RemoteException {
    }

    @Override
    public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
        if (messageEvent.getPath().equals(LOCATION_REQUESTS)) {
            //DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
            //locationService.onLocationRequests(messageEvent.getSourceNodeId(), parseLocationRequestList(dataMap, locationService), dataMap.getBoolean("TRIGGER_UPDATE", false));
        } else if (messageEvent.getPath().equals(CAPABILITY_QUERY)) {
            locationService.onCapabilityQuery(messageEvent.getSourceNodeId());
        }
    }

    @Override
    public void onPeerConnected(NodeParcelable node) throws RemoteException {
    }

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
        locationService.onLocationRequests(node.getId(), Collections.<LocationRequestInternal>emptyList(), false);
    }

    @Override
    public void onConnectedNodes(List<NodeParcelable> nodes) throws RemoteException {
    }

    @Override
    public void onNotificationReceived(AncsNotificationParcelable notification) throws RemoteException {
    }

    @Override
    public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {
    }

    @Override
    public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {
    }

    @Override
    public void onEntityUpdate(AmsEntityUpdateParcelable update) throws RemoteException {
    }

    /*public static Collection<LocationRequestInternal> parseLocationRequestList(DataMap dataMap, Context context) {
        if (!dataMap.containsKey("REQUEST_LIST")) {
            Log.w(TAG, "malformed DataMap: missing key REQUEST_LIST");
            return Collections.emptyList();
        }
        List<DataMap> requestMapList = dataMap.getDataMapArrayList("REQUEST_LIST");
        List<LocationRequestInternal> locationRequests = new ArrayList<LocationRequestInternal>();
        for (DataMap map : requestMapList) {
            locationRequests.add(parseLocationRequest(map, context));
        }
        return locationRequests;
    }

    private static LocationRequestInternal parseLocationRequest(DataMap dataMap, Context context) {
        LocationRequestInternal request = new LocationRequestInternal();
        request.triggerUpdate = true;
        request.request = new LocationRequest();
        request.clients = Collections.emptyList();

        if (dataMap.containsKey("PRIORITY"))
            request.request.setPriority(dataMap.getInt("PRIORITY", 0));
        if (dataMap.containsKey("INTERVAL_MS"))
            request.request.setInterval(dataMap.getLong("INTERVAL_MS", 0));
        if (dataMap.containsKey("FASTEST_INTERVAL_MS"))
            request.request.setFastestInterval(dataMap.getLong("FASTEST_INTERVAL_MS", 0));
        //if (dataMap.containsKey("MAX_WAIT_TIME_MS"))
        if (dataMap.containsKey("SMALLEST_DISPLACEMENT_METERS"))
            request.request.setSmallestDisplacement(dataMap.getFloat("SMALLEST_DISPLACEMENT_METERS", 0));
        if (dataMap.containsKey("NUM_UPDATES"))
            request.request.setNumUpdates(dataMap.getInt("NUM_UPDATES", 0));
        if (dataMap.containsKey("EXPIRATION_DURATION_MS"))
            request.request.setExpirationDuration(dataMap.getLong("EXPIRATION_DURATION_MS", 0));
        if (dataMap.containsKey("TAG"))
            request.tag = dataMap.getString("TAG");
        if (dataMap.containsKey("CLIENTS_PACKAGE_ARRAY")) {
            String[] packages = dataMap.getStringArray("CLIENTS_PACKAGE_ARRAY");
            if (packages != null) {
                request.clients = new ArrayList<ClientIdentity>();
                for (String packageName : packages) {
                    request.clients.add(generateClientIdentity(packageName, context));
                }
            }
        }

        return request;
    }

    private static ClientIdentity generateClientIdentity(String packageName, Context context) {
        return null;
        try {
            return new ClientIdentity(context.getPackageManager().getApplicationInfo(packageName, 0).uid, packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unknown client identity: " + packageName, e);
            return new ClientIdentity(context.getApplicationInfo().uid, context.getPackageName());
        }
    }*/
}
