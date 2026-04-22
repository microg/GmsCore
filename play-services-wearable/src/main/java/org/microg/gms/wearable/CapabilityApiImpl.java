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

package org.microg.gms.wearable;

import android.net.Uri;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.AddLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse;
import com.google.android.gms.wearable.internal.GetCapabilityResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.RemoveLocalCapabilityResponse;

import org.microg.gms.common.GmsConnector;

public class CapabilityApiImpl implements CapabilityApi {

    @Override
    public PendingResult<Status> addCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability) {
        Uri uri = Uri.parse("wear://* /" + capability);
        return addListener(client, listener, uri, FILTER_LITERAL);
    }

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, CapabilityListener listener, Uri uri, @CapabilityFilterType int filterType) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                AddListenerRequest request = new AddListenerRequest(listener, null);
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<AddLocalCapabilityResult> addLocalCapability(GoogleApiClient client, final String capability) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, AddLocalCapabilityResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<AddLocalCapabilityResult> resultProvider) throws RemoteException {
                client.getServiceInterface().addLocalCapability(new BaseWearableCallbacks() {
                    @Override
                    public void onAddLocalCapabilityResponse(AddLocalCapabilityResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new AddLocalCapabilityResultImpl(response));
                    }
                }, capability);
            }
        });
    }

    @Override
    public PendingResult<GetAllCapabilitiesResult> getAllCapabilities(GoogleApiClient client, @NodeFilterType int nodeFilter) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetAllCapabilitiesResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<GetAllCapabilitiesResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getAllCapabilities(new BaseWearableCallbacks() {
                    @Override
                    public void onGetAllCapabilitiesResponse(GetAllCapabilitiesResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetAllCapabilitiesResultImpl(response));
                    }
                }, nodeFilter);
            }
        });
    }

    @Override
    public PendingResult<GetCapabilityResult> getCapability(GoogleApiClient client, String capability, @NodeFilterType int nodeFilter) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetCapabilityResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<GetCapabilityResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getConnectedCapability(new BaseWearableCallbacks() {
                    @Override
                    public void onGetCapabilityResponse(GetCapabilityResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetCapabilityResultImpl(response));
                    }
                }, capability, nodeFilter);
            }
        });
    }

    @Override
    public PendingResult<Status> removeCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability) {
        return removeListener(client, listener);
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, CapabilityListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                RemoveListenerRequest request = new RemoveListenerRequest(listener);
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<RemoveLocalCapabilityResult> removeLocalCapability(GoogleApiClient client, final String capability) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, RemoveLocalCapabilityResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<RemoveLocalCapabilityResult> resultProvider) throws RemoteException {
                client.getServiceInterface().removeLocalCapability(new BaseWearableCallbacks() {
                    @Override
                    public void onRemoveLocalCapabilityResponse(RemoveLocalCapabilityResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new RemoveLocalCapabilityResultImpl(response));
                    }
                }, capability);
            }
        });
    }

    public static class AddLocalCapabilityResultImpl implements AddLocalCapabilityResult {
        private final AddLocalCapabilityResponse response;

        public AddLocalCapabilityResultImpl(AddLocalCapabilityResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }

    public static class GetAllCapabilitiesResultImpl implements GetAllCapabilitiesResult {
        private final GetAllCapabilitiesResponse response;

        public GetAllCapabilitiesResultImpl(GetAllCapabilitiesResponse response) {
            this.response = response;
        }

        @Override
        public java.util.Map<String, com.google.android.gms.wearable.CapabilityInfo> getAllCapabilities() {
            java.util.HashMap<String, com.google.android.gms.wearable.CapabilityInfo> result = new java.util.HashMap<>();
            if (response.capabilities != null) {
                for (CapabilityInfoParcelable cap : response.capabilities) {
                    result.put(cap.getName(), cap);
                }
            }
            return result;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }

    public static class GetCapabilityResultImpl implements GetCapabilityResult {
        private final GetCapabilityResponse response;

        public GetCapabilityResultImpl(GetCapabilityResponse response) {
            this.response = response;
        }

        @Override
        public com.google.android.gms.wearable.CapabilityInfo getCapability() {
            return response.capabilityInfo;
        }

        @Override
        public Status getStatus() {
            return new Status(response.status);
        }
    }

    public static class RemoveLocalCapabilityResultImpl implements RemoveLocalCapabilityResult {
        private final RemoveLocalCapabilityResponse response;

        public RemoveLocalCapabilityResultImpl(RemoveLocalCapabilityResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}