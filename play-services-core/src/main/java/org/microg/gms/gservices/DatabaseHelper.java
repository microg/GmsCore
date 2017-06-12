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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 3;
    private static final int DB_VERSION_OLD = 1;
    public static final String DB_NAME = "gservices.db";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE main (name TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE overrides (name TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE saved_system (name TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE saved_secure (name TEXT PRIMARY KEY, value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == DB_VERSION_OLD) {
            db.execSQL("DROP TABLE IF EXISTS main");
            db.execSQL("DROP TABLE IF EXISTS overrides");
            onCreate(db);
        }
        db.setVersion(newVersion);
    }

    public String get(String name) {
        String result = null;
        Cursor cursor = getReadableDatabase().query("overrides", new String[]{"value"}, "name=?",
                new String[]{name}, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
            cursor.close();
        }
        if (result != null) return result;
        cursor = getReadableDatabase().query("main", new String[]{"value"}, "name=?",
                new String[]{name}, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
            cursor.close();
        }
        return result;
    }

    public Map<String, String> search(String search) {
        Map<String, String> map = new HashMap<String, String>();
        Cursor cursor = getReadableDatabase().query("overrides", new String[]{"name", "value"},
                "name LIKE ?", new String[]{search}, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                map.put(cursor.getString(0), cursor.getString(1));
            }
            cursor.close();
        }
        cursor = getReadableDatabase().query("main", new String[]{"name", "value"},
                "name LIKE ?", new String[]{search}, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                if (!map.containsKey(cursor.getString(0)))
                    map.put(cursor.getString(0), cursor.getString(1));
            }
            cursor.close();
        }
        return map;

    }

    public void put(String table, ContentValues values) {
        getWritableDatabase().insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
