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

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;

import java.util.ArrayList;
import java.util.List;

public class NodeClientImpl extends NodeClient {
    public NodeClientImpl(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, options);
    }

    public NodeClientImpl(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, options);
    }

    @NonNull
    @Override
    public Task<Node> getLocalNode() {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Node> completionSource) -> {
            client.getServiceInterface().getLocalNode(new BaseWearableCallbacks() {
                @Override
                public void onGetLocalNodeResponse(GetLocalNodeResponse response) {
                    WearableTasks.complete(completionSource, response.statusCode, response.node);
                }
            });
        });
    }

    @NonNull
    @Override
    public Task<List<Node>> getConnectedNodes() {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<List<Node>> completionSource) -> {
            client.getServiceInterface().getConnectedNodes(new BaseWearableCallbacks() {
                @Override
                public void onGetConnectedNodesResponse(GetConnectedNodesResponse response) {
                    List<Node> nodes = new ArrayList<>();
                    if (response.nodes != null) {
                        nodes.addAll(response.nodes);
                    }
                    WearableTasks.complete(completionSource, response.statusCode, nodes);
                }
            });
        });
    }
}
