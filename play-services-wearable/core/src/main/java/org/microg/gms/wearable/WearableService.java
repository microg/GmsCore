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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.wearable.core.R;

public class WearableService extends BaseService {

    private WearableImpl wearable;

    // All what i found
    // for now, just to not spam outdated GMS at my companion
    public static final Feature[] FEATURES = new Feature[]{
            new Feature("app_client", 4L),
            new Feature("carrier_auth", 1L),
            new Feature("wear3_oem_companion", 1L),
            new Feature("wear_await_data_sync_complete", 1L),
            new Feature("wear_backup_restore", 6L),
            new Feature("wear_consent", 2L),
            new Feature("wear_consent_recordoptin", 1L),
            new Feature("wear_consent_recordoptin_swaadl", 1L),
            new Feature("wear_consent_supervised", 2L),
            new Feature("wear_get_phone_switching_feature_status", 1L),
            new Feature("wear_fast_pair_account_key_sync", 1L),
            new Feature("wear_fast_pair_get_account_keys", 1L),
            new Feature("wear_fast_pair_get_account_key_by_account", 1L),
            new Feature("wear_flush_batched_data", 1L),
            new Feature("wear_get_related_configs", 1L),
            new Feature("wear_get_node_id", 1L),
            new Feature("wear_logging_service", 2L),
            new Feature("wear_retry_connection", 1L),
            new Feature("wear_set_cloud_sync_setting_by_node", 1L),
            new Feature("wear_first_party_consents", 2L),
            new Feature("wear_update_config", 1L),
            new Feature("wear_update_connection_retry_strategy", 1L),
            new Feature("wear_update_delay_config", 1L),
            new Feature("wearable_services", 1L),
            new Feature("wear_cancel_migration", 1L),
            new Feature("wear_customizable_screens", 2L),
            new Feature("wear_wifi_immediate_connect", 1L),
            new Feature("wear_get_node_active_network_metered", 1L),
            new Feature("wear_consents_per_watch", 3L)
    };

    private boolean isForeground = false;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "wearable_service";

    public WearableService() {
        super("GmsWearSvc", GmsService.WEAR);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        ConfigurationDatabaseHelper configurationDatabaseHelper = new ConfigurationDatabaseHelper(getApplicationContext());
        NodeDatabaseHelper nodeDatabaseHelper = new NodeDatabaseHelper(getApplicationContext());
        wearable = new WearableImpl(getApplicationContext(), nodeDatabaseHelper, configurationDatabaseHelper);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wearable Connection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    public void setConnectionActive(boolean active) {
        if (active && !isForeground) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(org.microg.gms.base.core.R.drawable.ic_radio_checked)  // or whatever icon
                    .setContentTitle("Connected to watch")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
            isForeground = true;
            Log.d(TAG, "Started foreground service for active connection");
        } else if (!active && isForeground) {
            stopForeground(true);
            isForeground = false;
            Log.d(TAG, "Stopped foreground service");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wearable.stop();
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FEATURES;
        callback.onPostInitCompleteWithConnectionInfo(0, new WearableServiceImpl(this, wearable, request.packageName), connectionInfo);

    }
}
