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

package org.microg.gms.gservices;

import static android.os.Build.VERSION.SDK_INT;

import android.app.admin.DevicePolicyManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.microg.gms.utils.ExtendedPackageInfo;

/**
 * Originally found in Google Services Framework (com.google.android.gsf), this provides a generic
 * key-value store, that is written by the checkin service and read from various Google Apps.
 * <p/>
 * Google uses the checkin process to store various device or country specific settings and
 * if certain "experiments" are enabled on the device.
 */
public class GServicesProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.google.android.gsf.gservices/");
    public static final Uri MAIN_URI = Uri.withAppendedPath(CONTENT_URI, "main");
    public static final Uri OVERRIDE_URI = Uri.withAppendedPath(CONTENT_URI, "override");
    public static final Uri PREFIX_URI = Uri.withAppendedPath(CONTENT_URI, "prefix");

    private static final String TAG = "GmsServicesProvider";

    private DatabaseHelper databaseHelper;
    private Map<String, String> cache = new HashMap<String, String>();
    private Set<String> cachedPrefixes = new HashSet<String>();

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    private boolean callerIsGoogle() {
        try {
            String packageName = getCallingPackage();
            if (packageName == null) return false;
            PackageManager packageManager = getContext().getPackageManager();
            return new ExtendedPackageInfo(packageManager, packageName).isGooglePackage();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean callerIsOwner() {
        try {
            String packageName = getCallingPackage();
            if (packageName == null) return false;
            DevicePolicyManager devicePolicyManager = ContextCompat.getSystemService(getContext(), DevicePolicyManager.class);
            return devicePolicyManager.isDeviceOwnerApp(packageName) || (SDK_INT >= 21 && devicePolicyManager.isProfileOwnerApp(packageName));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Instead of checking if the permissions is granted, we only check if it was requested.
     * <p>
     * This is because:
     * <ul>
     *     <li>
     *         this is a permission with protectionLevel normal and requesting should usually
     *         silently grant it, as Android no longer asks users to confirm install-time
     *         permissions on installation
     *     </li>
     *     <li>
     *         on some Android versions, requesting a permission that is not declared yet will not
     *         grant that permission if the declaring app is installed later
     *     </li>
     * </ul>
     */
    private boolean callerHasReadPermissionRequested() {
        String packageName = getCallingPackage();
        if (packageName == null) return false;
        try {
            String[] requestedPermissions = getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            return requestedPermissions != null && Arrays.asList(requestedPermissions).contains("com.google.android.providers.gsf.permission.READ_GSERVICES");
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (Binder.getCallingUid() != Process.myUid() && !callerIsGoogle() && !callerHasReadPermissionRequested()) {
            Log.w(TAG, "Rejected read access for package " + getCallingPackage());
            return null;
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{"name", "value"});
        if (PREFIX_URI.equals(uri)) {
            for (String prefix : selectionArgs) {
                if (!cachedPrefixes.contains(prefix)) {
                    cache.putAll(databaseHelper.search(prefix + "%"));
                    cachedPrefixes.add(prefix);
                }

                for (String name : cache.keySet()) {
                    if (name.startsWith(prefix)) {
                        String value = cache.get(name);
                        if (value != null) {
                            cursor.addRow(new String[]{name, value});
                        }
                    }
                }
            }
        } else {
            for (String name : selectionArgs) {
                String value;
                if (cache.containsKey(name)) {
                    value = cache.get(name);
                } else {
                    value = databaseHelper.get(name);
                    cache.put(name, value);
                }
                if ("android_id".equals(name)) {
                    // Only Google and device/profile owner can access android_id
                    if (Binder.getCallingUid() != Process.myUid() && !callerIsGoogle() && !callerIsOwner()) {
                        Log.w(TAG, "Rejected access to android_id for package " + getCallingPackage());
                        value = null;
                    } else {
                        Log.d(TAG, "Granted access to android_id for package " + getCallingPackage());
                    }
                }
                if (value != null) {
                    cursor.addRow(new String[]{name, value});
                }
            }
        }
        if (cursor.getCount() == 0) return null;
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (Binder.getCallingUid() != Process.myUid() && !callerIsGoogle()) {
            Log.w(TAG, "Rejected write access for package " + getCallingPackage());
            return 0;
        }
        Log.d(TAG, "update caller=" + getCallingPackage() + " table=" + uri.getLastPathSegment()
                + " name=" + values.getAsString("name") + " value=" + values.getAsString("value"));
        if (uri.equals(MAIN_URI)) {
            databaseHelper.put("main", values);
        } else if (uri.equals(OVERRIDE_URI)) {
            databaseHelper.put("override", values);
        }
        String name = values.getAsString("name");
        cache.remove(name);
        Iterator<String> iterator = cachedPrefixes.iterator();
        while (iterator.hasNext()) if (name.startsWith(iterator.next())) iterator.remove();
        return 1;
    }
}
