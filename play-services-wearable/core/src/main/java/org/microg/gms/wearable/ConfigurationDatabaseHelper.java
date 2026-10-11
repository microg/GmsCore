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

package org.microg.gms.wearable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Objects;

public class ConfigurationDatabaseHelper extends SQLiteOpenHelper {

    public static final String NULL_STRING = "NULL_STRING";
    public static final String TABLE_NAME = "connectionConfigurations";
    public static final String BY_NAME = "name=?";
    private static final String EMULATOR_ADDRESS_PREFIX = "EmulatorAddr-";

    public ConfigurationDatabaseHelper(Context context) {
        super(context, "connectionconfig.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE connectionConfigurations (_id INTEGER PRIMARY KEY AUTOINCREMENT,androidId TEXT,name TEXT NOT NULL,pairedBtAddress TEXT NOT NULL,connectionType INTEGER NOT NULL,role INTEGER NOT NULL,connectionEnabled INTEGER NOT NULL,nodeId TEXT, UNIQUE(name) ON CONFLICT REPLACE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private static ConnectionConfiguration configFromCursor(final Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String pairedBtAddress = cursor.getString(cursor.getColumnIndexOrThrow("pairedBtAddress"));
        int connectionType = cursor.getInt(cursor.getColumnIndexOrThrow("connectionType"));
        int role = cursor.getInt(cursor.getColumnIndexOrThrow("role"));
        int enabled = cursor.getInt(cursor.getColumnIndexOrThrow("connectionEnabled"));
        String nodeId = cursor.getString(cursor.getColumnIndexOrThrow("nodeId"));
        if (NULL_STRING.equals(name)) name = null;
        if (NULL_STRING.equals(pairedBtAddress)) pairedBtAddress = null;
        return new ConnectionConfiguration(name, pairedBtAddress, connectionType, role, enabled > 0, nodeId);
    }

    public ConnectionConfiguration getConfiguration(String name) {
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, null, BY_NAME, new String[]{name}, null, null, null);
        ConnectionConfiguration config = null;
        if (cursor != null) {
            if (cursor.moveToNext())
                config = configFromCursor(cursor);
            cursor.close();
        }
        return config;
    }

    public void putConfiguration(ConnectionConfiguration config) {
        putConfiguration(config, null);
    }

    public void putConfiguration(ConnectionConfiguration config, String oldNodeId) {
        ContentValues contentValues = new ContentValues();
        if (config.name != null) {
            contentValues.put("name", config.name);
        } else if (config.role == 2) {
            contentValues.put("name", "server");
        } else {
            contentValues.put("name", "NULL_STRING");
        }
        if (config.address != null) {
            contentValues.put("pairedBtAddress", config.address);
        } else {
            contentValues.put("pairedBtAddress", "NULL_STRING");
        }
        contentValues.put("connectionType", config.type);
        contentValues.put("role", config.role);
        contentValues.put("connectionEnabled", config.enabled);
        contentValues.put("nodeId", config.nodeId);
        if (oldNodeId == null) {
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        } else {
            getWritableDatabase().update(TABLE_NAME, contentValues, "name=? AND nodeId=?", new String[]{configurationName(config), oldNodeId});
        }
    }

    static String configurationName(ConnectionConfiguration config) {
        return config.name != null ? config.name : config.role == 2 ? "server" : NULL_STRING;
    }

    static void validateManagedConfiguration(ConnectionConfiguration config) {
        if (config == null || config.name != null && (config.name.isEmpty() || config.name.length() > 256)
                || NULL_STRING.equals(config.name) || NULL_STRING.equals(config.address)
                || config.address != null && config.address.length() > 256) {
            throw new IllegalArgumentException("Invalid connection configuration");
        }
        boolean tcp = config.type == 2 && config.role == 2;
        boolean bluetooth = (config.type == 1 || config.type == 5) && config.role == 1
                && config.name != null && config.address != null;
        if (!tcp && !bluetooth) {
            throw new IllegalArgumentException("Unsupported connection transport");
        }
        if (tcp) emulatorNodeId(config);
        if (config.hasUnsupportedConnectionPolicies || config.migrating || config.skipConnectingIfNotBonded
                || config.runtimeType != 0 || config.connectionRetryStrategy != 0
                || config.allowedConfigPackages != null && !config.allowedConfigPackages.isEmpty()) {
            throw new IllegalArgumentException("Unsupported connection policy");
        }
    }

    static String emulatorNodeId(ConnectionConfiguration config) {
        if (config.address == null) return null; // Legacy unnamed emulator listener.
        // The companion wraps emulator-id in this marker; this is not a network address.
        if (!config.address.startsWith(EMULATOR_ADDRESS_PREFIX)
                || config.address.length() == EMULATOR_ADDRESS_PREFIX.length()) {
            throw new IllegalArgumentException("Invalid emulator node address");
        }
        return config.address.substring(EMULATOR_ADDRESS_PREFIX.length());
    }

