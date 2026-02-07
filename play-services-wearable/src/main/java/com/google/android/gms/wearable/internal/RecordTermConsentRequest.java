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

public class RecordTermConsentRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public final int unk1;
    @SafeParceled(2)
    public final int unk2;
    @SafeParceled(3)
    public final boolean unk3;
    @SafeParceled(4)
    public final String unk4;
    @SafeParceled(5)
    public final String unk5;
    @SafeParceled(6)
    public final String unk6;

    public RecordTermConsentRequest(int unk1, int unk2, boolean unk3, String unk4, String unk5, String unk6) {
        this.unk1 = unk1;
        this.unk2 = unk2;
        this.unk3 = unk3;
        this.unk4 = unk4;
        this.unk5 = unk5;
        this.unk6 = unk6;
    }

    public static final Creator<RecordTermConsentRequest> CREATOR = new AutoCreator<RecordTermConsentRequest>(RecordTermConsentRequest.class);

}
