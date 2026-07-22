/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.data;

import com.google.android.gms.common.api.Releasable;

import org.microg.gms.common.PublicApi;

import java.util.Iterator;

/**
 * Interface for a buffer of typed data.
 */
public interface DataBuffer<T> extends Releasable, Iterable<T> {

    /**
     * Releases the data buffer, for use in try-with-resources.
     * <p>
     * Both close and release shall have identical semantics, and are idempotent.
     */
    void close();

    /**
     * Returns an element on specified position.
     */
    T get(int position);

    int getCount();

    /**
     * @deprecated {@link #release()} is idempotent, and so is safe to call multiple times
     */
    @Deprecated
    boolean isClosed();

    @Override
    Iterator<T> iterator();

    /**
     * Releases resources used by the buffer. This method is idempotent.
     */
    @Override
    void release();

    /**
     * In order to use this one should correctly override setDataRow(int) in his DataBufferRef
     * implementation. Be careful: there will be single DataBufferRef while iterating.
     * If you are not sure - DO NOT USE this iterator.
     */
    Iterator<T> singleRefIterator();
}
