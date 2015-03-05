package com.google.android.gms.common.data;

import android.database.Cursor;
import android.database.CursorWindow;
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
    private String[] columns;

    @SafeParceled(2)
    private CursorWindow[] windows;

    @SafeParceled(3)
    private int statusCode;

    @SafeParceled(4)
    private Bundle metadata;

    public DataHolder(String[] columns, CursorWindow[] windows, int statusCode, Bundle metadata) {
        this.columns = columns;
        this.windows = windows;
        this.statusCode = statusCode;
        this.metadata = metadata;
    }


    public static DataHolder fromCursor(Cursor cursor, int statusCode, Bundle metadata) {
        List<CursorWindow> windows = new ArrayList<>();
        CursorWindow cursorWindow = null;
        int row = 0;
        while (cursor.moveToNext()) {
            if (cursorWindow == null || !cursorWindow.allocRow()) {
                cursorWindow = new CursorWindow(false);
                cursorWindow.setNumColumns(cursor.getColumnCount());
                windows.add(cursorWindow);
                row = 0;
            }
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_NULL:
                        cursorWindow.putNull(row, i);
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        cursorWindow.putBlob(cursor.getBlob(i), row, i);
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        cursorWindow.putDouble(cursor.getDouble(i), row, i);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        cursorWindow.putLong(cursor.getLong(i), row, i);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
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

    @Override
    public String toString() {
        return "DataHolder{" +
                "columns=" + Arrays.toString(columns) +
                ", windows=" + Arrays.toString(windows) +
                ", statusCode=" + statusCode +
                ", metadata=" + metadata +
                '}';
    }

    public static final Creator<DataHolder> CREATOR = new AutoCreator<>(DataHolder.class);
}
