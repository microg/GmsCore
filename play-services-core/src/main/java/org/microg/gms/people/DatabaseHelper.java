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

package org.microg.gms.people;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "pluscontacts.db";
    private static final String CREATE_OWNERS = "CREATE TABLE owners (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "account_name TEXT NOT NULL UNIQUE," + // example@gmail.com
            "gaia_id TEXT," + // 123456789123456789123
            "page_gaia_id TEXT," +
            "display_name TEXT," + // firstName lastName
            "avatar TEXT," + // url (relative?)
            "cover_photo_url TEXT," + // cover url (relative?)
            "cover_photo_height INTEGER NOT NULL DEFAULT 0," +
            "cover_photo_width INTEGER NOT NULL DEFAULT 0," +
            "cover_photo_id TEXT," +
            "last_sync_start_time INTEGER NOT NULL DEFAULT 0," + // timestamp
            "last_sync_finish_time INTEGER NOT NULL DEFAULT 0," + // timestamp
            "last_sync_status INTEGER NOT NULL DEFAULT 0," + // eg. 2
            "last_successful_sync_time INTEGER NOT NULL DEFAULT 0," + // timestamp
            "sync_to_contacts INTEGER NOT NULL DEFAULT 0," + // 0
            "is_dasher INTEGER NOT NULL DEFAULT 0," + // 0
            "dasher_domain TEXT," +
            "etag TEXT," +
            "sync_circles_to_contacts INTEGER NOT NULL DEFAULT 0," + // 0
            "sync_evergreen_to_contacts INTEGER NOT NULL DEFAULT 0," + // 0
            "last_full_people_sync_time INTEGER NOT NULL DEFAULT 0);"; // timestamp
    public static final String OWNERS_TABLE = "owners";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_OWNERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // silently accept
    }

    public Cursor getOwners() {
        return getReadableDatabase().query(OWNERS_TABLE, null, null, null, null, null, null);
    }

    public void putOwner(ContentValues contentValues) {
        getWritableDatabase().insertWithOnConflict(OWNERS_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getOwner(String accountName) {
        return getReadableDatabase().query(OWNERS_TABLE, null, "account_name=?", new String[]{accountName}, null, null, null);
    }
}
