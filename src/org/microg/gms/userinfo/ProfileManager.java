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

package org.microg.gms.userinfo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthRequest;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class ProfileManager {
    private static final String TAG = "GmsProfileManager";
    private static final String PREFERENCES_NAME = "profile_manager";
    public static final String SERVICE_TOKEN = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    public static class ProfileInfo {
        public final String familyName;
        public final String givenName;
        public final long id;
        public final String link;
        public final String locale;
        public final String name;
        public final String picture;

        public ProfileInfo(String familyName, String givenName, long id, String link, String locale, String name, String picture) {
            this.familyName = familyName;
            this.givenName = givenName;
            this.id = id;
            this.link = link;
            this.locale = locale;
            this.name = name;
            this.picture = picture;
        }

        public static ProfileInfo parse(byte[] bytes) throws JSONException {
            return parse(new String(bytes));
        }

        private static ProfileInfo parse(String info) throws JSONException {
            return parse(new JSONObject(info));
        }

        private static ProfileInfo parse(JSONObject info) throws JSONException {
            return new ProfileInfo(
                    info.has("family_name") ? info.getString("family_name") : null,
                    info.has("given_name") ? info.getString("given_name") : null,
                    info.has("id") ? info.getLong("id") : 0,
                    info.has("link") ? info.getString("link") : null,
                    info.has("locale") ? info.getString("locale") : null,
                    info.has("name") ? info.getString("name") : null,
                    info.has("picture") ? info.getString("picture") : null);
        }
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static Bitmap getProfilePicture(Context context, Account account, boolean network) {
        SharedPreferences preferences = getPreferences(context);
        String picture = preferences.getString("profile_picture", null);
        if (picture != null) {
            byte[] bytes = Base64.decode(picture, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        if (!network) return null;
        try {
            URLConnection conn = new URL(getProfileInfo(context, account).picture).openConnection();
            conn.setDoInput(true);
            byte[] bytes = Utils.readStreamToEnd(conn.getInputStream());
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null)
                preferences.edit().putString("profile_picture", Base64.encodeToString(bytes, Base64.DEFAULT)).apply();
            return bitmap;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static ProfileInfo getProfileInfo(Context context, Account account) {
        try {
            URLConnection conn = new URL("https://www.googleapis.com/oauth2/v1/userinfo").openConnection();
            conn.addRequestProperty("Authorization", "Bearer " + getAuthKey(context, account));
            conn.setDoInput(true);
            byte[] bytes = Utils.readStreamToEnd(conn.getInputStream());
            return ProfileInfo.parse(bytes);
        } catch (JSONException | IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static AuthRequest getAuthKeyRequest(Context context, Account account) {
        return new AuthRequest().fromContext(context)
                .appIsGms().callerIsGms()
                .service(SERVICE_TOKEN)
                .email(account.name)
                .token(AccountManager.get(context).getPassword(account))
                .systemPartition()
                .hasPermission()
                .getAccountId();
    }

    public static String getAuthKey(Context context, Account account) {
        String result = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(account.name + "_auth_key", null);
        if (result == null) {
            try {
                AuthResponse response = getAuthKeyRequest(context, account).getResponse();
                AuthManager.storeResponse(context, account,
                        Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1,
                        SERVICE_TOKEN, response);
                result = response.auth;
                storeAuthKey(context, account, result);
            } catch (IOException e) {
                return null;
            }
        }
        return result;
    }

    public static void storeAuthKey(Context context, Account account, String key) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(account.name + "_auth_key", key).commit();
    }
}
