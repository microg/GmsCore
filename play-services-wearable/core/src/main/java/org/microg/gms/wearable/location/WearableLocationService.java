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

package org.microg.gms.wearable.location;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.common.internal.ClientIdentity;
import com.google.android.gms.location.internal.LocationRequestInternal;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WearableLocationService extends WearableListenerService {
    private static final String TAG = "GmsWearLocSvc";

    public static final String PATH_LOCATION_REQUESTS = "com/google/android/location/fused/wearable/LOCATION_REQUESTS";
    public static final String PATH_CAPABILITY_QUERY = "com/google/android/location/fused/wearable/CAPABILITY_QUERY";
    public static final String PATH_CAPABILITY = "com/google/android/location/fused/wearable/CAPABILITY";

    private GoogleApiClient apiClient;
    private Map<String, Collection<LocationRequestInternal>> requestMap = new HashMap<String, Collection<LocationRequestInternal>>();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PATH_LOCATION_REQUESTS)) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
            onLocationRequests(messageEvent.getSourceNodeId(), readLocationRequestList(dataMap, this), dataMap.getBoolean("TRIGGER_UPDATE", false));
        } else if (messageEvent.getPath().equals(PATH_CAPABILITY_QUERY)) {
            onCapabilityQuery(messageEvent.getSourceNodeId());
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        onLocationRequests(peer.getId(), null, false);
    }

    public void onLocationRequests(String nodeId, Collection<LocationRequestInternal> requests, boolean triggerUpdate) {
        if (requests == null || requests.isEmpty()) {
            requestMap.remove(nodeId);
        } else {
            requestMap.put(nodeId, requests);
        }
        Log.d(TAG, "Requests: "+requestMap.entrySet());
        // TODO actually request
    }

    public void onCapabilityQuery(String nodeId) {
        Wearable.MessageApi.sendMessage(getApiClient(), nodeId, PATH_CAPABILITY, writeLocationCapability(new DataMap(), true).toByteArray());
    }

    private GoogleApiClient getApiClient() {
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        }
        if (!apiClient.isConnected()) {
            apiClient.connect();
        }
        return apiClient;
    }

    public static DataMap writeLocationCapability(DataMap dataMap, boolean locationCapable) {
        dataMap.putBoolean("CAPABILITY_LOCATION", locationCapable);
        return dataMap;
    }

    public static Collection<LocationRequestInternal> readLocationRequestList(DataMap dataMap, Context context) {
        if (!dataMap.containsKey("REQUEST_LIST")) {
            Log.w(TAG, "malformed DataMap: missing key REQUEST_LIST");
            return Collections.emptyList();
        }
        List<DataMap> requestMapList = dataMap.getDataMapArrayList("REQUEST_LIST");
        List<LocationRequestInternal> locationRequests = new ArrayList<LocationRequestInternal>();
        for (DataMap map : requestMapList) {
            locationRequests.add(readLocationRequest(map, context));
        }
        return locationRequests;
    }

    private static LocationRequestInternal readLocationRequest(DataMap dataMap, Context context) {
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
        /*try {
            return new ClientIdentity(context.getPackageManager().getApplicationInfo(packageName, 0).uid, packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unknown client identity: " + packageName, e);
            return new ClientIdentity(context.getApplicationInfo().uid, context.getPackageName());
        }*/
    }
}
