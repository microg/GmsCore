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
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;
import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.OpenChannelResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import java.io.InputStream;
import java.io.OutputStream;

public class ChannelClientImpl extends ChannelClient {
    private static final String LISTENER_TYPE = "channel";

    public static final int EVENT_CHANNEL_OPENED = 1;
    public static final int EVENT_CHANNEL_CLOSED = 2;
    public static final int EVENT_INPUT_CLOSED = 3;
    public static final int EVENT_OUTPUT_CLOSED = 4;

    public ChannelClientImpl(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, options);
    }

    public ChannelClientImpl(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, options);
    }

    private static Channel toChannel(ChannelParcelable p, String fallbackNodeId, String fallbackPath) {
        if (p != null) {
            return new ChannelImpl(p.token, p.nodeId, p.path);
        }
        return new ChannelImpl("chan_" + System.currentTimeMillis(), fallbackNodeId, fallbackPath);
    }

    static Channel channelForOpen(ChannelParcelable channel, int statusCode, String nodeId, String path) {
        if (channel == null && statusCode != 0) {
            return null;
        }
        return toChannel(channel, nodeId, path);
    }

    static void closeOnError(int statusCode, ErrorCloseable closeable) {
        if (statusCode != 0 && closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    static void closeOnError(int statusCode, ParcelFileDescriptor pfd) {
        if (pfd == null) return;
        closeOnError(statusCode, (ErrorCloseable) pfd::close);
    }

    interface ErrorCloseable {
        void close() throws Exception;
    }

    private static String getToken(Channel channel) {
        if (channel instanceof ChannelImpl) {
            return ((ChannelImpl) channel).getToken();
        }
        return channel.getNodeId() + "_" + channel.getPath();
    }

    @NonNull
    @Override
    public Task<Channel> openChannel(@NonNull String nodeId, @NonNull String path) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Channel> completionSource) -> {
            client.getServiceInterface().openChannel(new BaseWearableCallbacks() {
                @Override
                public void onOpenChannelResponse(OpenChannelResponse response) {
                    Channel chan = channelForOpen(response.channel, response.statusCode, nodeId, path);
                    WearableTasks.complete(completionSource, response.statusCode, chan);
                }
            }, nodeId, path);
        });
    }

    @NonNull
    @Override
    public Task<Void> close(@NonNull Channel channel) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            client.getServiceInterface().closeChannel(new BaseWearableCallbacks() {
                @Override
                public void onCloseChannelResponse(CloseChannelResponse response) {
                    WearableTasks.completeVoid(completionSource, response.statusCode);
                }
            }, getToken(channel));
        });
    }

    @NonNull
    @Override
    public Task<Void> close(@NonNull Channel channel, int errorCode) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            client.getServiceInterface().closeChannelWithError(new BaseWearableCallbacks() {
                @Override
                public void onCloseChannelResponse(CloseChannelResponse response) {
                    WearableTasks.completeVoid(completionSource, response.statusCode);
                }
            }, getToken(channel), errorCode);
        });
    }

    @NonNull
    @Override
    public Task<InputStream> getInputStream(@NonNull Channel channel) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<InputStream> completionSource) -> {
            client.getServiceInterface().getChannelInputStream(new BaseWearableCallbacks() {
                @Override
                public void onGetChannelInputStreamResponse(GetChannelInputStreamResponse response) {
                    if (response.statusCode != 0) {
                        closeOnError(response.statusCode, response.pfd);
                        WearableTasks.complete(completionSource, response.statusCode, null);
                        return;
                    }
                    if (response.pfd == null) {
                        WearableTasks.complete(completionSource, CommonStatusCodes.ERROR, null);
                        return;
                    }
                    InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(response.pfd);
                    WearableTasks.complete(completionSource, response.statusCode, is);
                }
            }, new IChannelStreamCallbacks.Stub() {}, getToken(channel));
        });
    }

    @NonNull
    @Override
    public Task<OutputStream> getOutputStream(@NonNull Channel channel) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<OutputStream> completionSource) -> {
            client.getServiceInterface().getChannelOutputStream(new BaseWearableCallbacks() {
                @Override
                public void onGetChannelOutputStreamResponse(GetChannelOutputStreamResponse response) {
                    if (response.statusCode != 0) {
                        closeOnError(response.statusCode, response.pfd);
                        WearableTasks.complete(completionSource, response.statusCode, null);
                        return;
                    }
                    if (response.pfd == null) {
                        WearableTasks.complete(completionSource, CommonStatusCodes.ERROR, null);
                        return;
                    }
                    OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(response.pfd);
                    WearableTasks.complete(completionSource, response.statusCode, os);
                }
            }, new IChannelStreamCallbacks.Stub() {}, getToken(channel));
        });
    }

    @NonNull
    @Override
    public Task<Void> registerChannelCallback(@NonNull ChannelCallback callback) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            IntentFilter filter = new IntentFilter(ACTION_CHANNEL_EVENT);
            filter.addDataScheme("wear");
            filter.addDataAuthority("*", null);

            IWearableListener stub = WearableListenerAdapter.getOrCreate(callback, LISTENER_TYPE, () -> new WearableListenerAdapter() {
                @Override
                public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {
                    WearableListenerAdapter.postOnMainThread(() -> {
                        if (channelEvent == null || channelEvent.channel == null) return;
                        Channel chan = toChannel(channelEvent.channel, "unknown", "");
                        switch (channelEvent.eventType) {
                            case EVENT_CHANNEL_OPENED:
                                callback.onChannelOpened(chan);
                                break;
                            case EVENT_CHANNEL_CLOSED:
                                callback.onChannelClosed(chan, channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                                break;
                            case EVENT_INPUT_CLOSED:
                                callback.onInputClosed(chan, channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                                break;
                            case EVENT_OUTPUT_CLOSED:
                                callback.onOutputClosed(chan, channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                                break;
                        }
                    });
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
    public Task<Boolean> unregisterChannelCallback(@NonNull ChannelCallback callback) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Boolean> completionSource) -> {
            IWearableListener stub = WearableListenerAdapter.remove(callback, LISTENER_TYPE);
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
