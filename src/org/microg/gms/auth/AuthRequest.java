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

import java.util.HashMap;
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
    public int gmsVersion;
    public String accountType = "HOSTED_OR_GOOGLE";
    public String email;
    public String service;
    public String source = "android";
    public boolean isCalledFromAccountManager;
    public String token;
    public boolean isSystemPartition;
    public boolean getAccountId;
    public boolean isAccessToken;
    public String droidguardResults;
    public boolean hasPermission;
    public boolean addAccount;


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
        if (isSystemPartition) map.put("system_partition", "1");
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
}
