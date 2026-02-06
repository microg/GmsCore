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
    private static final int VERSION = 14;

    private final ClockworkNodePreferences clockworkNodePreferences;

    public NodeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        clockworkNodePreferences = new ClockworkNodePreferences(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE appkeys(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "packageName TEXT NOT NULL, " +
                "signatureDigest TEXT NOT NULL);");

        db.execSQL("CREATE TABLE dataitems(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id)," +
                "host TEXT NOT NULL, " +
                "path TEXT NOT NULL, " +
                "seqId INTEGER NOT NULL, " +
                "deleted INTEGER NOT NULL, " +
                "sourceNode TEXT NOT NULL, " +
                "data BLOB," +
                "timestampMs INTEGER NOT NULL, " +
                "assetsPresent INTEGER NOT NULL, " +
                "v1SourceNode TEXT NOT NULL, " +
                "v1SeqId INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE archiveDataItems(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "migratingNode TEXT NOT NULL, " +
                "appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), " +
                "path TEXT NOT NULL, " +
                "data BLOB, " +
                "timestampMs INTEGER NOT NULL, " +
                "assetsPresent INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE assets(" +
                "digest TEXT PRIMARY KEY, " +
                "dataPresent INTEGER NOT NULL DEFAULT 0, " +
                "timestampMs INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE assetrefs(" +
                "assetname TEXT NOT NULL, " +
                "dataitems_id INTEGER NOT NULL REFERENCES dataitems(_id), " +
                "assets_digest TEXT NOT NULL REFERENCES assets(digest));");

        db.execSQL("CREATE TABLE archiveAssetRefs(" +
                "assetname TEXT NOT NULL, " +
                "archiveDataItems_id INTEGER NOT NULL REFERENCES dataitems(_id), " +
                "assets_digest TEXT NOT NULL REFERENCES assets(digest));");

        db.execSQL("CREATE TABLE assetsacls(" +
                "appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), " +
                "assets_digest TEXT NOT NULL);");

        db.execSQL("CREATE TABLE nodeinfo(" +
                "node TEXT NOT NULL PRIMARY KEY, " +
                "seqId INTEGER, " +
                "lastActivityMs INTEGER, " +
                "migratingFrom TEXT DEFAULT NULL, " +
                "enrollmentId TEXT DEFAULT NULL);");

        db.execSQL("CREATE VIEW appKeyDataItems AS SELECT " +
                "appkeys._id AS appkeys_id, " +
                "appkeys.packageName AS packageName, " +
                "appkeys.signatureDigest AS signatureDigest, " +
                "dataitems._id AS dataitems_id, " +
                "dataitems.host AS host, " +
                "dataitems.path AS path, " +
                "dataitems.seqId AS seqId, " +
                "dataitems.deleted AS deleted, " +
                "dataitems.sourceNode AS sourceNode, " +
                "dataitems.data AS data, " +
                "dataitems.timestampMs AS timestampMs, " +
                "dataitems.assetsPresent AS assetsPresent, " +
                "dataitems.v1SourceNode AS v1SourceNode, " +
                "dataitems.v1SeqId AS v1SeqId " +
                "FROM appkeys, dataitems " +
                "WHERE appkeys._id=dataitems.appkeys_id");

        db.execSQL("CREATE VIEW appKeyAcls AS SELECT " +
                "appkeys._id AS appkeys_id, " +
                "appkeys.packageName AS packageName, " +
                "appkeys.signatureDigest AS signatureDigest, " +
                "assetsacls.assets_digest AS assets_digest " +
                "FROM appkeys, assetsacls " +
                "WHERE _id=appkeys_id");

        db.execSQL("CREATE VIEW dataItemsAndAssets AS SELECT " +
                "appKeyDataItems.packageName AS packageName, " +
                "appKeyDataItems.signatureDigest AS signatureDigest, " +
                "appKeyDataItems.dataitems_id AS dataitems_id, " +
                "appKeyDataItems.host AS host, " +
                "appKeyDataItems.path AS path, " +
                "appKeyDataItems.seqId AS seqId, " +
                "appKeyDataItems.deleted AS deleted, " +
                "appKeyDataItems.sourceNode AS sourceNode, " +
                "appKeyDataItems.data AS data, " +
                "appKeyDataItems.timestampMs AS timestampMs, " +
                "appKeyDataItems.assetsPresent AS assetsPresent, " +
                "assetrefs.assetname AS assetname, " +
                "assetrefs.assets_digest AS assets_digest, " +
                "appKeyDataItems.v1SourceNode AS v1SourceNode, " +
                "appKeyDataItems.v1SeqId AS v1SeqId " +
                "FROM appKeyDataItems " +
                "LEFT OUTER JOIN assetrefs ON appKeyDataItems.dataitems_id=assetrefs.dataitems_id");

        db.execSQL("CREATE VIEW assetsReadyStatus AS SELECT " +
                "dataitems_id AS dataitems_id, " +
                "COUNT(*) = SUM(dataPresent) AS nowReady, " +
                "assetsPresent AS markedReady " +
                "FROM assetrefs, dataitems " +
                "LEFT OUTER JOIN assets ON assetrefs.assets_digest = assets.digest " +
                "WHERE assetrefs.dataitems_id=dataitems._id " +
                "GROUP BY dataitems_id;");

        db.execSQL("CREATE VIEW appKeyArchiveDataItems AS SELECT " +
                "appkeys._id AS appkeys_id, " +
                "appkeys.packageName AS packageName, " +
                "appkeys.signatureDigest AS signatureDigest, " +
                "archiveDataItems._id AS archiveDataItems_id, " +
                "archiveDataItems.migratingNode AS migratingNode, " +
                "archiveDataItems.path AS path, " +
                "archiveDataItems.data AS data, " +
                "archiveDataItems.timestampMs AS timestampMs, " +
                "archiveDataItems.assetsPresent AS assetsPresent " +
                "FROM appkeys, archiveDataItems " +
                "WHERE appkeys._id = archiveDataItems.appkeys_id");

        db.execSQL("CREATE VIEW archiveDataItemsAndAssets AS SELECT " +
                "appKeyArchiveDataItems.appkeys_id AS appkeys_id, " +
                "appKeyArchiveDataItems.packageName AS packageName, " +
                "appKeyArchiveDataItems.signatureDigest AS signatureDigest, " +
                "appKeyArchiveDataItems.archiveDataItems_id AS archiveDataItems_id, " +
                "appKeyArchiveDataItems.migratingNode AS migratingNode, " +
                "appKeyArchiveDataItems.path AS path, " +
                "appKeyArchiveDataItems.data AS data, " +
                "appKeyArchiveDataItems.timestampMs AS timestampMs, " +
                "appKeyArchiveDataItems.assetsPresent AS assetsPresent, " +
                "archiveAssetRefs.assetname AS assetname, " +
                "archiveAssetRefs.assets_digest AS assets_digest " +
                "FROM appKeyArchiveDataItems " +
                "LEFT OUTER JOIN archiveAssetRefs ON appKeyArchiveDataItems.archiveDataItems_id = archiveAssetRefs.archiveDataItems_id");

        db.execSQL("CREATE VIEW archiveAssetsReadyStatus AS SELECT " +
                "archiveDataItems_id AS archiveDataItems_id, " +
                "COUNT(*) = SUM(dataPresent) AS nowReady, " +
                "assetsPresent AS markedReady " +
                "FROM archiveAssetRefs, archiveDataItems " +
                "LEFT OUTER JOIN assets ON archiveAssetRefs.assets_digest = assets.digest " +
                "WHERE archiveAssetRefs.archiveDataItems_id = archiveDataItems._id " +
                "GROUP BY archiveDataItems_id;");

        db.execSQL("CREATE UNIQUE INDEX appkeys_NAME_AND_SIG ON appkeys(" +
                "packageName, signatureDigest);");

        db.execSQL("CREATE UNIQUE INDEX assetrefs_ASSET_REFS ON assetrefs(" +
                "assets_digest, dataitems_id, assetname);");

        db.execSQL("CREATE INDEX assetrefs_DATAITEM_ID ON assetrefs(dataitems_id);");

        db.execSQL("CREATE UNIQUE INDEX archiveAssetRefs_ASSET_REFS ON archiveAssetRefs(" +
                "assets_digest, archiveDataItems_id, assetname);");

        db.execSQL("CREATE INDEX archiveAssetRefs_DATAITEM_ID ON archiveAssetRefs(archiveDataItems_id);");

        db.execSQL("CREATE UNIQUE INDEX assets_DIGEST ON assets(digest);");

        db.execSQL("CREATE UNIQUE INDEX assetsacls_APPKEY_AND_DIGEST ON assetsacls(" +
                "appkeys_id, assets_digest);");

        db.execSQL("CREATE UNIQUE INDEX dataitems_APPPKEY_PATH_AND_HOST ON dataitems(" +
                "appkeys_id, path, host);");

        db.execSQL("CREATE UNIQUE INDEX dataitems_SOURCENODE_AND_SEQID ON dataitems(" +
                "sourceNode, seqId);");
        db.execSQL("CREATE UNIQUE INDEX dataitems_SOURCENODE_DELETED_AND_SEQID ON dataitems(" +
                "sourceNode, deleted, seqId);");

        db.execSQL("CREATE UNIQUE INDEX archiveDataItems_NODE_APPPKEY_PATH ON archiveDataItems(" +
                "migratingNode, appkeys_id, path);");
    }

    public synchronized Cursor getDataItemsForDataHolder(String packageName, String signatureDigest) {
        return getDataItemsForDataHolderByHostAndPath(packageName, signatureDigest, null, null);
    }

    public synchronized Cursor getDataItemsForDataHolderByHostAndPath(String packageName, String signatureDigest, String host, String path) {
        SQLiteDatabase db = getReadableDatabase();

        String[] params;
        String selection;

        if (path == null) {
            params = new String[]{packageName, signatureDigest};
            selection = "a.packageName = ? AND a.signatureDigest = ?";
        } else if (TextUtils.isEmpty(host)) {
            if (path.endsWith("/")) path = path + "%";
            path = path.replace("*", "%");
            params = new String[]{packageName, signatureDigest, path};
            selection = "a.packageName = ? AND a.signatureDigest = ? AND d.path LIKE ?";
        } else {
            if (path.endsWith("/")) path = path + "%";
            path = path.replace("*", "%");
            host = host.replace("*", "%");
            params = new String[]{packageName, signatureDigest, host, path};

            selection = "a.packageName = ? AND a.signatureDigest = ? " +
                    "AND d.host LIKE ? AND d.path LIKE ?";
        }

        selection += " AND d.deleted = 0 AND d.assetsPresent != 0";

        String query =
                "SELECT " +
                        "d._id AS _id, " +
                        "d.host AS host, " +
                        "d.path AS path, " +
                        "d.data AS data, " +
                        "'' AS tags, " +
                        "d.seqId AS seqId, " +
                        "d.timestampMs AS timestampMs, " +
                        "d.deleted AS deleted, " +
                        "d.sourceNode AS sourceNode, " +
                        "d.assetsPresent AS assetsPresent, " +
                        "a.packageName AS packageName, " +
                        "a.signatureDigest AS signatureDigest " +
                        "FROM dataitems d " +
                        "JOIN appkeys a ON d.appkeys_id = a._id " +
                        "WHERE " + selection;

        return db.rawQuery(query, params);
    }

    public synchronized Cursor getDataItemsByHostAndPath(String packageName, String signatureDigest, String host, String path) {
        Log.d(TAG, "getDataItemsByHostAndPath: " + packageName + ", " + signatureDigest + ", " + host + ", " + path);
        return getDataItemsByHostAndPath(getReadableDatabase(), packageName, signatureDigest, host, path);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != VERSION) {
            // just recreate everything
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

        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }

        ContentValues appKey = new ContentValues();
        appKey.put("packageName", packageName);
        appKey.put("signatureDigest", signatureDigest);
        return db.insert("appkeys", null, appKey);
    }

    public synchronized void putRecord(DataItemRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long appKeyId = getAppKey(db, record.packageName, record.signatureDigest);

            ContentValues cv = new ContentValues();
            cv.put("appkeys_id", appKeyId);
            cv.put("host", record.dataItem.host);
            cv.put("path", record.dataItem.path);
            cv.put("seqId", record.seqId);
            cv.put("deleted", record.deleted ? 1 : 0);
            cv.put("sourceNode", record.source);
            cv.put("data", record.dataItem.data);
            cv.put("timestampMs", System.currentTimeMillis());
            cv.put("assetsPresent", record.assetsAreReady ? 1 : 0);
            cv.put("v1SourceNode", record.source);
            cv.put("v1SeqId", record.v1SeqId != 0 ? record.v1SeqId : record.seqId);

            long dataItemId = db.insertWithOnConflict("dataitems", null, cv,
                    SQLiteDatabase.CONFLICT_REPLACE);

            db.delete("assetrefs", "dataitems_id=?",
                    new String[]{String.valueOf(dataItemId)});

            if (record.dataItem.getAssets() != null && !record.dataItem.getAssets().isEmpty()) {
                for (Map.Entry<String, Asset> entry : record.dataItem.getAssets().entrySet()) {
                    ContentValues assetRef = new ContentValues();
                    assetRef.put("dataitems_id", dataItemId);
                    assetRef.put("assetname", entry.getKey());
                    assetRef.put("assets_digest", entry.getValue().getDigest());

                    db.insertWithOnConflict("assetrefs", null, assetRef,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error in putRecord", e);
        } finally {
            db.endTransaction();
        }
    }

    private static void updateRecord(SQLiteDatabase db, String dataItemId, DataItemRecord record) {
        ContentValues cv = new ContentValues();
        cv.put("seqId", record.seqId);
        cv.put("deleted", record.deleted ? 1 : 0);
        cv.put("sourceNode", record.source);
        cv.put("data", record.dataItem.data);
        cv.put("timestampMs", System.currentTimeMillis());
        cv.put("assetsPresent", record.assetsAreReady ? 1 : 0);
        cv.put("v1SourceNode", record.source);
        cv.put("v1SeqId", record.v1SeqId != 0 ? record.v1SeqId : record.seqId);
        db.update("dataitems", cv, "_id=?", new String[]{dataItemId});
        db.delete("assetrefs", "dataitems_id=?", new String[]{dataItemId});

        if (record.dataItem.getAssets() != null && !record.dataItem.getAssets().isEmpty()) {
            for (Map.Entry<String, Asset> entry : record.dataItem.getAssets().entrySet()) {
                ContentValues assetRef = new ContentValues();
                assetRef.put("dataitems_id", Long.parseLong(dataItemId));
                assetRef.put("assetname", entry.getKey());
                assetRef.put("assets_digest", entry.getValue().getDigest());

                db.insertWithOnConflict("assetrefs", null, assetRef,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        }

    }

    private static String insertRecord(SQLiteDatabase db, DataItemRecord record) {
        long appKeyId = getAppKey(db, record.packageName, record.signatureDigest);

        ContentValues cv = new ContentValues();
        cv.put("appkeys_id", appKeyId);
        cv.put("host", record.dataItem.host);
        cv.put("path", record.dataItem.path);
        cv.put("seqId", record.seqId);
        cv.put("deleted", record.deleted ? 1 : 0);
        cv.put("sourceNode", record.source);
        cv.put("data", record.dataItem.data);
        cv.put("timestampMs", System.currentTimeMillis());
        cv.put("assetsPresent", record.assetsAreReady ? 1 : 0);
        cv.put("v1SourceNode", record.source);
        cv.put("v1SeqId", record.v1SeqId != 0 ? record.v1SeqId : record.seqId);

        long dataItemId = db.insertWithOnConflict("dataitems", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);

        db.delete("assetrefs", "dataitems_id=?",
                new String[]{String.valueOf(dataItemId)});

        if (record.dataItem.getAssets() != null && !record.dataItem.getAssets().isEmpty()) {
            for (Map.Entry<String, Asset> entry : record.dataItem.getAssets().entrySet()) {
                ContentValues assetRef = new ContentValues();
                assetRef.put("dataitems_id", dataItemId);
                assetRef.put("assetname", entry.getKey());
                assetRef.put("assets_digest", entry.getValue().getDigest());

                db.insertWithOnConflict("assetrefs", null, assetRef,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        }

        return String.valueOf(dataItemId);
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
            Cursor status = db.query("assetsReadyStatus",
                    new String[]{"nowReady"}, "dataitems_id=?",
                    new String[]{key}, null, null, null);

            if (status.moveToNext()) {
                record.assetsAreReady = status.getLong(0) != 0;
            }
            status.close();
        } else {
            record.assetsAreReady = false;
        }
        return key;
    }

    private static Cursor getDataItemsByHostAndPath(SQLiteDatabase db, String packageName,
                                                    String signatureDigest, String host, String path) {
        String[] params;
        String selection;

        if (path == null) {
            params = new String[]{packageName, signatureDigest};
            selection = "packageName = ? AND signatureDigest = ? AND deleted = 0";
        } else if (host == null) {
            String pathPattern = path;
            if (path.endsWith("/")) {
                pathPattern = path + "*";
            }
            pathPattern = pathPattern.replace("*", "%");

            params = new String[]{packageName, signatureDigest, pathPattern};
            selection = "packageName = ? AND signatureDigest = ? AND path LIKE ? AND deleted = 0";
        } else {
            String pathPattern = path;
            if (path.endsWith("/")) {
                pathPattern = path + "*";
            }
            pathPattern = pathPattern.replace("*", "%");

            String hostPattern = host.replace("*", "%");

            params = new String[]{packageName, signatureDigest, hostPattern, pathPattern};
            selection = "packageName = ? AND signatureDigest = ? AND host LIKE ? AND path LIKE ? AND deleted = 0";
        }

        return db.query("dataItemsAndAssets", GDIBHAP_FIELDS, selection, params,
                null, null, "packageName, signatureDigest, host, path");
    }

    public Cursor getModifiedDataItems(final String nodeId, final long seqId, final boolean excludeDeleted) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = "d.sourceNode = ? AND d.seqId > ?";
        if (excludeDeleted) {
            selection += " AND d.deleted = 0";
        }

        String query =
                "SELECT " +
                        "d._id AS dataitems_id, " +
                        "a.packageName AS packageName, " +
                        "a.signatureDigest AS signatureDigest, " +
                        "d.host AS host, " +
                        "d.path AS path, " +
                        "d.seqId AS seqId, " +
                        "d.deleted AS deleted, " +
                        "d.sourceNode AS sourceNode, " +
                        "d.data AS data, " +
                        "d.timestampMs AS timestampMs, " +
                        "d.assetsPresent AS assetsPresent, " +
                        "'' AS assetname, " +
                        "'' AS assets_digest, " +
                        "d.v1SourceNode AS v1SourceNode, " +
                        "d.v1SeqId AS v1SeqId " +
                        "FROM dataitems d " +
                        "JOIN appkeys a ON d.appkeys_id = a._id " +
                        "WHERE " + selection + " " +
                        "ORDER BY d.seqId";

        return db.rawQuery(query, new String[]{nodeId, Long.toString(seqId)});
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
        if (cursor.moveToFirst()) {
            res = cursor.getLong(0);
        }
        cursor.close();
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
        SQLiteDatabase db = getReadableDatabase();

        String query =
                "SELECT DISTINCT " +
                        "a.packageName AS packageName, " +
                        "a.signatureDigest AS signatureDigest, " +
                        "d.host AS host, " +
                        "d.path AS path, " +
                        "d.seqId AS seqId, " +
                        "d.deleted AS deleted, " +
                        "d.sourceNode AS sourceNode, " +
                        "d.data AS data, " +
                        "d.timestampMs AS timestampMs, " +
                        "d.assetsPresent AS assetsPresent, " +
                        "d.v1SourceNode AS v1SourceNode, " +
                        "d.v1SeqId AS v1SeqId, " +
                        "ar.assetname AS assetname, " +
                        "ar.assets_digest AS assets_digest " +
                        "FROM dataitems d " +
                        "JOIN appkeys a ON d.appkeys_id = a._id " +
                        "JOIN assetrefs ar ON d._id = ar.dataitems_id " +
                        "LEFT JOIN assets ast ON ar.assets_digest = ast.digest " +
                        "WHERE d.deleted = 0 " +
                        "AND (ast.dataPresent = 0 OR ast.dataPresent IS NULL) " +
                        "ORDER BY a.packageName, a.signatureDigest, d.host, d.path";


        return db.rawQuery(query, null);
    }

    public boolean hasAsset(Asset asset) {
        Cursor cursor = getReadableDatabase().query("assets",new String[]{"dataPresent"},
                "digest=?", new String[]{asset.getDigest()},
                null, null, null);
        try {
            return (cursor.moveToNext() && cursor.getInt(0) == 1);
        } finally {
            cursor.close();
        }
    }

    public void markAssetAsMissing(String digest, String packageName, String signatureDigest) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.query("assets", new String[]{"digest"},
                "digest = ?", new String[]{digest}, null, null, null);

        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put("digest", digest);
            values.put("dataPresent", 0);
            values.put("timestampMs", System.currentTimeMillis());
            db.insertWithOnConflict("assets", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        allowAssetAccess(digest, packageName, signatureDigest);
    }

    public Cursor getDataItemsWaitingForAsset(String digest) {
        SQLiteDatabase db = getReadableDatabase();

        String query = "SELECT " +
                "d._id AS dataitems_id, " +
                "a.packageName AS packageName, " +
                "a.signatureDigest AS signatureDigest, " +
                "d.host AS host, " +
                "d.path AS path, " +
                "d.seqId AS seqId, " +
                "d.deleted AS deleted, " +
                "d.sourceNode AS sourceNode, " +
                "d.data AS data, " +
                "d.timestampMs AS timestampMs, " +
                "d.assetsPresent AS assetsPresent, " +
                "ar.assetname AS assetname, " +
                "ar.assets_digest AS assets_digest, " +
                "d.v1SourceNode AS v1SourceNode, " +
                "d.v1SeqId AS v1SeqId " +
                "FROM dataitems d " +
                "JOIN appkeys a ON d.appkeys_id = a._id " +
                "JOIN assetrefs ar ON d._id = ar.dataitems_id " +
                "WHERE d.assetsPresent = 0 " +
                "AND ar.assets_digest = ?";

        return db.rawQuery(query, new String[]{digest});
    }

    public synchronized void updateAssetsReady(String uri, boolean ready) {
        ContentValues cv = new ContentValues();
        cv.put("assetsPresent", ready ? 1 : 0);

        getWritableDatabase().update("dataitems", cv,
                "host || path = ?", new String[]{uri});
    }

    public synchronized void markAssetAsPresent(String digest) {
        ContentValues cv = new ContentValues();
        cv.put("digest", digest);
        cv.put("dataPresent", 1);
        cv.put("timestampMs", System.currentTimeMillis());

        getWritableDatabase().insertWithOnConflict("assets", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

}