    /** Companion writes cannot replace an existing link's negotiated node identity. */
    public ConnectionConfiguration putManagedConfiguration(ConnectionConfiguration config, String localNodeId) {
        validateManagedConfiguration(config);
        if (!config.dataItemSyncEnabled) throw new IllegalArgumentException("Deferred data sync is not supported");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String name = configurationName(config);
            ConnectionConfiguration existing = getConfiguration(name);
            for (ConnectionConfiguration other : getAllConfigurations()) {
                if (!name.equals(other.name) && Objects.equals(other.address, config.address)) {
                    throw new IllegalArgumentException("Connection already has another configuration");
                }
            }
            if (existing != null && (!Objects.equals(existing.address, config.address)
                    || existing.type != config.type || existing.role != config.role)) {
                throw new IllegalArgumentException("Configuration name belongs to another link");
            }
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("pairedBtAddress", config.address == null ? NULL_STRING : config.address);
            values.put("connectionType", config.type);
            values.put("role", config.role);
            values.put("connectionEnabled", config.enabled);
            if (existing == null) {
                values.put("nodeId", localNodeId);
                db.insertOrThrow(TABLE_NAME, null, values);
            } else {
                db.updateWithOnConflict(TABLE_NAME, values, BY_NAME, new String[]{name}, SQLiteDatabase.CONFLICT_ABORT);
            }
            if (config.type == 2 && config.enabled) selectEmulatorConfiguration(db, name);
            ConnectionConfiguration stored = getConfiguration(name);
            db.setTransactionSuccessful();
            return stored;
        } finally {
            db.endTransaction();
        }
    }

    /** Update is deliberately separate from put: absent links stay absent and no identity is imported. */
    public boolean updateConfiguration(ConnectionConfiguration config) {
        validateManagedConfiguration(config);
        // The update contract cannot disable data-item sync once it is enabled.
        // All configurations supported here already have sync enabled, so false preserves true.
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<ConnectionConfiguration> matching = new ArrayList<>();
            try (Cursor cursor = db.query(TABLE_NAME, null, "pairedBtAddress=?",
                    new String[]{config.address == null ? NULL_STRING : config.address}, null, null, null)) {
                while (cursor.moveToNext()) matching.add(configFromCursor(cursor));
            }
            if (matching.isEmpty()) {
                db.setTransactionSuccessful();
                return false;
            }
            if (matching.size() != 1) throw new IllegalArgumentException("Ambiguous connection address");
            ConnectionConfiguration existing = matching.get(0);
            if (!Objects.equals(existing.name, configurationName(config))
                    || existing.type != config.type || existing.role != config.role) {
                throw new IllegalArgumentException("Update cannot redirect a connection");
            }
            ContentValues values = new ContentValues();
            values.put("connectionEnabled", config.enabled);
            db.updateWithOnConflict(TABLE_NAME, values, BY_NAME, new String[]{existing.name}, SQLiteDatabase.CONFLICT_ABORT);
            if (config.type == 2 && config.enabled) selectEmulatorConfiguration(db, existing.name);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public ConnectionConfiguration[] getAllConfigurations() {
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
        if (cursor != null) {
            List<ConnectionConfiguration> configurations = new ArrayList<ConnectionConfiguration>();
            while (cursor.moveToNext()) {
                configurations.add(configFromCursor(cursor));
            }
            cursor.close();
            return configurations.toArray(new ConnectionConfiguration[configurations.size()]);
        } else {
            return null;
        }
    }

    public void setEnabledState(String name, boolean enabled) {
        getWritableDatabase().execSQL("UPDATE connectionConfigurations SET connectionEnabled=? WHERE name=?", new String[]{enabled ? "1" : "0", name});
    }

    /** Select one emulator without deleting another emulator's negotiated identity or data. */
    public void enableManagedConfiguration(String name) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ConnectionConfiguration config = getConfiguration(name);
            if (config == null) throw new IllegalArgumentException("Unknown connection configuration");
            validateManagedConfiguration(config);
            setEnabledState(name, true);
            if (config.type == 2) selectEmulatorConfiguration(db, name);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void selectEmulatorConfiguration(SQLiteDatabase db, String name) {
        List<String> addresses = new ArrayList<>();
        for (ConnectionConfiguration config : getAllConfigurations()) {
            if (config.type != 2) continue;
            validateManagedConfiguration(config);
            if (config.name == null || config.nodeId == null || config.nodeId.isEmpty()) {
                throw new IllegalArgumentException("Invalid stored emulator configuration");
            }
            if (addresses.contains(config.address)) throw new IllegalArgumentException("Ambiguous emulator address");
            addresses.add(config.address);
        }
        ContentValues disabled = new ContentValues();
        disabled.put("connectionEnabled", false);
        db.update(TABLE_NAME, disabled, "connectionType=? AND name<>?", new String[]{"2", name});
    }

    public int deleteConfiguration(String name) {
        return getWritableDatabase().delete(TABLE_NAME, BY_NAME, new String[]{name});
    }
}
