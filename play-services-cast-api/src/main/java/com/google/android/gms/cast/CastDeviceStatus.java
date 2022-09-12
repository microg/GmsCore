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

package com.google.android.gms.cast;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class CastDeviceStatus extends AutoSafeParcelable {

    public CastDeviceStatus() {
    }

    public CastDeviceStatus(double volume, boolean mute, int activeInputState, ApplicationMetadata applicationMetadata, int standbyState) {
        this.volume = volume;
        this.mute = mute;
        this.activeInputState = activeInputState;
        this.applicationMetadata = applicationMetadata;
        this.standbyState = standbyState;
    }

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private double volume;
    @SafeParceled(3)
    private boolean mute;
    @SafeParceled(4)
    private int activeInputState;
    @SafeParceled(5)
    private ApplicationMetadata applicationMetadata;
    @SafeParceled(6)
    private int standbyState;

    public static final Creator<CastDeviceStatus> CREATOR = new AutoCreator<CastDeviceStatus>(CastDeviceStatus.class);
}
