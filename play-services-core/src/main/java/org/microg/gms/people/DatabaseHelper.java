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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 5;
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
            "last_full_people_sync_time INTEGER NOT NULL DEFAULT 0," + // timestamp
            "is_active_plus_account INTEGER NOT NULL DEFAULT 0," + // 0
            "sync_me_to_contacts INTEGER NOT NULL DEFAULT 0," +
            "given_name TEXT," +
            "family_name TEXT," +
            "contacts_backup_and_sync INTEGER NOT NULL DEFAULT 0);"; // 0
    private static final String CREATE_CIRCLES = "CREATE TABLE circles (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "owner_id INTEGER NOT NULL," +
            "circle_id TEXT NOT NULL," +
            "name TEXT,sort_key TEXT," +
            "type INTEGER NOT NULL," +
            "for_sharing INTEGER NOT NULL DEFAULT 0," +
            "people_count INTEGER NOT NULL DEFAULT -1," +
            "client_policies INTEGER NOT NULL DEFAULT 0," +
            "etag TEXT,last_modified INTEGER NOT NULL DEFAULT 0," +
            "sync_to_contacts INTEGER NOT NULL DEFAULT 0," +
            "UNIQUE (owner_id,circle_id)," +
            "FOREIGN KEY (owner_id) REFERENCES owners(_id) ON DELETE CASCADE);";
    private static final String ALTER_OWNERS_3_1 = "ALTER TABLE owners ADD COLUMN is_active_plus_account INTEGER NOT NULL DEFAULT 0;";
    private static final String ALTER_OWNERS_3_2 = "ALTER TABLE owners ADD COLUMN sync_me_to_contacts INTEGER NOT NULL DEFAULT 0;";
    private static final String ALTER_OWNERS_4_1 = "ALTER TABLE owners ADD COLUMN given_name TEXT;";
    private static final String ALTER_OWNERS_4_2 = "ALTER TABLE owners ADD COLUMN family_name TEXT;";
    private static final String ALTER_OWNERS_5_1 = "ALTER TABLE owners ADD COLUMN contacts_backup_and_sync INTEGER NOT NULL DEFAULT 0;";
    public static final String OWNERS_TABLE = "owners";
    public static final String CIRCLES_TABLE = "circles";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_OWNERS);
        db.execSQL(CREATE_CIRCLES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL(CREATE_CIRCLES);
        if (oldVersion < 3) {
            db.execSQL(ALTER_OWNERS_3_1);
            db.execSQL(ALTER_OWNERS_3_2);
        }
        if (oldVersion < 4) {
            db.execSQL(ALTER_OWNERS_4_1);
            db.execSQL(ALTER_OWNERS_4_2);
        }
        if (oldVersion < 5) {
            db.execSQL(ALTER_OWNERS_5_1);
        }
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
        close();
    }

    public Cursor getOwner(String accountName) {
        return getReadableDatabase().query(OWNERS_TABLE, null, "account_name=?", new String[]{accountName}, null, null, null);
    }

    public Cursor getCircles(int ownerId, String circleId, int type) {
        return getReadableDatabase().query(CIRCLES_TABLE, null,
                "owner_id=?1 AND (circle_id = ?2 OR ?2 = '') AND (type = ?3 OR ?3 = -999 OR (?3 = -998 AND type = -1))",
                new String[]{Integer.toString(ownerId), circleId != null ? circleId : "", Integer.toString(type)}, null, null, null);
    }
}
