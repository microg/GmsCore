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
import android.net.Uri;

import com.google.android.gms.wearable.Asset;

import java.util.Map;

public class NodeDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "node.db";
    private static final int VERSION = 7;

    public NodeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE appkeys(_id INTEGER PRIMARY KEY AUTOINCREMENT,packageName TEXT NOT NULL,signatureDigest TEXT NOT NULL);");
        db.execSQL("CREATE TABLE dataitems(_id INTEGER PRIMARY KEY AUTOINCREMENT, appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), host TEXT NOT NULL, path TEXT NOT NULL, seqId INTEGER NOT NULL, deleted INTEGER NOT NULL, sourceNode TEXT NOT NULL, data BLOB, timestampMs INTEGER NOT NULL, assetsPresent INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE assets(digest TEXT PRIMARY KEY, dataPresent INTEGER NOT NULL DEFAULT 0, timestampMs INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE assetrefs(assetname TEXT NOT NULL, dataitems_id INTEGER NOT NULL REFERENCES dataitems(_id), assets_digest TEXT NOT NULL REFERENCES assets(digest));");
        db.execSQL("CREATE TABLE assetsacls(appkeys_id INTEGER NOT NULL REFERENCES appkeys(_id), assets_digest TEXT NOT NULL);");
        db.execSQL("CREATE VIEW appKeyDataItems AS SELECT appkeys._id AS appkeys_id, appkeys.packageName AS packageName, appkeys.signatureDigest AS signatureDigest, dataitems._id AS dataitems_id, dataitems.host AS host, dataitems.path AS path, dataitems.seqId AS seqId, dataitems.deleted AS deleted, dataitems.sourceNode AS sourceNode, dataitems.data AS data, dataitems.timestampMs AS timestampMs, dataitems.assetsPresent AS assetsPresent FROM appkeys, dataitems WHERE appkeys._id=dataitems.appkeys_id");
        db.execSQL("CREATE VIEW appKeyAcls AS SELECT appkeys._id AS appkeys_id, appkeys.packageName AS packageName, appkeys.signatureDigest AS signatureDigest, assetsacls.assets_digest AS assets_digest FROM appkeys, assetsacls WHERE _id=appkeys_id");
        db.execSQL("CREATE VIEW dataItemsAndAssets AS SELECT appKeyDataItems.packageName AS packageName, appKeyDataItems.signatureDigest AS signatureDigest, appKeyDataItems.dataitems_id AS dataitems_id, appKeyDataItems.host AS host, appKeyDataItems.path AS path, appKeyDataItems.seqId AS seqId, appKeyDataItems.deleted AS deleted, appKeyDataItems.sourceNode AS sourceNode, appKeyDataItems.data AS data, appKeyDataItems.timestampMs AS timestampMs, appKeyDataItems.assetsPresent AS assetsPresent, assetrefs.assetname AS assetname, assetrefs.assets_digest AS assets_digest FROM appKeyDataItems LEFT OUTER JOIN assetrefs ON appKeyDataItems.dataitems_id=assetrefs.dataitems_id");
        db.execSQL("CREATE VIEW assetsReadyStatus AS SELECT  dataitems_id AS dataitems_id,  COUNT(*) = SUM(dataPresent) AS nowReady,  assetsPresent AS markedReady FROM assetrefs, dataitems LEFT OUTER JOIN assets ON  assetrefs.assets_digest = assets.digest WHERE assetrefs.dataitems_id=dataitems._id GROUP BY dataitems_id;");
        db.execSQL("CREATE UNIQUE INDEX appkeys_NAME_AND_SIG ON appkeys(packageName,signatureDigest);");
        db.execSQL("CREATE UNIQUE INDEX assetrefs_ASSET_REFS ON assetrefs(assets_digest,dataitems_id,assetname);");
        db.execSQL("CREATE UNIQUE INDEX assets_DIGEST ON assets(digest);");
        db.execSQL("CREATE UNIQUE INDEX assetsacls_APPKEY_AND_DIGEST ON assetsacls(appkeys_id,assets_digest);");
        db.execSQL("CREATE UNIQUE INDEX dataitems_APPKEY_HOST_AND_PATH ON dataitems(appkeys_id,host,path);");
    }

    public Cursor getDataItemsForDataHolder(String packageName, String signatureDigest) {
        return getDataItemsForDataHolderByHostAndPath(packageName, signatureDigest, null, null);
    }

    public Cursor getDataItemsForDataHolderByHostAndPath(String packageName, String signatureDigest, String host, String path) {
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
        selection += " AND deleted=0 AND assetsPresent !=0";
        return getReadableDatabase().rawQuery("SELECT host AS host,path AS path,data AS data,\'\' AS tags,assetname AS asset_key,assets_digest AS asset_id FROM dataItemsAndAssets WHERE " + selection, params);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private synchronized long getAppKey(String packageName, String signatureDigest) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT _id FROM appkeys WHERE packageName=? AND signatureDigest=?", new String[]{packageName, signatureDigest});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            cursor.close();
        }
        ContentValues appKey = new ContentValues();
        appKey.put("packageName", packageName);
        appKey.put("signatureDigest", signatureDigest);
        return getWritableDatabase().insert("appkeys", null, appKey);
    }

    public void putDataItem(String packageName, String signatureDigest, String host, String path, ContentValues data) {
        ContentValues item = new ContentValues(data);
        item.put("appkeys_id", getAppKey(packageName, signatureDigest));
        item.put("host", host);
        item.put("path", path);
        getWritableDatabase().insertWithOnConflict("dataitems", "host", item, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteDataItem(String packageName, String signatureDigest, String host, String path) {
        getWritableDatabase().delete("dataitems", "packageName=? AND signatureDigest=? AND host=? AND path=?", new String[]{packageName, signatureDigest, host, packageName});
    }
}
