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

package org.microg.gms.people;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.common.Scopes;

import org.json.JSONObject;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class PeopleManager {
    private static final String TAG = "GmsPeopleManager";
    public static final String USERINFO_SCOPE = "oauth2:" + Scopes.USERINFO_PROFILE;
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    public static final String REGEX_SEARCH_USER_PHOTO = "https?\\:\\/\\/lh([0-9]*)\\.googleusercontent\\.com/";

    public static String getDisplayName(Context context, String accountName) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Cursor cursor = databaseHelper.getOwner(accountName);
        String displayName = null;
        try {
            if (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex("display_name");
                if (idx >= 0 && !cursor.isNull(idx)) displayName = cursor.getString(idx);
            }
        } finally {
            cursor.close();
            databaseHelper.close();
        }
        return displayName;
    }

    public static String getGivenName(Context context, String accountName) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Cursor cursor = databaseHelper.getOwner(accountName);
        String displayName = null;
        try {
            if (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex("given_name");
                if (idx >= 0 && !cursor.isNull(idx)) displayName = cursor.getString(idx);
            }
        } finally {
            cursor.close();
            databaseHelper.close();
        }
        return displayName;
    }

    public static File getOwnerAvatarFile(Context context, String accountName, boolean network) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Cursor cursor = databaseHelper.getOwner(accountName);
        String url = null;
        if (cursor.moveToNext()) {
            int idx = cursor.getColumnIndex("avatar");
            if (idx >= 0 && !cursor.isNull(idx)) url = cursor.getString(idx);
        }
        cursor.close();
        databaseHelper.close();
        if (url == null) return null;
        String urlLastPart = url.replaceFirst(REGEX_SEARCH_USER_PHOTO, "");
        File file = new File(context.getCacheDir(), urlLastPart);
        if (!file.getParentFile().mkdirs() && file.exists()) {
            return file;
        }
        if (!network) return null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setDoInput(true);
            byte[] bytes = Utils.readStreamToEnd(conn.getInputStream());
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }

    }

    public static Bitmap getOwnerAvatarBitmap(Context context, String accountName, boolean network) {
        File avaterFile = getOwnerAvatarFile(context, accountName, network);
        if (avaterFile == null) return null;
        return BitmapFactory.decodeFile(avaterFile.getPath());
    }

    public static String loadUserInfo(Context context, Account account) {
        try {
            URLConnection conn = new URL(USERINFO_URL).openConnection();
            conn.addRequestProperty("Authorization", "Bearer " + getUserInfoAuthKey(context, account));
            conn.setDoInput(true);
            byte[] bytes = Utils.readStreamToEnd(conn.getInputStream());
            JSONObject info = new JSONObject(new String(bytes));
            ContentValues contentValues = new ContentValues();
            contentValues.put("account_name", account.name);
            if (info.has("id")) contentValues.put("gaia_id", info.getString("id"));
            if (info.has("picture")) contentValues.put("avatar", info.getString("picture"));
            if (info.has("name")) contentValues.put("display_name", info.getString("name"));
            if (info.has("given_name")) contentValues.put("given_name", info.getString("given_name"));
            if (info.has("family_name")) contentValues.put("family_name", info.getString("family_name"));
            contentValues.put("last_sync_start_time", System.currentTimeMillis());
            contentValues.put("last_sync_finish_time", System.currentTimeMillis());
            contentValues.put("last_successful_sync_time", System.currentTimeMillis());
            contentValues.put("last_full_people_sync_time", System.currentTimeMillis());
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            databaseHelper.putOwner(contentValues);
            databaseHelper.close();
            return contentValues.getAsString("gaia_id");
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static String getUserInfoAuthKey(Context context, Account account) {
        AuthManager authManager = new AuthManager(context, account.name, Constants.GMS_PACKAGE_NAME, USERINFO_SCOPE);
        authManager.setPermitted(true);
        String result = authManager.getAuthToken();
        if (result == null) {
            try {
                AuthResponse response = authManager.requestAuthWithBackgroundResolution(false);
                result = response.auth;
            } catch (IOException e) {
                Log.w(TAG, e);
                return null;
            }
        }
        return result;
    }
}
