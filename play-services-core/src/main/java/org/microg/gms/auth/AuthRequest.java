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

package org.microg.gms.auth;

import android.content.Context;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.Build;
import org.microg.gms.common.Constants;
import org.microg.gms.common.HttpFormClient;
import org.microg.gms.common.Utils;

import java.io.IOException;
import java.util.Locale;

import static org.microg.gms.common.HttpFormClient.RequestContent;
import static org.microg.gms.common.HttpFormClient.RequestHeader;

public class AuthRequest extends HttpFormClient.Request {
    private static final String SERVICE_URL = "https://android.googleapis.com/auth";
    private static final String USER_AGENT = "GoogleAuth/1.4 (%s %s); gzip";

    @RequestHeader("User-Agent")
    private String userAgent;

    @RequestHeader("app")
    @RequestContent("app")
    public String app;
    @RequestContent("client_sig")
    public String appSignature;
    @RequestContent("callerPkg")
    public String caller;
    @RequestContent("callerSig")
    public String callerSignature;
    @RequestHeader(value = "device", nullPresent = true)
    @RequestContent(value = "androidId", nullPresent = true)
    public String androidIdHex;
    @RequestContent("sdk_version")
    public int sdkVersion;
    @RequestContent("device_country")
    public String countryCode;
    @RequestContent("operatorCountry")
    public String operatorCountryCode;
    @RequestContent("lang")
    public String locale;
    @RequestContent("google_play_services_version")
    public int gmsVersion = Constants.MAX_REFERENCE_VERSION;
    @RequestContent("accountType")
    public String accountType;
    @RequestContent("Email")
    public String email;
    @RequestContent("service")
    public String service;
    @RequestContent("source")
    public String source;
    @RequestContent({"is_called_from_account_manager", "_opt_is_called_from_account_manager"})
    public boolean isCalledFromAccountManager;
    @RequestContent("Token")
    public String token;
    @RequestContent("system_partition")
    public boolean systemPartition;
    @RequestContent("get_accountid")
    public boolean getAccountId;
    @RequestContent("ACCESS_TOKEN")
    public boolean isAccessToken;
    @RequestContent("droidguard_results")
    public String droidguardResults;
    @RequestContent("has_permission")
    public boolean hasPermission;
    @RequestContent("add_account")
    public boolean addAccount;
    public String deviceName;
    public String buildVersion;

    @Override
    protected void prepare() {
        userAgent = String.format(USER_AGENT, deviceName, buildVersion);
    }

    public AuthRequest build(Build build) {
        sdkVersion = build.sdk;
        deviceName = build.device;
        buildVersion = build.id;
        return this;
    }

    public AuthRequest source(String source) {
        this.source = source;
        return this;
    }

    public AuthRequest locale(Locale locale) {
        this.locale = locale.toString();
        this.countryCode = locale.getCountry().toLowerCase();
        this.operatorCountryCode = locale.getCountry().toLowerCase();
        return this;
    }

    public AuthRequest fromContext(Context context) {
        build(Utils.getBuild(context));
        locale(Utils.getLocale(context));
        androidIdHex = Long.toHexString(LastCheckinInfo.read(context).androidId);
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
        return HttpFormClient.request(SERVICE_URL, this, AuthResponse.class);
    }

    public void getResponseAsync(HttpFormClient.Callback<AuthResponse> callback) {
        HttpFormClient.requestAsync(SERVICE_URL, this, AuthResponse.class, callback);
    }
}
