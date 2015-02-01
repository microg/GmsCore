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

package org.microg.gms.auth;

import android.content.Context;
import android.os.Build;

import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

import java.util.Locale;

public class GmsAddAccountRequest extends AuthRequest {

    public GmsAddAccountRequest(Context context, String token) {
        this(Locale.getDefault(), Build.VERSION.SDK_INT, Constants.MAX_REFERENCE_VERSION,
                Build.DEVICE, Build.ID, Utils.getAndroidIdHex(context), token);
    }

    public GmsAddAccountRequest(Locale locale, int sdkVersion, int gmsVersion, String deviceName,
                                String buildVersion, String androidIdHex, String token) {
        this.service = "ac2dm";
        this.addAccount = true;
        this.isSystemPartition = true;
        this.hasPermission = true;
        this.getAccountId = true;
        this.app = "com.google.android.gms";
        this.appSignature = "38918a453d07199354f8b19af05ec6562ced5788";

        this.androidIdHex = androidIdHex;
        this.deviceName = deviceName;
        this.buildVersion = buildVersion;
        this.countryCode = locale.getCountry();
        this.gmsVersion = gmsVersion;
        this.operatorCountryCode = locale.getCountry();
        this.locale = locale.toString();
        this.sdkVersion = sdkVersion;
        this.token = token;
    }
}
