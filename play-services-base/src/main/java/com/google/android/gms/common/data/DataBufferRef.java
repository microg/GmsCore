/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.data;

import android.database.CharArrayBuffer;
import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;

@Hide
public abstract class DataBufferRef {
    protected DataHolder dataHolder;
    protected int dataRow;
    private int windowIndex;

    public DataBufferRef(DataHolder dataHolder, int dataRow) {
        this.dataHolder = dataHolder;
        setDataRow(dataRow);
    }

    protected void copyToBuffer(@NonNull String column, @NonNull CharArrayBuffer dataOut) {
        dataHolder.copyToBuffer(column, dataRow, windowIndex, dataOut);
    }

    protected boolean getBoolean(@NonNull String column) {
        return dataHolder.getBoolean(column, dataRow, windowIndex);
    }

    protected byte[] getByteArray(@NonNull String column) {
        return dataHolder.getByteArray(column, dataRow, windowIndex);
    }

    protected double getDouble(@NonNull String column) {
        return dataHolder.getDouble(column, dataRow, windowIndex);
    }

    protected float getFloat(@NonNull String column) {
        return dataHolder.getFloat(column, dataRow, windowIndex);
    }

    protected int getInteger(@NonNull String column) {
        return dataHolder.getInteger(column, dataRow, windowIndex);
    }

    protected long getLong(@NonNull String column) {
        return dataHolder.getLong(column, dataRow, windowIndex);
    }

    protected String getString(@NonNull String column) {
        return dataHolder.getString(column, dataRow, windowIndex);
    }

    protected boolean hasColumn(@NonNull String column) {
        return dataHolder.hasColumn(column);
    }

    protected boolean hasNull(@NonNull String column) {
        return dataHolder.hasNull(column, dataRow, windowIndex);
    }

    public boolean isDataValid() {
        return !this.dataHolder.isClosed();
    }

    public void setDataRow(int dataRow) {
        this.dataRow = dataRow;
        this.windowIndex = dataHolder.getWindowIndex(dataRow);
    }
}
