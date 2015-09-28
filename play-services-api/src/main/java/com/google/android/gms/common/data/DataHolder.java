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

package com.google.android.gms.common.data;

import android.annotation.SuppressLint;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.os.Build;
import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for accessing collections of data, organized into columns. This provides the backing
 * support for DataBuffer. Much like a cursor, the holder supports the notion of a current
 * position, and has methods for extracting various types of data from named columns.
 */
public class DataHolder extends AutoSafeParcelable {
    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    public final String[] columns;

    @SafeParceled(2)
    public final CursorWindow[] windows;

    @SafeParceled(3)
    public final int statusCode;

    @SafeParceled(4)
    public final Bundle metadata;

    private DataHolder() {
        columns = null;
        windows = null;
        statusCode = 0;
        metadata = null;
    }

    public DataHolder(String[] columns, CursorWindow[] windows, int statusCode, Bundle metadata) {
        this.columns = columns;
        this.windows = windows;
        this.statusCode = statusCode;
        this.metadata = metadata;
    }

    protected static final int FIELD_TYPE_BLOB = 4;
    protected static final int FIELD_TYPE_FLOAT = 2;
    protected static final int FIELD_TYPE_INTEGER = 1;
    protected static final int FIELD_TYPE_NULL = 0;
    protected static final int FIELD_TYPE_STRING = 3;

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    static int getCursorType(Cursor cursor, int i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return cursor.getType(i);
        }
        if (cursor instanceof AbstractWindowedCursor) {
            CursorWindow cursorWindow = ((AbstractWindowedCursor) cursor).getWindow();
            int pos = cursor.getPosition();
            int type = -1;
            if (cursorWindow.isNull(pos, i)) {
                type = FIELD_TYPE_NULL;
            } else if (cursorWindow.isLong(pos, i)) {
                type = FIELD_TYPE_INTEGER;
            } else if (cursorWindow.isFloat(pos, i)) {
                type = FIELD_TYPE_FLOAT;
            } else if (cursorWindow.isString(pos, i)) {
                type = FIELD_TYPE_STRING;
            } else if (cursorWindow.isBlob(pos, i)) {
                type = FIELD_TYPE_BLOB;
            }

            return type;
        }
        throw new RuntimeException("Unsupported cursor on this platform!");
    }

    public static DataHolder fromCursor(Cursor cursor, int statusCode, Bundle metadata) {
        List<CursorWindow> windows = new ArrayList<CursorWindow>();
        CursorWindow cursorWindow = null;
        int row = 0;
        while (cursor.moveToNext()) {
            if (cursorWindow == null || !cursorWindow.allocRow()) {
                cursorWindow = new CursorWindow(false);
                cursorWindow.setNumColumns(cursor.getColumnCount());
                windows.add(cursorWindow);
                if (!cursorWindow.allocRow())
                    throw new RuntimeException("Impossible to store Cursor in CursorWindows");
                row = 0;
            }
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                switch (getCursorType(cursor, i)) {
                    case FIELD_TYPE_NULL:
                        cursorWindow.putNull(row, i);
                        break;
                    case FIELD_TYPE_BLOB:
                        cursorWindow.putBlob(cursor.getBlob(i), row, i);
                        break;
                    case FIELD_TYPE_FLOAT:
                        cursorWindow.putDouble(cursor.getDouble(i), row, i);
                        break;
                    case FIELD_TYPE_INTEGER:
                        cursorWindow.putLong(cursor.getLong(i), row, i);
                        break;
                    case FIELD_TYPE_STRING:
                        cursorWindow.putString(cursor.getString(i), row, i);
                        break;
                }
            }
            row++;
        }
        DataHolder dataHolder = new DataHolder(cursor.getColumnNames(), windows.toArray(new CursorWindow[windows.size()]), statusCode, metadata);
        cursor.close();
        return dataHolder;
    }


    public int getCount() {
        int c = 0;
        if (windows != null) {
            for (CursorWindow window : windows) {
                c += window.getNumRows();
            }
        }
        return c;
    }

    @Override
    public String toString() {
        return "DataHolder{" +
                "columns=" + Arrays.toString(columns) +
                ", windows=" + Arrays.toString(windows) +
                ", statusCode=" + statusCode +
                ", metadata=" + metadata +
                '}';
    }

    public static final Creator<DataHolder> CREATOR = new AutoCreator<DataHolder>(DataHolder.class);
}
