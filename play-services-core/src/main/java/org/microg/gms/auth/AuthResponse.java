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

import android.util.Log;

import java.lang.reflect.Field;

import static org.microg.gms.common.HttpFormClient.ResponseField;

public class AuthResponse {
    private static final String TAG = "GmsAuthResponse";

    @ResponseField("SID")
    public String Sid;
    @ResponseField("LSID")
    public String LSid;
    @ResponseField("Auth")
    public String auth;
    @ResponseField("Token")
    public String token;
    @ResponseField("Email")
    public String email;
    @ResponseField("services")
    public String services;
    @ResponseField("GooglePlusUpgrade")
    public boolean isGooglePlusUpgrade;
    @ResponseField("PicasaUser")
    public String picasaUserName;
    @ResponseField("RopText")
    public String ropText;
    @ResponseField("RopRevision")
    public int ropRevision;
    @ResponseField("firstName")
    public String firstName;
    @ResponseField("lastName")
    public String lastName;
    @ResponseField("issueAdvice")
    public String issueAdvice;
    @ResponseField("accountId")
    public String accountId;
    @ResponseField("Expiry")
    public long expiry = -1;
    @ResponseField("storeConsentRemotely")
    public boolean storeConsentRemotely = true;
    @ResponseField("Permission")
    public String permission;
    @ResponseField("ScopeConsentDetails")
    public String scopeConsentDetails;
    @ResponseField("ConsentDataBase64")
    public String consentDataBase64;
    @ResponseField("grantedScopes")
    public String grantedScopes;
    @ResponseField("itMetadata")
    public String itMetadata;
    @ResponseField("ResolutionDataBase64")
    public String resolutionDataBase64;
    @ResponseField("it")
    public String auths;

    public static AuthResponse parse(String result) {
        AuthResponse response = new AuthResponse();
        String[] entries = result.split("\n");
        for (String s : entries) {
            String[] keyValuePair = s.split("=", 2);
            String key = keyValuePair[0].trim();
            String value = keyValuePair[1].trim();
            try {
                for (Field field : AuthResponse.class.getDeclaredFields()) {
                    if (field.isAnnotationPresent(ResponseField.class) &&
                            key.equals(field.getAnnotation(ResponseField.class).value())) {
                        if (field.getType().equals(String.class)) {
                            field.set(response, value);
                        } else if (field.getType().equals(boolean.class)) {
                            field.setBoolean(response, value.equals("1"));
                        } else if (field.getType().equals(long.class)) {
                            field.setLong(response, Long.parseLong(value));
                        } else if (field.getType().equals(int.class)) {
                            field.setInt(response, Integer.parseInt(value));
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        return response;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AuthResponse{");
        sb.append("auth='").append(auth).append('\'');
        if (Sid != null) sb.append(", Sid='").append(Sid).append('\'');
        if (LSid != null) sb.append(", LSid='").append(LSid).append('\'');
        if (token != null) sb.append(", token='").append(token).append('\'');
        if (email != null) sb.append(", email='").append(email).append('\'');
        if (services != null) sb.append(", services='").append(services).append('\'');
        if (isGooglePlusUpgrade) sb.append(", isGooglePlusUpgrade=").append(isGooglePlusUpgrade);
        if (picasaUserName != null) sb.append(", picasaUserName='").append(picasaUserName).append('\'');
        if (ropText != null) sb.append(", ropText='").append(ropText).append('\'');
        if (ropRevision != 0) sb.append(", ropRevision=").append(ropRevision);
        if (firstName != null) sb.append(", firstName='").append(firstName).append('\'');
        if (lastName != null) sb.append(", lastName='").append(lastName).append('\'');
        if (issueAdvice != null) sb.append(", issueAdvice='").append(issueAdvice).append('\'');
        if (accountId != null) sb.append(", accountId='").append(accountId).append('\'');
        if (expiry != -1) sb.append(", expiry=").append(expiry);
        if (!storeConsentRemotely) sb.append(", storeConsentRemotely=").append(storeConsentRemotely);
        if (permission != null) sb.append(", permission='").append(permission).append('\'');
        if (scopeConsentDetails != null) sb.append(", scopeConsentDetails='").append(scopeConsentDetails).append('\'');
        if (consentDataBase64 != null) sb.append(", consentDataBase64='").append(consentDataBase64).append('\'');
        if (auths != null) sb.append(", auths='").append(auths).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
