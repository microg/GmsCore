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

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.LaunchOptions;

import java.util.ArrayList;
import java.util.List;

public class CastOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    private String receiverApplicationId;
    @Field(3)
    private ArrayList<String> supportedNamespaces;
    @Field(4)
    private boolean stopReceiverApplicationWhenEndingSession;
    @Field(5)
    private LaunchOptions launchOptions;
    @Field(6)
    private boolean resumeSavedSession;
    @Field(7)
    private CastMediaOptions castMediaOptions;
    @Field(8)
    private boolean enableReconnectionService;
    @Field(9)
    private double volumeDeltaBeforeIceCreamSandwich;

    public String getReceiverApplicationId() {
        return this.receiverApplicationId;
    }

    public LaunchOptions getLaunchOptions() {
        return this.launchOptions;
    }

    @NonNull
    public List<String> getSupportedNamespaces() {
        return supportedNamespaces;
    }

    public static Creator<CastOptions> CREATOR = new AutoCreator<CastOptions>(CastOptions.class);
}
