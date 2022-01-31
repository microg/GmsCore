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

package org.microg.gms.playlog;

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.clearcut.internal.PlayLoggerContext;
import com.google.android.gms.playlog.internal.IPlayLogService;
import com.google.android.gms.playlog.internal.LogEvent;

import java.util.List;

@Deprecated
public class PlayLogServiceImpl extends IPlayLogService.Stub {
    private static final String TAG = "GmsPlayLogSvcImpl";

    @Override
    public void onEvent(String packageName, PlayLoggerContext context, LogEvent event) throws RemoteException {
        Log.d(TAG, "onEvent: context[packageName]:" + context.packageName + " event[tag]:" + event.tag);
    }

    @Override
    public void onMultiEvent(String packageName, PlayLoggerContext context, List<LogEvent> events) throws RemoteException {
        for (LogEvent event : events) {
            onEvent(packageName, context, event);
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
