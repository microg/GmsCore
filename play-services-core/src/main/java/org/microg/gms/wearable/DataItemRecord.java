/*
 * Copyright 2013-2015 microG Project Team
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
import android.database.Cursor;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.internal.DataItemAssetParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;

import java.util.Map;

public class DataItemRecord {
    public DataItemInternal dataItem;
    public String source;
    public long seqId;
    public long v1SeqId;
    public long lastModified;
    public boolean deleted;
    public boolean assetsAreReady;
    public String packageName;
    public String signatureDigest;

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("sourceNode", source);
        contentValues.put("seqId", seqId);
        contentValues.put("v1SourceNode", source);
        contentValues.put("v1SeqId", v1SeqId);
        contentValues.put("timestampMs", lastModified);
        if (deleted) {
            contentValues.put("deleted", 1);
            contentValues.putNull("data");
        } else {
            contentValues.put("deleted", 0);
            contentValues.put("data", dataItem.data);
        }
        contentValues.put("assetsPresent", assetsAreReady ? 1 : 0);
        return contentValues;
    }

    public DataItemParcelable toParcelable() {
        DataItemParcelable parcelable = new DataItemParcelable(dataItem.uri);
        parcelable.data = dataItem.data;
        for (Map.Entry<String, Asset> entry : dataItem.getAssets().entrySet()) {
            parcelable.getAssets().put(entry.getKey(), new DataItemAssetParcelable(entry.getValue().getDigest(), entry.getKey()));
        }
        return parcelable;
    }

    public static DataItemRecord fromCursor(Cursor cursor) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = cursor.getString(1);
        record.signatureDigest = cursor.getString(2);
        record.dataItem = new DataItemInternal(cursor.getString(3), cursor.getString(4));
        record.seqId = cursor.getLong(5);
        record.deleted = cursor.getLong(6) > 0;
        record.source = cursor.getString(7);
        record.dataItem.data = cursor.getBlob(8);
        record.lastModified = cursor.getLong(9);
        record.assetsAreReady = cursor.getLong(10) > 0;
        return record;
    }
}
