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

package org.microg.gms.wearable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationDatabaseHelper extends SQLiteOpenHelper {

    public static final String NULL_STRING = "NULL_STRING";

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
        String name = cursor.getString(cursor.getColumnIndex("name"));
        String pairedBtAddress = cursor.getString(cursor.getColumnIndex("pairedBtAddress"));
        int connectionType = cursor.getInt(cursor.getColumnIndex("connectionType"));
        int role = cursor.getInt(cursor.getColumnIndex("role"));
        int enabled = cursor.getInt(cursor.getColumnIndex("connectionEnabled"));
        String nodeId = cursor.getString(cursor.getColumnIndex("nodeId"));
        if (NULL_STRING.equals(name)) name = null;
        if (NULL_STRING.equals(pairedBtAddress)) pairedBtAddress = null;
        return new ConnectionConfiguration(name, pairedBtAddress, connectionType, role, enabled > 0, nodeId);
    }

    public ConnectionConfiguration getConfiguration(String name) {
        Cursor cursor = getReadableDatabase().query("connectionConfigurations", null, "name=?", new String[]{name}, null, null, null);
        ConnectionConfiguration config = null;
        if (cursor != null) {
            if (cursor.moveToNext())
                config = configFromCursor(cursor);
            cursor.close();
        }
        return config;
    }

    public void putConfiguration(ConnectionConfiguration config) {
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
        contentValues.put("connectionEnabled", true);
        contentValues.put("nodeId", config.nodeId);
        getWritableDatabase().insert("connectionConfigurations", null, contentValues);
    }

    public ConnectionConfiguration[] getAllConfigurations() {
        Cursor cursor = getReadableDatabase().query("connectionConfigurations", null, null, null, null, null, null);
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
}
