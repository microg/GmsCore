/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.data;

import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;

import java.util.NoSuchElementException;

@Hide
public class SingleRefDataBufferIterator<T> extends DataBufferIterator<T> {
    private T element;

    public SingleRefDataBufferIterator(@NonNull DataBuffer<T> dataBuffer) {
        super(dataBuffer);
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Cannot advance the iterator beyond " + position);
        }
        ++position;
        if (position == 0) {
            element = dataBuffer.get(position);
            if (!(element instanceof DataBufferRef)) {
                throw new IllegalStateException("DataBuffer reference of type " + element.getClass() + " is not movable");
            }
        } else {
            ((DataBufferRef) element).setDataRow(position);
        }
        return element;
    }
}
