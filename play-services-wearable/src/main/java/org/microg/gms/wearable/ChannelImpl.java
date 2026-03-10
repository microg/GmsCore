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
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.internal.ChannelParcelable;

public class ChannelImpl extends ChannelParcelable implements Channel {
    private static final String TAG = "GmsWearChannelImpl";

    public ChannelImpl(String token, String nodeId, String path) {
        super(token, nodeId, path);
    }

    public ChannelImpl(ChannelParcelable wrapped) {
        this(wrapped.token, wrapped.nodeId, wrapped.path);
    }


    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        Log.d(TAG, "unimplemented Method: addListener");
        return null;
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client, int errorCode) {
        Log.d(TAG, "unimplemented Method: close");
        return null;
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client) {
        Log.d(TAG, "unimplemented Method: close");
        return null;
    }

    @Override
    public PendingResult<GetInputStreamResult> getInputStream(GoogleApiClient client) {
        Log.d(TAG, "unimplemented Method: getInputStream");
        return null;
    }

    @Override
    public PendingResult<GetOutputStreamResult> getOutputStream(GoogleApiClient client) {
        Log.d(TAG, "unimplemented Method: getOutputStream");
        return null;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getToken() {
        return token;
    }

    @Override
    public PendingResult<Status> receiveFile(GoogleApiClient client, Uri uri, boolean append) {
        Log.d(TAG, "unimplemented Method: receiveFile");
        return null;
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        Log.d(TAG, "unimplemented Method: removeListener");
        return null;
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri) {
        Log.d(TAG, "unimplemented Method: sendFile");
        return null;
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length) {
        Log.d(TAG, "unimplemented Method: sendFile");
        return null;
    }
}
