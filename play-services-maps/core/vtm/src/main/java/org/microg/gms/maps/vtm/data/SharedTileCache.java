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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SharedTileCache implements ITileCache {
    private final ArrayList<ByteArrayOutputStream> cacheBuffers;
    private final Context context;

    public SharedTileCache(Context context) {
        this.context = context;
        this.cacheBuffers = new ArrayList<ByteArrayOutputStream>();
    }

    @Override
    public TileWriter writeTile(Tile tile) {
        ByteArrayOutputStream os;
        synchronized (this.cacheBuffers) {
            if (this.cacheBuffers.isEmpty()) {
                os = new ByteArrayOutputStream('耀');
            } else {
                os = this.cacheBuffers.remove(this.cacheBuffers.size() - 1);
            }
        }

        return new CacheTileWriter(tile, os);
    }

    public void saveTile(Tile tile, ByteArrayOutputStream data, boolean success) {
        byte[] bytes = null;
        if (success) {
            bytes = data.toByteArray();
        }

        synchronized (this.cacheBuffers) {
            data.reset();
            this.cacheBuffers.add(data);
        }

        if (success) {
            ContentValues values = new ContentValues();
            values.put("x", tile.tileX);
            values.put("y", tile.tileY);
            values.put("z", tile.zoomLevel);
            values.put("time", 0);
            values.put("last_access", 0);
            values.put("data", bytes);
            context.getContentResolver().insert(SharedTileProvider.PROVIDER_URI, values);
        }
    }

    @Override
    public TileReader getTile(Tile tile) {
        Cursor cursor = context.getContentResolver().query(SharedTileProvider.PROVIDER_URI, new String[]{"data"}, "z=? AND x=? AND y=?", new String[]{String.valueOf(tile.zoomLevel), String.valueOf(tile.tileX), String.valueOf(tile.tileY)}, null);
        if (cursor != null) {
            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            } else {
                ByteArrayInputStream in = new ByteArrayInputStream(cursor.getBlob(0));
                cursor.close();
                return new CacheTileReader(tile, in);
            }
        }
        return null;
    }

    @Override
    public void setCacheSize(long l) {

    }

    class CacheTileWriter implements TileWriter {
        final ByteArrayOutputStream os;
        final Tile tile;

        CacheTileWriter(Tile tile, ByteArrayOutputStream os) {
            this.tile = tile;
            this.os = os;
        }

        public Tile getTile() {
            return tile;
        }

        public OutputStream getOutputStream() {
            return os;
        }

        public void complete(boolean success) {
            saveTile(tile, os, success);
        }
    }

    class CacheTileReader implements TileReader {
        final InputStream is;
        final Tile tile;

        public CacheTileReader(Tile tile, InputStream is) {
            this.tile = tile;
            this.is = is;
        }

        public Tile getTile() {
            return tile;
        }

        public InputStream getInputStream() {
            return is;
        }
    }
}
