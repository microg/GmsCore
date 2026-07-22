/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.data;

import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;

import java.util.Iterator;
import java.util.NoSuchElementException;

@Hide
public class DataBufferIterator<T> implements Iterator<T> {
    protected DataBuffer<T> dataBuffer;
    protected int position = -1;

    public DataBufferIterator(@NonNull DataBuffer<T> dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Cannot advance the iterator beyond " + position);
        }
        return dataBuffer.get(++position);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove elements from a DataBufferIterator");
    }

    @Override
    public boolean hasNext() {
        return this.position < this.dataBuffer.getCount() - 1;
    }
}
