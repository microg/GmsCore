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

public interface Freezable<T> {
    /**
     * Freeze a volatile representation into an immutable representation. Objects returned from
     * this call are safe to cache.
     * <p/>
     * Note that the output of {@link #freeze} may not be identical to the parent object, but
     * should be equal.
     *
     * @return A concrete implementation of the data object.
     */
    T freeze();

    /**
     * Check to see if this object is valid for use. If the object is still volatile, this method
     * will indicate whether or not the object can be safely used.
     * The output of a call to {@link #freeze()} will always be valid.
     *
     * @return whether or not the object is valid for use.
     */
    boolean isDataValid();
}
