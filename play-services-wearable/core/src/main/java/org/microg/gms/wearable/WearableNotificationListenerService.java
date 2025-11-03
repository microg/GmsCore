/*
 * Copyright (C) 2023 microG Project Team
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

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.google.gson.Gson;

public class WearableNotificationListenerService extends NotificationListenerService {

    private WearableImpl wearable;
    private Gson gson;

    @Override
    public void onCreate() {
        super.onCreate();
        wearable = new WearableImpl(getApplicationContext(), new NodeDatabaseHelper(getApplicationContext()), new ConfigurationDatabaseHelper(getApplicationContext()));
        gson = new Gson();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (wearable != null) {
            Notification notification = sbn.getNotification();
            String packageName = sbn.getPackageName();
            int id = sbn.getId();
            String tag = sbn.getTag();

            NotificationHolder holder = new NotificationHolder(packageName, id, tag, notification);
            String json = gson.toJson(holder);

            for (String nodeId : wearable.getConnectedNodesParcelableList().stream().map(node -> node.getId()).toArray(String[]::new)) {
                wearable.sendMessage(packageName, nodeId, "/notification", json.getBytes());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (wearable != null) {
            String packageName = sbn.getPackageName();
            int id = sbn.getId();
            String tag = sbn.getTag();
            String notificationId = packageName + "|" + id + "|" + tag;

            for (String nodeId : wearable.getConnectedNodesParcelableList().stream().map(node -> node.getId()).toArray(String[]::new)) {
                wearable.sendMessage(packageName, nodeId, "/notification_removed", notificationId.getBytes());
            }
        }
    }
}
