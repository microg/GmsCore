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

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class ClockworkNodePreferences {

    private static final String CLOCKWORK_NODE_PREFERENCES = "cw_node";
    private static final String CLOCKWORK_NODE_PREFERENCE_NODE_ID = "node_id";
    private static final String CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK = "nextSeqIdBlock";

    private static final Object lock = new Object();
    private static long seqIdBlock;
    private static long seqIdInBlock = -1;

    private Context context;

    public ClockworkNodePreferences(Context context) {
        this.context = context;
    }

    public String getLocalNodeId() {
        SharedPreferences preferences = context.getSharedPreferences(CLOCKWORK_NODE_PREFERENCES, Context.MODE_PRIVATE);
        String nodeId = preferences.getString(CLOCKWORK_NODE_PREFERENCE_NODE_ID, null);
        if (nodeId == null) {
            nodeId = UUID.randomUUID().toString();
            preferences.edit().putString(CLOCKWORK_NODE_PREFERENCE_NODE_ID, nodeId).apply();
        }
        return nodeId;
    }

    public long getNextSeqId() {
        synchronized (lock) {
            SharedPreferences preferences = context.getSharedPreferences(CLOCKWORK_NODE_PREFERENCES, Context.MODE_PRIVATE);
            if (seqIdInBlock < 0) seqIdInBlock = 1000;
            if (seqIdInBlock >= 1000) {
                seqIdBlock = preferences.getLong(CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK, 100);
                preferences.edit().putLong(CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK, seqIdBlock + seqIdInBlock).apply();
                seqIdInBlock = 0;
            }
            return seqIdBlock + seqIdInBlock++;
        }
    }
}
