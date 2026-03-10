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

package org.microg.gms.maps.vtm.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/*
 * TODO: Writing to cache should be protected, tiles should be downloaded by service instead of client app.
 */
public class SharedTileProvider extends ContentProvider {
    private static final String DB_NAME = "tilecache.db";
    public static final String PROVIDER_NAME = "org.microg.gms.maps.vtm.tile";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_NAME);

    private SQLiteHelper sqLiteHelper;

    public SharedTileProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/org.microg.gms.map.tile";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        sqLiteHelper.getWritableDatabase().insert("tiles", null, values);
        return PROVIDER_URI;
    }

    @Override
    public boolean onCreate() {
        sqLiteHelper = new SQLiteHelper(getContext(), DB_NAME);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return sqLiteHelper.getReadableDatabase().query("tiles", projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return sqLiteHelper.getWritableDatabase().update("tiles", values, selection, selectionArgs);
    }

    class SQLiteHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        private static final String TILE_SCHEMA = "CREATE TABLE tiles(x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,time LONG NOT NULL,last_access LONG NOT NULL,data BLOB,PRIMARY KEY(x,y,z));";

        public SQLiteHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TILE_SCHEMA);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS tiles");
            this.onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            this.onUpgrade(db, oldVersion, newVersion);
        }
    }
}
