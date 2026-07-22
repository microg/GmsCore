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

package com.google.android.gms.cast.framework;

public final class CastState {
    public static final int NO_DEVICES_AVAILABLE = 1;
    public static final int NOT_CONNECTED = 2;
    public static final int CONNECTING = 3;
    public static final int CONNECTED = 4;

    public static String toString(int castState) {
        switch (castState) {
            case NO_DEVICES_AVAILABLE:
                return "NO_DEVICES_AVAILABLE";
            case NOT_CONNECTED:
                return "NOT_CONNECTED";
            case CONNECTING:
                return "CONNECTING";
            case CONNECTED:
                return "CONNECTED";
            default:
                return "UNKNOWN";
        }
    }
}
