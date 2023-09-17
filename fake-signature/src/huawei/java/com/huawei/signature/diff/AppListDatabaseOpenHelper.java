/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.huawei.signature.diff;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.microg.signature.fake.R;

public class AppListDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = AppListDatabaseOpenHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "app_list.db";
    public static final String TABLE_APPLIST = "applist";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FAKE = "fake";
    private static final int DATABASE_VERSION = 2;
    private static final String DROP_APP_LIST_TABLE = "DROP TABLE IF EXISTS " + TABLE_APPLIST;
    private static final String CREATE_APP_LIST_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_APPLIST + "(" +
            COLUMN_NAME + " VARCHAR(255) PRIMARY KEY, " +
            COLUMN_FAKE + " INTEGER CHECK(" + COLUMN_FAKE + " >= 0 and " + COLUMN_FAKE + " <= 1)" +
            ")";
    private final Context context;

    public AppListDatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(DROP_APP_LIST_TABLE);
        db.execSQL(CREATE_APP_LIST_TABLE);
        initData(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        initData(db);
    }

    private void initData(SQLiteDatabase db) {
        String[] wantFakeApps = context.getResources().getStringArray(R.array.signature_want_fake);
        String[] neverFakeApps = context.getResources().getStringArray(R.array.signature_never_fake);
        if (wantFakeApps.length == 0 && neverFakeApps.length == 0) {
            return;
        }
        for (String app : wantFakeApps) {
            db.insertWithOnConflict(TABLE_APPLIST, null, generateValues(app, true), SQLiteDatabase.CONFLICT_IGNORE);
        }
        for (String app : neverFakeApps) {
            db.insertWithOnConflict(TABLE_APPLIST, null, generateValues(app, false), SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private ContentValues generateValues(String packageName, boolean fake) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, packageName);
        contentValues.put(COLUMN_FAKE, fake ? 1 : 0);
        return contentValues;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}