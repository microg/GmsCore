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
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.Asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "GmsWearNodeDB";

    private static final String DB_NAME = "node.db";
    private static final String[] GDIBHAP_FIELDS = new String[]{"dataitems_id", "packageName", "signatureDigest", "host", "path", "seqId", "deleted", "sourceNode", "data", "timestampMs", "assetsPresent", "assetname", "assets_digest", "v1SourceNode", "v1SeqId"};
    private static final int VERSION = 9;

    private ClockworkNodePreferences clockworkNodePreferences;

    public NodeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        clockworkNodePreferences = new ClockworkNodePreferences(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE appkeys(_id INTEGER PRIMARY KEY AUTOINCREMENT,packageName TEXT NOT NULL,signatureDigest TEXT NOT NULL);");
        db.execSQL("CREATE TABLE dataitems(_id INTEGER PRIMARY KEY AUTOINCREMENT, appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), host TEXT NOT NULL, path TEXT NOT NULL, seqId INTEGER NOT NULL, deleted INTEGER NOT NULL, sourceNode TEXT NOT NULL, data BLOB, timestampMs INTEGER NOT NULL, assetsPresent INTEGER NOT NULL, v1SourceNode TEXT NOT NULL, v1SeqId INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE assets(digest TEXT PRIMARY KEY, dataPresent INTEGER NOT NULL DEFAULT 0, timestampMs INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE assetrefs(assetname TEXT NOT NULL, dataitems_id INTEGER NOT NULL REFERENCES dataitems(_id), assets_digest TEXT NOT NULL REFERENCES assets(digest));");
        db.execSQL("CREATE TABLE assetsacls(appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), assets_digest TEXT NOT NULL);");
        db.execSQL("CREATE TABLE nodeinfo(node TEXT NOT NULL PRIMARY KEY, seqId INTEGER, lastActivityMs INTEGER);");
        db.execSQL("CREATE VIEW appKeyDataItems AS SELECT appkeys._id AS appkeys_id, appkeys.packageName AS packageName, appkeys.signatureDigest AS signatureDigest, dataitems._id AS dataitems_id, dataitems.host AS host, dataitems.path AS path, dataitems.seqId AS seqId, dataitems.deleted AS deleted, dataitems.sourceNode AS sourceNode, dataitems.data AS data, dataitems.timestampMs AS timestampMs, dataitems.assetsPresent AS assetsPresent, dataitems.v1SourceNode AS v1SourceNode, dataitems.v1SeqId AS v1SeqId FROM appkeys, dataitems WHERE appkeys._id=dataitems.appkeys_id");
        db.execSQL("CREATE VIEW appKeyAcls AS SELECT appkeys._id AS appkeys_id, appkeys.packageName AS packageName, appkeys.signatureDigest AS signatureDigest, assetsacls.assets_digest AS assets_digest FROM appkeys, assetsacls WHERE _id=appkeys_id");
        db.execSQL("CREATE VIEW dataItemsAndAssets AS SELECT appKeyDataItems.packageName AS packageName, appKeyDataItems.signatureDigest AS signatureDigest, appKeyDataItems.dataitems_id AS dataitems_id, appKeyDataItems.host AS host, appKeyDataItems.path AS path, appKeyDataItems.seqId AS seqId, appKeyDataItems.deleted AS deleted, appKeyDataItems.sourceNode AS sourceNode, appKeyDataItems.data AS data, appKeyDataItems.timestampMs AS timestampMs, appKeyDataItems.assetsPresent AS assetsPresent, assetrefs.assetname AS assetname, assetrefs.assets_digest AS assets_digest, appKeyDataItems.v1SourceNode AS v1SourceNode, appKeyDataItems.v1SeqId AS v1SeqId FROM appKeyDataItems LEFT OUTER JOIN assetrefs ON appKeyDataItems.dataitems_id=assetrefs.dataitems_id");
        db.execSQL("CREATE VIEW assetsReadyStatus AS SELECT  dataitems_id AS dataitems_id,  COUNT(*) = SUM(dataPresent) AS nowReady,  assetsPresent AS markedReady FROM assetrefs, dataitems LEFT OUTER JOIN assets ON  assetrefs.assets_digest = assets.digest WHERE assetrefs.dataitems_id=dataitems._id GROUP BY dataitems_id;");
        db.execSQL("CREATE UNIQUE INDEX appkeys_NAME_AND_SIG ON appkeys(packageName,signatureDigest);");
        db.execSQL("CREATE UNIQUE INDEX assetrefs_ASSET_REFS ON assetrefs(assets_digest,dataitems_id,assetname);");
        db.execSQL("CREATE UNIQUE INDEX assets_DIGEST ON assets(digest);");
        db.execSQL("CREATE UNIQUE INDEX assetsacls_APPKEY_AND_DIGEST ON assetsacls(appkeys_id,assets_digest);");
        db.execSQL("CREATE UNIQUE INDEX dataitems_APPKEY_HOST_AND_PATH ON dataitems(appkeys_id,host,path);");
        db.execSQL("CREATE UNIQUE INDEX dataitems_SOURCENODE_AND_SEQID ON dataitems(sourceNode,seqId);");
        db.execSQL("CREATE UNIQUE INDEX dataitems_SOURCENODE_DELETED_AND_SEQID ON dataitems(sourceNode,deleted,seqId);");
    }

    public synchronized Cursor getDataItemsForDataHolder(String packageName, String signatureDigest) {
        return getDataItemsForDataHolderByHostAndPath(packageName, signatureDigest, null, null);
    }

    public synchronized Cursor getDataItemsForDataHolderByHostAndPath(String packageName, String signatureDigest, String host, String path) {
        String[] params;
        String selection;
        if (path == null) {
            params = new String[]{packageName, signatureDigest};
            selection = "packageName = ? AND signatureDigest = ?";
        } else if (TextUtils.isEmpty(host)) {
            if (path.endsWith("/")) path = path + "%";
            path = path.replace("*", "%");
            params = new String[]{packageName, signatureDigest, path};
            selection = "packageName = ? AND signatureDigest = ? AND path LIKE ?";
        } else {
            if (path.endsWith("/")) path = path + "%";
            path = path.replace("*", "%");
            host = host.replace("*", "%");
            params = new String[]{packageName, signatureDigest, host, path};
            selection = "packageName = ? AND signatureDigest = ? AND host = ? AND path LIKE ?";
        }
        selection += " AND deleted=0 AND assetsPresent !=0";
        return getReadableDatabase().rawQuery("SELECT host AS host,path AS path,data AS data,\'\' AS tags,assetname AS asset_key,assets_digest AS asset_id FROM dataItemsAndAssets WHERE " + selection, params);
    }

    public synchronized Cursor getDataItemsByHostAndPath(String packageName, String signatureDigest, String host, String path) {
        Log.d(TAG, "getDataItemsByHostAndPath: " + packageName + ", " + signatureDigest + ", " + host + ", " + path);
        return getDataItemsByHostAndPath(getReadableDatabase(), packageName, signatureDigest, host, path);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != VERSION) {
            // TODO: Upgrade not supported, cleaning up
            db.execSQL("DROP TABLE IF EXISTS appkeys;");
            db.execSQL("DROP TABLE IF EXISTS dataitems;");
            db.execSQL("DROP TABLE IF EXISTS assets;");
            db.execSQL("DROP TABLE IF EXISTS assetrefs;");
            db.execSQL("DROP TABLE IF EXISTS assetsacls;");
            db.execSQL("DROP TABLE IF EXISTS nodeinfo;");
            db.execSQL("DROP VIEW IF EXISTS appKeyDataItems;");
            db.execSQL("DROP VIEW IF EXISTS appKeyAcls;");
            db.execSQL("DROP VIEW IF EXISTS dataItemsAndAssets;");
            db.execSQL("DROP VIEW IF EXISTS assetsReadyStatus;");
            onCreate(db);
        }
    }

    private static synchronized long getAppKey(SQLiteDatabase db, String packageName, String signatureDigest) {
        Cursor cursor = db.rawQuery("SELECT _id FROM appkeys WHERE packageName=? AND signatureDigest=?", new String[]{packageName, signatureDigest});
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
        ContentValues appKey = new ContentValues();
        appKey.put("packageName", packageName);
        appKey.put("signatureDigest", signatureDigest);
        return db.insert("appkeys", null, appKey);
    }

    public synchronized void putRecord(DataItemRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = getDataItemsByHostAndPath(db, record.packageName, record.signatureDigest, record.dataItem.host, record.dataItem.path);
        try {
            String key;
            if (cursor.moveToNext()) {
                // update
                key = cursor.getString(0);
                updateRecord(db, key, record);
            } else {
                // insert
                key = insertRecord(db, record);
            }
            if (record.assetsAreReady) {
                ContentValues update = new ContentValues();
                update.put("assetsPresent", 1);
                db.update("dataitems", update, "_id=?", new String[]{key});
            }
            db.setTransactionSuccessful();
        } finally {
            cursor.close();
        }
        db.endTransaction();
    }

    private static void updateRecord(SQLiteDatabase db, String key, DataItemRecord record) {
        ContentValues cv = record.toContentValues();
        db.update("dataitems", cv, "_id=?", new String[]{key});
        finishRecord(db, key, record);
    }

    private static String insertRecord(SQLiteDatabase db, DataItemRecord record) {
        ContentValues contentValues = record.toContentValues();
        contentValues.put("appkeys_id", getAppKey(db, record.packageName, record.signatureDigest));
        contentValues.put("host", record.dataItem.host);
        contentValues.put("path", record.dataItem.path);
        String key = Long.toString(db.insertWithOnConflict("dataitems", "host", contentValues, SQLiteDatabase.CONFLICT_REPLACE));
        return finishRecord(db, key, record);
    }

    private static String finishRecord(SQLiteDatabase db, String key, DataItemRecord record) {
        if (!record.deleted) {
            for (Map.Entry<String, Asset> asset : record.dataItem.getAssets().entrySet()) {
                ContentValues assetValues = new ContentValues();
                assetValues.put("assets_digest", asset.getValue().getDigest());
                assetValues.put("dataitems_id", key);
                assetValues.put("assetname", asset.getKey());
                db.insertWithOnConflict("assetrefs", "assetname", assetValues, SQLiteDatabase.CONFLICT_IGNORE);
            }
            Cursor status = db.query("assetsReadyStatus", new String[]{"nowReady"}, "dataitems_id=?", new String[]{key}, null, null, null);
            if (status.moveToNext()) {
                record.assetsAreReady = status.getLong(0) != 0;
            }
            status.close();
        } else {
            record.assetsAreReady = false;
        }
        return key;
    }

    private static Cursor getDataItemsByHostAndPath(SQLiteDatabase db, String packageName, String signatureDigest, String host, String path) {
        String[] params;
        String selection;
        if (path == null) {
            params = new String[]{packageName, signatureDigest};
            selection = "packageName =? AND signatureDigest =?";
        } else if (host == null) {
            params = new String[]{packageName, signatureDigest, path};
            selection = "packageName =? AND signatureDigest =? AND path =?";
        } else {
            params = new String[]{packageName, signatureDigest, host, path};
            selection = "packageName =? AND signatureDigest =? AND host =? AND path =?";
        }
        selection += " AND deleted=0";
        return db.query("dataItemsAndAssets", GDIBHAP_FIELDS, selection, params, null, null, "packageName, signatureDigest, host, path");
    }

    public Cursor getModifiedDataItems(final String nodeId, final long seqId, final boolean excludeDeleted) {
        String selection = "sourceNode =? AND seqId >?" + (excludeDeleted ? " AND deleted =0" : "");
        return getReadableDatabase().query("dataItemsAndAssets", GDIBHAP_FIELDS, selection, new String[]{nodeId, Long.toString(seqId)}, null, null, "seqId", null);
    }

    public synchronized List<DataItemRecord> deleteDataItems(String packageName, String signatureDigest, String host, String path) {
        List<DataItemRecord> updated = new ArrayList<DataItemRecord>();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = getDataItemsByHostAndPath(db, packageName, signatureDigest, host, path);
        while (cursor.moveToNext()) {
            DataItemRecord record = DataItemRecord.fromCursor(cursor);
            record.deleted = true;
            record.assetsAreReady = true;
            record.dataItem.data = null;
            record.seqId = clockworkNodePreferences.getNextSeqId();
            record.v1SeqId = record.seqId;
            updateRecord(db, cursor.getString(0), record);
            updated.add(record);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return updated;
    }

    public long getCurrentSeqId(String sourceNode) {
        if (TextUtils.isEmpty(sourceNode)) return 1;
        return getCurrentSeqId(getReadableDatabase(), sourceNode);
    }

    private long getCurrentSeqId(SQLiteDatabase db, String sourceNode) {
        Cursor cursor = db.query("dataItemsAndAssets", new String[]{"seqId"}, "sourceNode=?", new String[]{sourceNode}, null, null, "seqId DESC", "1");
        long res = 1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                res = cursor.getLong(0);
            }
            cursor.close();
        }
        return res;
    }

    public synchronized void putAsset(Asset asset, boolean dataPresent) {
        ContentValues cv = new ContentValues();
        cv.put("digest", asset.getDigest());
        cv.put("dataPresent", dataPresent ? 1 : 0);
        cv.put("timestampMs", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("assets", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void allowAssetAccess(String digest, String packageName, String signatureDigest) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("assets_digest", digest);
        cv.put("appkeys_id", getAppKey(db, packageName, signatureDigest));
        db.insertWithOnConflict("assetsacls", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor listMissingAssets() {
        return getReadableDatabase().query("dataItemsAndAssets", GDIBHAP_FIELDS, "assetsPresent = 0 AND assets_digest NOT NULL", null, null, null, "packageName, signatureDigest, host, path");
    }

    public boolean hasAsset(Asset asset) {
        Cursor cursor = getReadableDatabase().query("assets", new String[]{"dataPresent"}, "digest=?", new String[]{asset.getDigest()}, null, null, null);
        if (cursor == null) return false;
        try {
            return (cursor.moveToNext() && cursor.getInt(0) == 1);
        } finally {
            cursor.close();
        }
    }

    public synchronized void markAssetAsPresent(String digest) {
        ContentValues cv = new ContentValues();
        cv.put("dataPresent", 1);
        SQLiteDatabase db = getWritableDatabase();
        db.update("assets", cv, "digest=?", new String[]{digest});
        Cursor status = db.query("assetsReadyStatus", null, "nowReady != markedReady", null, null, null, null);
        while (status.moveToNext()) {
            cv = new ContentValues();
            cv.put("assetsPresent", status.getInt(status.getColumnIndexOrThrow("nowReady")));
            db.update("dataitems", cv, "_id=?", new String[]{Integer.toString(status.getInt(status.getColumnIndexOrThrow("dataitems_id")))});
        }
        status.close();
    }
}
