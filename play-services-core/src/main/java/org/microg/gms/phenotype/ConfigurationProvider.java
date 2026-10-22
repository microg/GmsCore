/*
 * Copyright (C) 2018 microG Project Team
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

package org.microg.gms.phenotype;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.UserManager;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.phenotype.Configuration;
import com.google.android.gms.phenotype.Configurations;
import com.google.android.gms.phenotype.Flag;
import org.microg.gms.common.GooglePackagePermission;
import org.microg.gms.common.PackageUtils;

import java.util.Objects;

public class ConfigurationProvider extends ContentProvider {
    private static final String TAG = "GmsPhenotypeCfgProvider";
    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String requestedPackage = uri.getLastPathSegment();
        if (requestedPackage == null) return null;
        Context context = Objects.requireNonNull(getContext());
        boolean unlocked = android.os.Build.VERSION.SDK_INT < 24 || context.getSystemService(UserManager.class).isUserUnlocked();
        Context storageContext = unlocked ? context : context.createDeviceProtectedStorageContext();
        PhenotypeDatabase database = PhenotypeDatabase.Companion.getInstance(storageContext);
        String caller = getCallingPackage();
        if (caller == null) caller = context.getPackageManager().getNameForUid(Binder.getCallingUid());
        String packageName = database.resolvePackageName(requestedPackage, caller == null ? "" : caller);
        int separator = packageName.indexOf('#');
        String namespace = separator < 0 ? packageName : packageName.substring(0, separator);
        String owner = separator < 0 ? "" : packageName.substring(separator + 1);

        if (Binder.getCallingUid() != Process.myUid()
                && !PackageUtils.callerHasGooglePackagePermission(context, GooglePackagePermission.APP_CERT)
                && (!database.allowsThirdPartyNamespace(namespace) || !Objects.equals(owner, caller))) return null;

        String account = null;
        if (projection != null) {
            for (int i = 0; i + 1 < projection.length; i += 2) {
                if ("account".equals(projection[i])) {
                    if (account != null || projection[i + 1] == null) return null;
                    account = projection[i + 1];
                }
            }
        }
        Configurations configurations;
        if (account == null) {
            configurations = database.getCommittedConfiguration(packageName);
        } else {
            if (!unlocked) {
                throw new IllegalArgumentException("Phenotype commitOnQuery cannot be used in direct boot mode");
            }
            if (!account.isEmpty()) {
                boolean found = false;
                for (Account available : PhenotypeSyncClient.accounts(context)) {
                    if (account.equals(available.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return null;
            }
            configurations = database.getConfigurationAndCommit(packageName, account);
            if (configurations != null) {
                database.publishCommittedConfiguration(context, packageName);
            }
        }
        if (configurations == null || configurations.field4 == null) return null;
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        for (Configuration configuration : configurations.field4) {
            for (Flag flag : configuration.flags) cursor.addRow(new String[]{flag.name, value(flag)});
        }
        cursor.addRow(new String[]{"__phenotype_server_token", configurations.serverToken});
        cursor.addRow(new String[]{"__phenotype_snapshot_token", configurations.snapshotToken});
        cursor.addRow(new String[]{"__phenotype_configuration_version", Long.toString(configurations.version)});
        return cursor;
    }

    private static String value(Flag flag) {
        switch (flag.dataType) {
            case Flag.DATA_TYPE_LONG: return Long.toString(flag.getLong());
            case Flag.DATA_TYPE_BOOL: return Boolean.toString(flag.getBool());
            case Flag.DATA_TYPE_DOUBLE: return Double.toString(flag.getDouble());
            case Flag.DATA_TYPE_STRING: return String.valueOf(flag.getString());
            case Flag.DATA_TYPE_BYTES: return Base64.encodeToString(Objects.requireNonNull(flag.getBytes()), Base64.NO_PADDING | Base64.NO_WRAP);
            default: throw new AssertionError("Unknown flag value type");
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }
}
