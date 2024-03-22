/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth;

import android.content.Context;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.profile.Build;
import org.microg.gms.common.Constants;
import org.microg.gms.common.HttpFormClient;
import org.microg.gms.common.Utils;
import org.microg.gms.profile.ProfileManager;
import org.microg.gms.settings.SettingsContract;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

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
    public int gmsVersion = Constants.GMS_VERSION_CODE;
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
    @RequestContent("delegation_type")
    public String delegationType;
    @RequestContent("delegatee_user_id")
    public String delegateeUserId;
    @RequestContent("oauth2_foreground")
    public String oauth2Foreground;
    @RequestContent("token_request_options")
    public String tokenRequestOptions;
    @RequestContent("it_caveat_types")
    public String itCaveatTypes;
    @RequestContent("check_email")
    public boolean checkEmail;
    @RequestContent("request_visible_actions")
    public String requestVisibleActions;
    @RequestContent("oauth2_prompt")
    public String oauth2Prompt;
    @RequestContent("oauth2_include_profile")
    public String oauth2IncludeProfile;
    @RequestContent("oauth2_include_email")
    public String oauth2IncludeEmail;
    @HttpFormClient.RequestContentDynamic
    public Map<Object, Object> dynamicFields;

    public String deviceName;
    public String buildVersion;

    @Override
    protected void prepare() {
        userAgent = String.format(USER_AGENT, deviceName, buildVersion);
    }

    public AuthRequest build(Context context) {
        ProfileManager.ensureInitialized(context);
        sdkVersion = Build.VERSION.SDK_INT;
        deviceName = Build.DEVICE;
        buildVersion = Build.ID;
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
        build(context);
        locale(Utils.getLocale(context));
        if (AuthPrefs.shouldIncludeAndroidId(context)) {
            androidIdHex = Long.toHexString(LastCheckinInfo.read(context).getAndroidId());
        }
        if (AuthPrefs.shouldStripDeviceName(context)) {
            deviceName = "";
            buildVersion = "";
        }
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

    public AuthRequest systemPartition(boolean systemPartition) {
        this.systemPartition = systemPartition;
        return this;
    }

    public AuthRequest hasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
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

    public AuthRequest delegation(int delegationType, String delegateeUserId) {
        this.delegationType = delegationType == 0 ? null : Integer.toString(delegationType);
        this.delegateeUserId = delegateeUserId;
        return this;
    }

    public AuthRequest oauth2Foreground(String oauth2Foreground) {
        this.oauth2Foreground = oauth2Foreground;
        return this;
    }

    public AuthRequest tokenRequestOptions(String tokenRequestOptions) {
        this.tokenRequestOptions = tokenRequestOptions;
        return this;
    }

    public AuthRequest oauth2IncludeProfile(String oauth2IncludeProfile) {
        this.oauth2IncludeProfile = oauth2IncludeProfile;
        return this;
    }

    public AuthRequest oauth2IncludeEmail(String oauth2IncludeProfile) {
        this.oauth2IncludeEmail = oauth2IncludeEmail;
        return this;
    }

    public AuthRequest oauth2Prompt(String oauth2Prompt) {
        this.oauth2Prompt = oauth2Prompt;
        return this;
    }

    public AuthRequest itCaveatTypes(String itCaveatTypes) {
        this.itCaveatTypes = itCaveatTypes;
        return this;
    }

    public AuthRequest putDynamicFiledMap(Map<Object, Object> dynamicFields) {
        this.dynamicFields = dynamicFields;
        return this;
    }

    public AuthResponse getResponse() throws IOException {
        return HttpFormClient.request(SERVICE_URL, this, AuthResponse.class);
    }

    public void getResponseAsync(HttpFormClient.Callback<AuthResponse> callback) {
        HttpFormClient.requestAsync(SERVICE_URL, this, AuthResponse.class, callback);
    }
}
