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

package org.microg.gms.clearcut;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.clearcut.LogEventParcelable;
import com.google.android.gms.clearcut.internal.IClearcutLoggerCallbacks;
import com.google.android.gms.clearcut.internal.IClearcutLoggerService;
import com.google.android.gms.common.api.Status;

public class ClearcutLoggerServiceImpl extends IClearcutLoggerService.Stub {
    private static final String TAG = "GmsClearcutLogSvcImpl";

    @Override
    public void log(IClearcutLoggerCallbacks callbacks, LogEventParcelable event) throws RemoteException {
        // These logs are not really helpful for us, so let's just ignore it.
        try {
            callbacks.onStatus(Status.SUCCESS);
        } catch (Exception ignored) {
        }
    }
}
