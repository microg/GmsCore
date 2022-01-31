/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GcmDatabase extends SQLiteOpenHelper {
    private static final String TAG = GcmDatabase.class.getSimpleName();
    public static final String DB_NAME = "gcmstatus";
    private static final int DB_VERSION = 1;
    private static final String CREATE_TABLE_APPS = "CREATE TABLE apps (" +
            "package_name TEXT," +
            "last_error TEXT DEFAULT ''," +
            "last_message_timestamp INTEGER," +
            "total_message_count INTEGER," +
            "total_message_bytes INTEGER," +
            "allow_register INTEGER DEFAULT 1," +
            "wake_for_delivery INTEGER DEFAULT 1," +
            "PRIMARY KEY (package_name));";
    private static final String TABLE_APPS = "apps";
    private static final String FIELD_PACKAGE_NAME = "package_name";
    private static final String FIELD_LAST_ERROR = "last_error";
    private static final String FIELD_LAST_MESSAGE_TIMESTAMP = "last_message_timestamp";
    private static final String FIELD_TOTAL_MESSAGE_COUNT = "total_message_count";
    private static final String FIELD_TOTAL_MESSAGE_BYTES = "total_message_bytes";
    private static final String FIELD_ALLOW_REGISTER = "allow_register";
    private static final String FIELD_WAKE_FOR_DELIVERY = "wake_for_delivery";

    private static final String CREATE_TABLE_REGISTRATIONS = "CREATE TABLE registrations (" +
            "package_name TEXT," +
            "signature TEXT," +
            "timestamp INTEGER," +
            "register_id TEXT," +
            "PRIMARY KEY (package_name, signature));";
    private static final String TABLE_REGISTRATIONS = "registrations";
    private static final String FIELD_SIGNATURE = "signature";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_REGISTER_ID = "register_id";

    private final Context context;

    public GcmDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        if (Build.VERSION.SDK_INT >= 16) {
            this.setWriteAheadLoggingEnabled(true);
        }
    }

    public static class App {
        public final String packageName;
        public final String lastError;
        public final long lastMessageTimestamp;
        public final long totalMessageCount;
        public final long totalMessageBytes;
        public final boolean allowRegister;
        public final boolean wakeForDelivery;

        private App(Cursor cursor) {
            packageName = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_PACKAGE_NAME));
            lastError = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_LAST_ERROR));
            lastMessageTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_LAST_MESSAGE_TIMESTAMP));
            totalMessageCount = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_TOTAL_MESSAGE_COUNT));
            totalMessageBytes = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_TOTAL_MESSAGE_BYTES));
            allowRegister = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_ALLOW_REGISTER)) == 1;
            wakeForDelivery = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_WAKE_FOR_DELIVERY)) == 1;
        }

        public boolean hasError() {
            return !TextUtils.isEmpty(lastError);
        }
    }

    public static class Registration {
        public final String packageName;
        public final String signature;
        public final long timestamp;
        public final String registerId;

        public Registration(Cursor cursor) {
            packageName = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_PACKAGE_NAME));
            signature = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_SIGNATURE));
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_TIMESTAMP));
            registerId = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_REGISTER_ID));
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APPS);
        db.execSQL(CREATE_TABLE_REGISTRATIONS);
        importLegacyData(db);
    }

    public synchronized List<App> getAppList() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_APPS, null, null, null, null, null, null);
        if (cursor != null) {
            List<App> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                result.add(new App(cursor));
            }
            cursor.close();
            return result;
        }
        return Collections.emptyList();
    }

    public synchronized List<Registration> getRegistrationList() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_REGISTRATIONS, null, null, null, null, null, null);
        if (cursor != null) {
            List<Registration> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                result.add(new Registration(cursor));
            }
            cursor.close();
            return result;
        }
        return Collections.emptyList();
    }


    public synchronized List<Registration> getRegistrationsByApp(String packageName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_REGISTRATIONS, null, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName}, null, null, null);
        if (cursor != null) {
            List<Registration> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                result.add(new Registration(cursor));
            }
            cursor.close();
            return result;
        }
        return Collections.emptyList();
    }

    public synchronized void setAppAllowRegister(String packageName, boolean allowRegister) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(FIELD_ALLOW_REGISTER, allowRegister ? 1 : 0);
        db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
    }

    public synchronized void setAppWakeForDelivery(String packageName, boolean wakeForDelivery) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(FIELD_WAKE_FOR_DELIVERY, wakeForDelivery ? 1 : 0);
        db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
    }


    public synchronized void removeApp(String packageName) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_REGISTRATIONS, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
        db.delete(TABLE_APPS, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
    }

    public synchronized void noteAppRegistrationError(String packageName, String error) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(FIELD_LAST_ERROR, error);
        db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
    }

    public synchronized void noteAppKnown(String packageName, boolean allowRegister) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        App app = getApp(db, packageName);
        ContentValues cv = new ContentValues();
        cv.put(FIELD_ALLOW_REGISTER, allowRegister);
        if (app == null) {
            cv.put(FIELD_PACKAGE_NAME, packageName);
            db.insert(TABLE_APPS, null, cv);
        } else {
            db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public synchronized void noteAppMessage(String packageName, int numBytes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        App app = getApp(db, packageName);
        ContentValues cv = new ContentValues();
        cv.put(FIELD_LAST_MESSAGE_TIMESTAMP, System.currentTimeMillis());
        if (app == null) {
            cv.put(FIELD_PACKAGE_NAME, packageName);
            cv.put(FIELD_TOTAL_MESSAGE_COUNT, 1);
            cv.put(FIELD_TOTAL_MESSAGE_BYTES, numBytes);
            db.insert(TABLE_APPS, null, cv);
        } else {
            cv.put(FIELD_TOTAL_MESSAGE_COUNT, app.totalMessageCount + 1);
            cv.put(FIELD_TOTAL_MESSAGE_BYTES, app.totalMessageBytes + numBytes);
            db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public synchronized void noteAppRegistered(String packageName, String signature, String registrationId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        App app = getApp(db, packageName);
        if (app == null) {
            ContentValues cv = new ContentValues();
            cv.put(FIELD_PACKAGE_NAME, packageName);
            db.insert(TABLE_APPS, null, cv);
        } else {
            ContentValues cv = new ContentValues();
            cv.put(FIELD_LAST_ERROR, "");
            db.update(TABLE_APPS, cv, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName});
        }

        ContentValues cv = new ContentValues();
        cv.put(FIELD_PACKAGE_NAME, packageName);
        cv.put(FIELD_SIGNATURE, signature);
        cv.put(FIELD_REGISTER_ID, registrationId);
        cv.put(FIELD_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_REGISTRATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public synchronized void noteAppUnregistered(String packageName, String signature) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_REGISTRATIONS, FIELD_PACKAGE_NAME + " LIKE ? AND " + FIELD_SIGNATURE + " LIKE ?", new String[]{packageName, signature});
    }

    public App getApp(String packageName) {
        return getApp(getReadableDatabase(), packageName);
    }

    private App getApp(SQLiteDatabase db, String packageName) {
        Cursor cursor = db.query(TABLE_APPS, null, FIELD_PACKAGE_NAME + " LIKE ?", new String[]{packageName}, null, null, null, "1");
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return new App(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public Registration getRegistration(String packageName, String signature) {
        return getRegistration(getReadableDatabase(), packageName, signature);
    }

    private Registration getRegistration(SQLiteDatabase db, String packageName, String signature) {
        Cursor cursor = db.query(TABLE_REGISTRATIONS, null, FIELD_PACKAGE_NAME + " LIKE ? AND " + FIELD_SIGNATURE + " LIKE ?", new String[]{packageName, signature}, null, null, null, "1");
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return new Registration(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }


    @SuppressWarnings("deprecation")
    private void importLegacyData(SQLiteDatabase db) {
        db.beginTransaction();

        GcmLegacyData legacyData = new GcmLegacyData(context);
        for (GcmLegacyData.LegacyAppInfo appInfo : legacyData.getAppsInfo()) {
            ContentValues cv = new ContentValues();
            cv.put(FIELD_PACKAGE_NAME, appInfo.app);
            cv.put(FIELD_TOTAL_MESSAGE_COUNT, legacyData.getAppMessageCount(appInfo.app));
            cv.put(FIELD_LAST_ERROR, appInfo.hasUnregistrationError() ? "Unregistration error" : null);
            db.insert(TABLE_APPS, null, cv);
            cv.clear();
            if (appInfo.isRegistered()) {
                cv.put(FIELD_PACKAGE_NAME, appInfo.app);
                cv.put(FIELD_SIGNATURE, appInfo.appSignature);
                cv.put(FIELD_REGISTER_ID, appInfo.registerID);
                db.insert(TABLE_REGISTRATIONS, null, cv);
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Upgrades not supported");
    }

}
