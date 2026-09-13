/*
 * Copyright (C) 2017 microG Project Team
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

package org.microg.gms.people;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ContactSyncService extends Service {
    private static final String TAG = "GmsContactSync";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return (new AbstractThreadedSyncAdapter(this, true) {
            @Override
            public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
                if (account == null) return;
                Log.d(TAG, "onPerformSync for account: " + account.name + " authority: " + authority);
                try {
                    // Gracefully complete sync without errors to prevent SyncManager infinite retry loops
                    syncResult.stats.numInserts = 0;
                    syncResult.stats.numUpdates = 0;
                    syncResult.stats.numDeletes = 0;
                } catch (Exception e) {
                    Log.w(TAG, "Error in onPerformSync", e);
                }
            }
        }).getSyncAdapterBinder();
    }
}
