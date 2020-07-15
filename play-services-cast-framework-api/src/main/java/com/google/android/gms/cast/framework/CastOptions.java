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

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.LaunchOptions;

import java.util.ArrayList;
import java.util.List;

public class CastOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;

    @SafeParceled(2)
    private String receiverApplicationId;

    @SafeParceled(3)
    private ArrayList<String> supportedNamespaces;

    @SafeParceled(4)
    private boolean stopReceiverApplicationWhenEndingSession;

    @SafeParceled(5)
    private LaunchOptions launchOptions;

    @SafeParceled(6)
    private boolean resumeSavedSession;

    @SafeParceled(7)
    private CastMediaOptions castMediaOptions;

    @SafeParceled(8)
    private boolean enableReconnectionService;

    @SafeParceled(9)
    private double volumeDeltaBeforeIceCreamSandwich;

    public String getReceiverApplicationId() {
        return this.receiverApplicationId;
    }

    public LaunchOptions getLaunchOptions() {
        return this.launchOptions;
    }

    public static Creator<CastOptions> CREATOR = new AutoCreator<CastOptions>(CastOptions.class);
}
