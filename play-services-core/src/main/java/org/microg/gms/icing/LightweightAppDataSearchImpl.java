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

package org.microg.gms.icing;

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.appdatasearch.UsageInfo;
import com.google.android.gms.appdatasearch.internal.ILightweightAppDataSearch;
import com.google.android.gms.appdatasearch.internal.ILightweightAppDataSearchCallbacks;

import java.util.Arrays;

public class LightweightAppDataSearchImpl extends ILightweightAppDataSearch.Stub {
    private static final String TAG = "GmsIcingLightSearchImpl";

    public void view(ILightweightAppDataSearchCallbacks callbacks, String packageName, UsageInfo[] usageInfos) {
        Log.d(TAG, "view: " + Arrays.toString(usageInfos));
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
