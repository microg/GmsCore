/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import org.microg.gms.common.Hide;

/**
 * Class to return annotated data. Currently, the only annotation is whether the data is stale or not.
 */
public class AnnotatedData<T> {
    private final T value;
    private final boolean stale;

    @Hide
    public AnnotatedData(T value, boolean stale) {
        this.value = value;
        this.stale = stale;
    }

    /**
     * Returns the data that is annotated by this class.
     */
    public T get() {
        return value;
    }

    /**
     * Returns {@code true} if the data returned by {@link #get()} is stale. This usually indicates that there was a network error and data was
     * returned from the local cache.
     */
    public boolean isStale() {
        return stale;
    }
}
