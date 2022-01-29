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

package org.microg.gms.gcm;

import static org.microg.gms.common.HttpFormClient.RequestContent;
import static org.microg.gms.common.HttpFormClient.RequestContentDynamic;
import static org.microg.gms.common.HttpFormClient.RequestHeader;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.HttpFormClient;
import org.microg.gms.profile.Build;
import org.microg.gms.profile.ProfileManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterRequest extends HttpFormClient.Request {
    private static final String SERVICE_URL = "https://android.clients.google.com/c2dm/register3";
    private static final String USER_AGENT = "Android-GCM/1.5 (%s %s)";

    @RequestHeader("Authorization")
    private String auth;
    @RequestHeader("User-Agent")
    private String userAgent;

    @RequestHeader("app")
    @RequestContent("app")
    public String app;
    @RequestContent("cert")
    public String appSignature;
    @RequestContent("app_ver")
    public int appVersion;
    @RequestContent("info")
    public String info;
    @RequestContent("sender")
    public String sender;
    @RequestContent("device")
    public long androidId;
    @RequestContent("delete")
    public boolean delete;
    public long securityToken;
    public String deviceName;
    public String buildVersion;
    @RequestContent("target_ver")
    public Integer sdkVersion;
    @RequestContentDynamic
    private final Map<String, String> extraParams = new LinkedHashMap<>();

    @Override
    public void prepare() {
        userAgent = String.format(USER_AGENT, deviceName, buildVersion);
        auth = "AidLogin " + androidId + ":" + securityToken;
    }

    public RegisterRequest checkin(LastCheckinInfo lastCheckinInfo) {
        androidId = lastCheckinInfo.getAndroidId();
        securityToken = lastCheckinInfo.getSecurityToken();
        return this;
    }

    public RegisterRequest app(String app) {
        this.app = app;
        return this;
    }

    public RegisterRequest app(String app, String appSignature) {
        this.app = app;
        this.appSignature = appSignature;
        return this;
    }

    public RegisterRequest app(String app, String appSignature, int appVersion) {
        this.app = app;
        this.appSignature = appSignature;
        this.appVersion = appVersion;
        return this;
    }

    public RegisterRequest info(String info) {
        this.info = info;
        return this;
    }

    public RegisterRequest sender(String sender) {
        this.sender = sender;
        return this;
    }

    public RegisterRequest build(Context context) {
        ProfileManager.ensureInitialized(context);
        deviceName = Build.DEVICE;
        buildVersion = Build.ID;
        return this;
    }

    public RegisterRequest delete() {
        return delete(true);
    }

    public RegisterRequest delete(boolean delete) {
        this.delete = delete;
        return this;
    }

    public RegisterRequest extraParams(Bundle extraBundle) {
        for (String key : extraBundle.keySet()) {
            if (!key.equals(GcmConstants.EXTRA_SENDER) && !key.equals(GcmConstants.EXTRA_DELETE)) {
                extraParam(key, extraBundle.getString(key));
            }
        }
        return this;
    }

    public RegisterRequest extraParam(String key, String value) {
        // Ignore empty registration extras
        if (!TextUtils.isEmpty(value)) {
            extraParams.put(extraParamKey(key), value);
        }
        return this;
    }

    public boolean hasExtraParam(String key) {
        return extraParams.containsKey(extraParamKey(key));
    }

    private static String extraParamKey(String key) {
        return "X-" + key;
    }

    public RegisterResponse getResponse() throws IOException {
        return HttpFormClient.request(SERVICE_URL, this, RegisterResponse.class);
    }

    public void getResponseAsync(HttpFormClient.Callback<RegisterResponse> callback) {
        HttpFormClient.requestAsync(SERVICE_URL, this, RegisterResponse.class, callback);
    }
}
