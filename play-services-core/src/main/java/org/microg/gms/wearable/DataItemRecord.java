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
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.internal.DataItemAssetParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;

import org.microg.wearable.proto.AssetEntry;
import org.microg.wearable.proto.SetDataItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okio.ByteString;

public class DataItemRecord {
    private static String[] EVENT_DATA_HOLDER_FIELDS = new String[] { "event_type", "path", "data", "tags", "asset_key", "asset_id" };

    public DataItemInternal dataItem;
    public String source;
    public long seqId;
    public long v1SeqId;
    public long lastModified;
    public boolean deleted;
    public boolean assetsAreReady;
    public String packageName;
    public String signatureDigest;

    public ContentValues toContentValues() {
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

    public DataHolder toEventDataHolder() {
        DataHolder.Builder builder = DataHolder.builder(EVENT_DATA_HOLDER_FIELDS);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("path", dataItem.uri.toString());
        if (deleted) {
            data.put("event_type", 2);
            builder.withRow(data);
        } else {
            data.put("event_type", 1);
            data.put("data", dataItem.data);
            data.put("tags", "");
            boolean added = false;
            for (Map.Entry<String, Asset> entry : dataItem.getAssets().entrySet()) {
                added = true;
                data.put("asset_id", entry.getValue().getDigest());
                data.put("asset_key", entry.getKey());
                builder.withRow(data);
                data = new HashMap<String, Object>();
                data.put("path", dataItem.uri.toString());
            }
            if (!added) {
                builder.withRow(data);
            }
        }
        return builder.build(0);
    }

    public DataItemParcelable toParcelable() {
        Map<String, DataItemAssetParcelable> assets = new HashMap<>();
        for (Map.Entry<String, Asset> entry : dataItem.getAssets().entrySet()) {
            assets.put(entry.getKey(), new DataItemAssetParcelable(entry.getValue().getDigest(), entry.getKey()));
        }
        DataItemParcelable parcelable = new DataItemParcelable(dataItem.uri, assets);
        parcelable.data = dataItem.data;
        return parcelable;
    }

    public SetDataItem toSetDataItem() {
        SetDataItem.Builder builder = new SetDataItem.Builder()
                .packageName(packageName)
                .signatureDigest(signatureDigest)
                .uri(dataItem.uri.toString())
                .seqId(seqId)
                .deleted(deleted)
                .lastModified(lastModified);
        if (source != null) builder.source(source);
        if (dataItem.data != null) builder.data(ByteString.of(dataItem.data));
        List<AssetEntry> protoAssets = new ArrayList<AssetEntry>();
        Map<String, Asset> assets = dataItem.getAssets();
        for (String key : assets.keySet()) {
            protoAssets.add(new AssetEntry.Builder()
                    .key(key)
                    .unknown3(4)
                    .value(new org.microg.wearable.proto.Asset.Builder()
                            .digest(assets.get(key).getDigest())
                            .build()).build());
        }
        builder.assets(protoAssets);
        return builder.build();
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
        if (cursor.getString(11) != null) {
            record.dataItem.addAsset(cursor.getString(11), Asset.createFromRef(cursor.getString(12)));
            while (cursor.moveToNext()) {
                if (cursor.getLong(5) == record.seqId) {
                    record.dataItem.addAsset(cursor.getString(11), Asset.createFromRef(cursor.getString(12)));
                }
            }
            cursor.moveToPrevious();
        }
        return record;
    }

    public static DataItemRecord fromSetDataItem(SetDataItem setDataItem) {
        DataItemRecord record = new DataItemRecord();
        record.dataItem = new DataItemInternal(Uri.parse(setDataItem.uri));
        if (setDataItem.data != null) record.dataItem.data = setDataItem.data.toByteArray();
        if (setDataItem.assets != null) {
            for (AssetEntry asset : setDataItem.assets) {
                record.dataItem.addAsset(asset.key, Asset.createFromRef(asset.value.digest));
            }
        }
        record.source = setDataItem.source;
        record.seqId = setDataItem.seqId;
        record.v1SeqId = -1;
        record.lastModified = setDataItem.lastModified;
        record.deleted = setDataItem.deleted == null ? false : setDataItem.deleted;
        record.packageName = setDataItem.packageName;
        record.signatureDigest = setDataItem.signatureDigest;
        return record;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataItemRecord{");
        sb.append("dataItem=").append(dataItem);
        sb.append(", source='").append(source).append('\'');
        sb.append(", seqId=").append(seqId);
        sb.append(", v1SeqId=").append(v1SeqId);
        sb.append(", lastModified=").append(lastModified);
        sb.append(", deleted=").append(deleted);
        sb.append(", assetsAreReady=").append(assetsAreReady);
        sb.append(", packageName='").append(packageName).append('\'');
        sb.append(", signatureDigest='").append(signatureDigest).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
