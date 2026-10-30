/*
 * Copyright (C) 2026 microG Project Team
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

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PatternMatcher;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.AddLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse;
import com.google.android.gms.wearable.internal.GetCapabilityResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.RemoveLocalCapabilityResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CapabilityClientImpl extends CapabilityClient {
    private static final String DEFAULT_TYPE = "capability_default";

    public CapabilityClientImpl(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, options);
    }

    public CapabilityClientImpl(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, options);
    }

    @NonNull
    @Override
    public Task<CapabilityInfo> getCapability(@NonNull String capability, int nodeFilter) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<CapabilityInfo> completionSource) -> {
            client.getServiceInterface().getConnectedCapability(new BaseWearableCallbacks() {
                @Override
                public void onGetCapabilityResponse(GetCapabilityResponse response) {
                    WearableTasks.complete(completionSource, response.getStatusCode(), response.getCapabilityInfo());
                }
            }, capability, nodeFilter);
        });
    }

    @NonNull
    @Override
    public Task<Map<String, CapabilityInfo>> getAllCapabilities(int nodeFilter) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Map<String, CapabilityInfo>> completionSource) -> {
            client.getServiceInterface().getAllCapabilities(new BaseWearableCallbacks() {
                @Override
                public void onGetAllCapabilitiesResponse(GetAllCapabilitiesResponse response) {
                    java.util.Map<String, CapabilityInfo> map = new java.util.HashMap<>();
                    if (response.capabilities != null) {
                        for (CapabilityInfo info : response.capabilities) {
                            map.put(info.getName(), info);
                        }
                    }
                    WearableTasks.complete(completionSource, response.statusCode, map);
                }
            }, nodeFilter);
        });
    }

    @NonNull
    @Override
    public Task<Void> addLocalCapability(@NonNull String capability) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            client.getServiceInterface().addLocalCapability(new BaseWearableCallbacks() {
                @Override
                public void onAddLocalCapabilityResponse(AddLocalCapabilityResponse response) {
                    WearableTasks.completeVoid(completionSource, response.getStatusCode());
                }
            }, capability);
        });
    }

    @NonNull
    @Override
    public Task<Void> removeLocalCapability(@NonNull String capability) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            client.getServiceInterface().removeLocalCapability(new BaseWearableCallbacks() {
                @Override
                public void onRemoveLocalCapabilityResponse(RemoveLocalCapabilityResponse response) {
                    WearableTasks.completeVoid(completionSource, response.getStatusCode());
                }
            }, capability);
        });
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnCapabilityChangedListener listener, @NonNull String capability) {
        Uri uri = new Uri.Builder().scheme("wear").authority("*").path("/" + capability).build();
        return addListenerInternal(listener, uri, FILTER_LITERAL, capability);
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnCapabilityChangedListener listener, @NonNull Uri uri, int filterType) {
        return addListenerInternal(listener, uri, filterType, DEFAULT_TYPE);
    }

    private Task<Void> addListenerInternal(OnCapabilityChangedListener listener, Uri uri, int filterType, String subKey) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            IntentFilter filter = new IntentFilter(ACTION_CAPABILITY_CHANGED);
            if (uri != null) {
                if (uri.getScheme() != null) filter.addDataScheme(uri.getScheme());
                if (uri.getAuthority() != null) filter.addDataAuthority(uri.getAuthority(), null);
                if (uri.getPath() != null) {
                    int patternType = (filterType == FILTER_PREFIX) ? PatternMatcher.PATTERN_PREFIX : PatternMatcher.PATTERN_LITERAL;
                    filter.addDataPath(uri.getPath(), patternType);
                }
            } else {
                filter.addDataScheme("wear");
                filter.addDataAuthority("*", null);
            }
            IWearableListener stub = WearableListenerAdapter.getOrCreate(listener, "cap_" + subKey, () -> new WearableListenerAdapter() {
                @Override
                public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {
                    WearableListenerAdapter.postOnMainThread(() -> listener.onCapabilityChanged(capabilityInfo));
                }
            });
            client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                @Override
                public void onStatus(com.google.android.gms.common.api.Status status) {
                    WearableTasks.completeVoid(completionSource, status.getStatusCode());
                }
            }, new AddListenerRequest(stub, new IntentFilter[]{filter}, null));
        });
    }

    @NonNull
    @Override
    public Task<Boolean> removeListener(@NonNull OnCapabilityChangedListener listener) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Boolean> completionSource) -> {
            List<IWearableListener> stubs = WearableListenerAdapter.removeAllForListener(listener);
            if (stubs.isEmpty()) {
                completionSource.setResult(false);
                return;
            }
            AtomicInteger pending = new AtomicInteger(stubs.size());
            AtomicInteger failure = new AtomicInteger(0);
            for (IWearableListener stub : stubs) {
                try {
                    client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                        @Override
                        public void onStatus(com.google.android.gms.common.api.Status status) {
                            int code = status == null ? CommonStatusCodes.INTERNAL_ERROR : status.getStatusCode();
                            if (code != 0) {
                                failure.compareAndSet(0, code);
                            }
                            if (pending.decrementAndGet() == 0) {
                                WearableTasks.complete(completionSource, failure.get(), true);
                            }
                        }
                    }, new RemoveListenerRequest(stub));
                } catch (RemoteException remoteException) {
                    failure.compareAndSet(0, CommonStatusCodes.INTERNAL_ERROR);
                    if (pending.decrementAndGet() == 0) {
                        WearableTasks.complete(completionSource, failure.get(), true);
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public Task<Boolean> removeListener(@NonNull OnCapabilityChangedListener listener, @NonNull String capability) {
        return removeListenerInternal(listener, capability);
    }

    private Task<Boolean> removeListenerInternal(OnCapabilityChangedListener listener, String subKey) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Boolean> completionSource) -> {
            IWearableListener stub = WearableListenerAdapter.remove(listener, "cap_" + subKey);
            if (stub == null) {
                completionSource.setResult(false);
                return;
            }
            client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                @Override
                public void onStatus(com.google.android.gms.common.api.Status status) {
                    WearableTasks.complete(completionSource, status.getStatusCode(), true);
                }
            }, new RemoveListenerRequest(stub));
        });
    }
}
