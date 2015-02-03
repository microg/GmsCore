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

import org.microg.gms.common.Build;
import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthRequest {
    private static final String USER_AGENT = "GoogleAuth/1.4 (%s %s)";

    public String app;
    public String appSignature;
    public String caller;
    public String callerSignature;
    public String androidIdHex;
    public String deviceName;
    public String buildVersion;
    public int sdkVersion;
    public String countryCode;
    public String operatorCountryCode;
    public String locale;
    public int gmsVersion = Constants.MAX_REFERENCE_VERSION;
    public String accountType = "HOSTED_OR_GOOGLE";
    public String email;
    public String service;
    public String source = "android";
    public boolean isCalledFromAccountManager;
    public String token;
    public boolean systemPartition;
    public boolean getAccountId;
    public boolean isAccessToken;
    public String droidguardResults;
    public boolean hasPermission;
    public boolean addAccount;

    public AuthRequest() {

    }


    @Deprecated
    public AuthRequest(Context context, String token) {
        this(Utils.getLocale(context), Utils.getBuild(context), Utils.getAndroidIdHex(context), token);
    }

    @Deprecated
    public AuthRequest(Locale locale, Build build, String androidIdHex, String token) {
        this(locale, build.sdk, Constants.MAX_REFERENCE_VERSION, build.device, build.id, androidIdHex, token);
    }

    @Deprecated
    public AuthRequest(Locale locale, int sdkVersion, int gmsVersion, String deviceName,
                       String buildVersion, String androidIdHex, String token) {
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


    public Map<String, String> getHttpHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("app", app);
        map.put("device", androidIdHex);
        map.put("User-Agent", String.format(USER_AGENT, deviceName, buildVersion));
        map.put("Content-Type", "application/x-www-form-urlencoded");
        return map;
    }

    public Map<String, String> getFormContent() {
        Map<String, String> map = new HashMap<>();
        map.put("device_country", countryCode);
        map.put("operatorCountry", operatorCountryCode);
        map.put("lang", locale);
        map.put("sdk_version", Integer.toString(sdkVersion));
        map.put("google_play_services_version", Integer.toString(gmsVersion));
        map.put("accountType", accountType);
        if (systemPartition) map.put("system_partition", "1");
        if (hasPermission) map.put("has_permission", "1");
        if (addAccount) map.put("add_account", "1");
        if (email != null) map.put("Email", email);
        map.put("service", service);
        map.put("source", source);
        map.put("androidId", androidIdHex);
        if (getAccountId) map.put("get_accountid", "1");
        map.put("app", app);
        map.put("client_sig", appSignature);
        if (caller != null) {
            map.put("callerPkg", caller);
            map.put("callerSig", callerSignature);
        }
        if (isCalledFromAccountManager) {
            map.put("is_called_from_account_manager", "1");
            map.put("_opt_is_called_from_account_manager", "1");
        }
        if (isAccessToken) map.put("ACCESS_TOKEN", "1");
        map.put("Token", token);
        if (droidguardResults != null) map.put("droidguard_results", droidguardResults);
        return map;
    }

    public AuthRequest build(Build build) {
        sdkVersion = build.sdk;
        deviceName = build.device;
        buildVersion = build.id;
        return this;
    }

    public AuthRequest locale(Locale locale) {
        this.locale = locale.toString();
        this.countryCode = locale.getCountry();
        this.operatorCountryCode = locale.getCountry();
        return this;
    }

    public AuthRequest fromContext(Context context) {
        build(Utils.getBuild(context));
        locale(Utils.getLocale(context));
        androidIdHex = Utils.getAndroidIdHex(context);
        return this;
    }

    public AuthRequest email(String email) {
        this.email = email;
        return this;
    }

    public AuthRequest token(String token) {
        this.token = token;
        return this;
    }

    public AuthRequest service(String service) {
        this.service = service;
        return this;
    }

    public AuthRequest app(String app, String appSignature) {
        this.app = app;
        this.appSignature = appSignature;
        return this;
    }

    public AuthRequest appIsGms() {
        return app(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1);
    }

    public AuthRequest callerIsGms() {
        return caller(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1);
    }

    public AuthRequest callerIsApp() {
        return caller(app, appSignature);
    }

    public AuthRequest caller(String caller, String callerSignature) {
        this.caller = caller;
        this.callerSignature = callerSignature;
        return this;
    }

    public AuthRequest calledFromAccountManager() {
        isCalledFromAccountManager = true;
        return this;
    }

    public AuthRequest addAccount() {
        addAccount = true;
        return this;
    }

    public AuthRequest systemPartition() {
        systemPartition = true;
        return this;
    }

    public AuthRequest hasPermission() {
        hasPermission = true;
        return this;
    }

    public AuthRequest getAccountId() {
        getAccountId = true;
        return this;
    }

    public AuthRequest isAccessToken() {
        isAccessToken = true;
        return this;
    }

    public AuthRequest droidguardResults(String droidguardResults) {
        this.droidguardResults = droidguardResults;
        return this;
    }

    public AuthResponse getResponse() throws IOException {
        return AuthClient.request(this);
    }

    public void getResponseAsync(AuthClient.GmsAuthCallback callback) {
        AuthClient.request(this, callback);
    }
}
