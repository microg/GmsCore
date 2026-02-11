/*
 * Copyright 2013-2025 microG Project Team
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

package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Objects;

public class BooleanResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    public int status;
    @SafeParceled(2)
    public boolean result;

    private BooleanResponse() {}

    public BooleanResponse(int status, boolean result) {
        this.status = status;
        this.result = result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BooleanResponse)) {
            return false;
        }
        BooleanResponse other = (BooleanResponse) obj;
        return this.status == other.status && this.result == other.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, result);
    }

	public static final Creator<BooleanResponse> CREATOR = new AutoCreator<BooleanResponse>(BooleanResponse.class);
}
