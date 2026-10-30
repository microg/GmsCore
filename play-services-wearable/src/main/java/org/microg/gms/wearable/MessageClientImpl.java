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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.SendMessageResponse;

public class MessageClientImpl extends MessageClient {
    private static final String LISTENER_TYPE = "message";

    public MessageClientImpl(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, options);
    }

    public MessageClientImpl(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, options);
    }

    @NonNull
    @Override
    public Task<Integer> sendMessage(@NonNull String nodeId, @NonNull String path, byte[] data) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Integer> completionSource) -> {
            client.getServiceInterface().sendMessage(new BaseWearableCallbacks() {
                @Override
                public void onSendMessageResponse(SendMessageResponse response) {
                    WearableTasks.complete(completionSource, response.statusCode, response.requestId);
                }
            }, nodeId, path, data);
        });
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnMessageReceivedListener listener) {
        return addListener(listener, null, FILTER_LITERAL);
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnMessageReceivedListener listener, Uri uri, int filterType) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            IntentFilter filter = new IntentFilter(ACTION_MESSAGE_RECEIVED);
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
            IWearableListener stub = WearableListenerAdapter.getOrCreate(listener, LISTENER_TYPE, () -> new WearableListenerAdapter() {
                @Override
                public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
                    WearableListenerAdapter.postOnMainThread(() -> listener.onMessageReceived(messageEvent));
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
    public Task<Boolean> removeListener(@NonNull OnMessageReceivedListener listener) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Boolean> completionSource) -> {
            IWearableListener stub = WearableListenerAdapter.remove(listener, LISTENER_TYPE);
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
