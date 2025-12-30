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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfigurationDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ConfigDB";

    public static final String NULL_STRING = "NULL_STRING";
    public static final String TABLE_NAME = "connectionConfigurations";
    public static final String BY_NAME = "name=?";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ANDROID_ID = "androidId";
    private static final String COLUMN_ALLOWED_CONFIG_PACKAGES = "allowedConfigPackages";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PAIRED_BT_ADDRESS = "pairedBtAddress";
    private static final String COLUMN_CONNECTION_TYPE = "connectionType";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_CONNECTION_ENABLED = "connectionEnabled";
    private static final String COLUMN_NODE_ID = "nodeId";
    private static final String COLUMN_CRYPTO = "crypto";
    private static final String COLUMN_PACKAGE_NAME = "packageName";
    private static final String COLUMN_IS_MIGRATING = "isMigrating";
    private static final String COLUMN_DATA_ITEM_SYNC_ENABLED = "dataItemSyncEnabled";
    private static final String COLUMN_RESTRICTIONS = "restrictions";
    private static final String COLUMN_REMOVE_CONNECTION_WHEN_BOND_REMOVED = "removeConnectionWhenBondRemovedByUser";
    private static final String COLUMN_CONNECTION_DELAY_FILTERS = "connectionDelayFilters";
    private static final String COLUMN_MAX_SUPPORTED_REMOTE_ANDROID_SDK = "maxSupportedRemoteAndroidSdkVersion";

    public static final int TYPE_BLUETOOTH_RFCOMM = 1;
    public static final int TYPE_NETWORK = 2;
    public static final int TYPE_BLE = 3;
    public static final int TYPE_CLOUD = 4;
    public static final int TYPE_BLUETOOTH_L2CAP = 5;

    public static final int ROLE_CLIENT = 1;
    public static final int ROLE_SERVER = 2;

    public ConfigurationDatabaseHelper(Context context) {
        super(context, "connectionconfig.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("CREATE TABLE connectionConfigurations (_id INTEGER PRIMARY KEY AUTOINCREMENT,androidId TEXT,name TEXT NOT NULL,pairedBtAddress TEXT NOT NULL,connectionType INTEGER NOT NULL,role INTEGER NOT NULL,connectionEnabled INTEGER NOT NULL,nodeId TEXT, UNIQUE(name) ON CONFLICT REPLACE);");
        try {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_ANDROID_ID + " TEXT," +
                    COLUMN_ALLOWED_CONFIG_PACKAGES + " TEXT," +
                    COLUMN_NAME + " TEXT NOT NULL," +
                    COLUMN_PAIRED_BT_ADDRESS + " TEXT NOT NULL," +
                    COLUMN_CONNECTION_TYPE + " INTEGER NOT NULL," +
                    COLUMN_ROLE + " INTEGER NOT NULL," +
                    COLUMN_CONNECTION_ENABLED + " INTEGER NOT NULL," +
                    COLUMN_NODE_ID + " TEXT," +
                    COLUMN_CRYPTO + " TEXT," +
                    COLUMN_PACKAGE_NAME + " TEXT," +
                    COLUMN_IS_MIGRATING + " INTEGER DEFAULT 0," +
                    COLUMN_DATA_ITEM_SYNC_ENABLED + " INTEGER DEFAULT 1," +
                    COLUMN_RESTRICTIONS + " BLOB," +
                    COLUMN_REMOVE_CONNECTION_WHEN_BOND_REMOVED + " INTEGER DEFAULT 1," +
                    COLUMN_CONNECTION_DELAY_FILTERS + " BLOB," +
                    COLUMN_MAX_SUPPORTED_REMOTE_ANDROID_SDK + " INTEGER DEFAULT 0," +
                    " UNIQUE(" + COLUMN_NAME + ") ON CONFLICT REPLACE);");
        } catch (SQLException e) {
            Log.e(TAG, "Error creating database", e);
            throw e;
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            if (oldVersion < 2) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;",
                        TABLE_NAME, COLUMN_NODE_ID));
                oldVersion = 2;
            }

            if (oldVersion < 3) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;",
                        TABLE_NAME, COLUMN_CRYPTO));
                oldVersion = 3;
            }

            if (oldVersion < 4) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;",
                        TABLE_NAME, COLUMN_PACKAGE_NAME));
                oldVersion = 4;
            }

            if (oldVersion < 5) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;",
                        TABLE_NAME, COLUMN_ALLOWED_CONFIG_PACKAGES));
                oldVersion = 5;
            }

            if (oldVersion < 6) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 0;",
                        TABLE_NAME, COLUMN_IS_MIGRATING));
                oldVersion = 6;
            }

            if (oldVersion < 7) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;",
                        TABLE_NAME, COLUMN_DATA_ITEM_SYNC_ENABLED));
                oldVersion = 7;
            }

            if (oldVersion < 8) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s BLOB;",
                        TABLE_NAME, COLUMN_RESTRICTIONS));
                oldVersion = 8;
            }

            if (oldVersion < 9) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;",
                        TABLE_NAME, COLUMN_REMOVE_CONNECTION_WHEN_BOND_REMOVED));
                oldVersion = 9;
            }

            if (oldVersion < 10) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s BLOB;",
                        TABLE_NAME, COLUMN_CONNECTION_DELAY_FILTERS));
                oldVersion = 10;
            }

            if (oldVersion < 11) {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 0;",
                        TABLE_NAME, COLUMN_MAX_SUPPORTED_REMOTE_ANDROID_SDK));
            }

        } catch (SQLException e) {
            Log.e(TAG, "Error upgrading database", e);
            throw e;
        }
    }

    private static ConnectionConfiguration configFromCursor(final Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        String pairedBtAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAIRED_BT_ADDRESS));
        int connectionType = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CONNECTION_TYPE));
        int role = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
        int enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CONNECTION_ENABLED));
        String nodeId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NODE_ID));

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
            contentValues.put(COLUMN_NAME, config.name);
        } else if (config.role == 2) {
            contentValues.put(COLUMN_NAME, "server");
        } else {
            contentValues.put(COLUMN_NAME, NULL_STRING);
        }

        if (config.address != null) {
            contentValues.put(COLUMN_PAIRED_BT_ADDRESS, config.address);
        } else {
            contentValues.put(COLUMN_PAIRED_BT_ADDRESS, NULL_STRING);
        }

        contentValues.put(COLUMN_CONNECTION_TYPE, config.type);
        contentValues.put(COLUMN_ROLE, config.role);
        contentValues.put(COLUMN_CONNECTION_ENABLED, config.enabled ? 1 : 0);
        contentValues.put(COLUMN_NODE_ID, config.nodeId);

        if (oldNodeId == null) {
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        } else {
            getWritableDatabase().update(TABLE_NAME, contentValues, COLUMN_NODE_ID + "=?", new String[]{oldNodeId});
        }
    }

    public ConnectionConfiguration[] getAllConfigurations() {
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
        if (cursor != null) {
            try {
                List<ConnectionConfiguration> configurations = new ArrayList<>();
                while (cursor.moveToNext()) {
                    configurations.add(configFromCursor(cursor));
                }
                return configurations.toArray(new ConnectionConfiguration[0]);
            } finally {
                cursor.close();
            }
        }
        return new ConnectionConfiguration[0];
    }

    public void setEnabledState(String packageName, boolean enabled) {
        Log.d(TAG, "setEnabledState(" + packageName + ", " + enabled + ")");

        ConnectionConfiguration oldConfig = getConfiguration(packageName);
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONNECTION_ENABLED, enabled ? 1 : 0);
        getWritableDatabase().updateWithOnConflict(TABLE_NAME, values, BY_NAME, new String[]{packageName != null ? packageName : NULL_STRING}, SQLiteDatabase.CONFLICT_REPLACE);

        ConnectionConfiguration config = getConfiguration(packageName);
        Log.d(TAG, "setConnectionEnabled configName=" + packageName + ", connectionEnabled=" + enabled + ", originalConfig=" + oldConfig + ", updatedConfig=" + config);

        switch (config.type) {
            case TYPE_CLOUD:
                return;  // abort on cloud type
            case TYPE_BLUETOOTH_RFCOMM:
            case TYPE_BLUETOOTH_L2CAP:
                handleLegacy(config, enabled);
                break;
            case TYPE_NETWORK:
                handleNetwork(config, enabled);
                break;
            case TYPE_BLE:
                handleBle(config, enabled);
                break;
            default:
                Log.w(TAG, "unimplemented config type: " + config.type);
        }
    }

    private void handleBle(ConnectionConfiguration config, boolean enabled) {
        if (config.role == ROLE_CLIENT) {
            if (enabled) {
                // add ble client config
            } else {
                // remove ble client config
            }
        } else if (config.role == ROLE_SERVER) {
            // update ble server config
        }
    }

    private void handleNetwork(ConnectionConfiguration config, boolean enabled) {
        if (enabled) {
            // initialize new network service
        } else {
            // close network service
        }
    }

    private void handleLegacy(ConnectionConfiguration config, boolean enabled) {
        if (config.role == ROLE_CLIENT) {
            if (enabled) {
                // Add/Retry bluetooth client config
            } else {
                // remove bluetooth client config
            }
        } else if (config.role == ROLE_SERVER) {
            if (enabled) {
                // add bluetooth server config
            } else {
                // remove bluetooth server config
            }
        }
    }

    public int deleteConfiguration(String name) {
        return getWritableDatabase().delete(TABLE_NAME, BY_NAME, new String[]{name});
    }
}
