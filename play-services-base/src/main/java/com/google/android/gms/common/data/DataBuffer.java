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

package com.google.android.gms.common.data;

import com.google.android.gms.common.api.Releasable;

import org.microg.gms.common.PublicApi;

import java.util.Iterator;

/**
 * TODO
 */
@PublicApi
public abstract class DataBuffer<T> implements Releasable, Iterable<T> {

    private DataHolder dataHolder;

    @PublicApi(exclude = true)
    public DataBuffer(DataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    /**
     * @deprecated use {@link #release()} instead
     */
    @Deprecated
    public final void close() {
        release();
    }

    /**
     * Get the item at the specified position. Note that the objects returned from subsequent
     * invocations of this method for the same position may not be identical objects, but will be
     * equal in value.
     *
     * @param position The position of the item to retrieve.
     * @return the item at {@code position} in this buffer.
     */
    public abstract T get(int position);

    public int getCount() {
        return dataHolder == null ? 0 : dataHolder.getCount();
    }

    /**
     * @deprecated {@link #release()} is idempotent, and so is safe to call multiple times
     */
    @Deprecated
    public boolean isClosed() {
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    /**
     * Releases resources used by the buffer. This method is idempotent.
     */
    @Override
    public void release() {

    }

    /**
     * In order to use this one should correctly override setDataRow(int) in his DataBufferRef
     * implementation. Be careful: there will be single DataBufferRef while iterating.
     * If you are not sure - DO NOT USE this iterator.
     */
    public Iterator<T> singleRefIterator() {
        return null;
    }
}
