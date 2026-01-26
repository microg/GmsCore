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

import android.net.Uri;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse;
import com.google.android.gms.wearable.internal.GetCapabilityResponse;
import com.google.android.gms.wearable.internal.RemoveLocalCapabilityResponse;

import org.microg.gms.common.GmsConnector;

import java.util.Map;

public class CapabilityApiImpl implements CapabilityApi {
    @Override
    public PendingResult<Status> addCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, CapabilityListener listener, Uri uri, int filterType) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<AddLocalCapabilityResult> addLocalCapability(GoogleApiClient client, final String capability) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().addLocalCapability(new BaseWearableCallbacks() {
                @Override
                public void onAddLocalCapabilityResponse(AddLocalCapabilityResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new AddLocalCapabilityResultImpl(new Status(response.statusCode)));
                }
            }, capability);
        });
    }

    @Override
    public PendingResult<GetAllCapabilitiesResult> getAllCapabilities(GoogleApiClient client, final int nodeFilter) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getAllCapabilities(new BaseWearableCallbacks() {
                @Override
                public void onGetAllCapabilitiesResponse(GetAllCapabilitiesResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetAllCapabilitiesResultImpl(new Status(response.statusCode), (Map<String, CapabilityInfo>) (Object) response.capabilities));
                }
            }, nodeFilter);
        });
    }

    @Override
    public PendingResult<GetCapabilityResult> getCapability(GoogleApiClient client, final String capability, final int nodeFilter) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getConnectedCapability(new BaseWearableCallbacks() {
                @Override
                public void onGetCapabilityResponse(GetCapabilityResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetCapabilityResultImpl(new Status(response.statusCode), response.capability));
                }
            }, capability, nodeFilter);
        });
    }

    @Override
    public PendingResult<Status> removeCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, CapabilityListener listener) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<RemoveLocalCapabilityResult> removeLocalCapability(GoogleApiClient client, final String capability) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().removeLocalCapability(new BaseWearableCallbacks() {
                @Override
                public void onRemoveLocalCapabilityResponse(RemoveLocalCapabilityResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new RemoveLocalCapabilityResultImpl(new Status(response.statusCode)));
                }
            }, capability);
        });
    }

    private static class AddLocalCapabilityResultImpl implements AddLocalCapabilityResult {
        private final Status status;

        private AddLocalCapabilityResultImpl(Status status) {
            this.status = status;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class RemoveLocalCapabilityResultImpl implements RemoveLocalCapabilityResult {
        private final Status status;

        private RemoveLocalCapabilityResultImpl(Status status) {
            this.status = status;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class GetCapabilityResultImpl implements GetCapabilityResult {
        private final Status status;
        private final CapabilityInfo capabilityInfo;

        private GetCapabilityResultImpl(Status status, CapabilityInfo capabilityInfo) {
            this.status = status;
            this.capabilityInfo = capabilityInfo;
        }

        @Override
        public CapabilityInfo getCapability() {
            return capabilityInfo;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class GetAllCapabilitiesResultImpl implements GetAllCapabilitiesResult {
        private final Status status;
        private final Map<String, CapabilityInfo> capabilities;

        private GetAllCapabilitiesResultImpl(Status status, Map<String, CapabilityInfo> capabilities) {
            this.status = status;
            this.capabilities = capabilities;
        }

        @Override
        public Map<String, CapabilityInfo> getAllCapabilities() {
            return capabilities;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }
}
