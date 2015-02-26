/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.checkin;

import android.content.ContentResolver;
import android.content.Context;

import org.microg.gms.common.Utils;
import org.microg.gms.gservices.GServices;

import java.io.IOException;

public class CheckinManager {
    private static final long MIN_CHECKIN_INTERVAL = 3 * 60 * 60 * 1000; // 3 hours

    public static synchronized LastCheckinInfo checkin(Context context) throws IOException {
        LastCheckinInfo info = LastCheckinInfo.read(context);
        if (info.lastCheckin > System.currentTimeMillis() - MIN_CHECKIN_INTERVAL) return null;
        CheckinRequest request = CheckinClient.makeRequest(Utils.getBuild(context), null, null, null, info); // TODO
        return handleResponse(context, CheckinClient.request(request));
    }

    private static LastCheckinInfo handleResponse(Context context, CheckinResponse response) {
        LastCheckinInfo info = new LastCheckinInfo();
        info.androidId = response.androidId;
        info.lastCheckin = response.timeMs;
        info.securityToken = response.securityToken;
        info.digest = response.digest;
        info.write(context);

        ContentResolver resolver = context.getContentResolver();
        for (CheckinResponse.GservicesSetting setting : response.setting) {
            GServices.setString(resolver, setting.name.utf8(), setting.value.utf8());
        }

        return info;
    }
}
