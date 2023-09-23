/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.data;

import org.microg.gms.common.Hide;

import java.util.Iterator;

/**
 * Default implementation of DataBuffer. An {@code AbstractDataBuffer} wraps data provided across the binder from Google Play services.
 */
public abstract class AbstractDataBuffer<T> implements DataBuffer<T> {
    protected final DataHolder dataHolder;

    @Hide
    public AbstractDataBuffer(DataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    /**
     * Releases the data buffer, for use in try-with-resources.
     * <p>
     * Both close and release shall have identical semantics, and are idempotent.
     */
    @Override
    public void close() {
        release();
    }

    /**
     * Get the item at the specified position. Note that the objects returned from subsequent invocations of this method for the
     * same position may not be identical objects, but will be equal in value. In other words:
     * <p>
     * {@code buffer.get(i) == buffer.get(i)} may return false.
     * <p>
     * {@code buffer.get(i).equals(buffer.get(i))} will return true.
     *
     * @param position The position of the item to retrieve.
     * @return the item at {@code position} in this buffer.
     */
    public abstract T get(int position);

    @Override
    public int getCount() {
        if (dataHolder == null) return 0;
        return dataHolder.getCount();
    }

    /**
     * @deprecated {@link #release()} and {@link #close()} are idempotent, and so is safe to call multiple times
     */
    @Deprecated
    @Override
    public boolean isClosed() {
        if (dataHolder == null) return true;
        return dataHolder.isClosed();
    }

    @Override
    public Iterator<T> iterator() {
        return new DataBufferIterator(this);
    }

    /**
     * Releases resources used by the buffer. This method is idempotent.
     */
    @Override
    public void release() {
        if (dataHolder != null) dataHolder.close();
    }

    /**
     * In order to use this you should correctly override DataBufferRef.setDataRow(int) in your DataBufferRef implementation.
     * Be careful: there will be single DataBufferRef while iterating. If you are not sure - DO NOT USE this iterator.
     *
     * @see SingleRefDataBufferIterator
     */
    @Override
    public Iterator<T> singleRefIterator() {
        return new SingleRefDataBufferIterator(this);
    }
}
