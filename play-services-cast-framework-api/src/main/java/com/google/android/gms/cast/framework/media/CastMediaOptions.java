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

package com.google.android.gms.cast.framework.media;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class CastMediaOptions extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public String mediaIntentReceiverClassName;
    @SafeParceled(3)
    public String expandedControllerActivityClassName;
    @SafeParceled(4)
    public IImagePicker imagePicker;
    @SafeParceled(5)
    public NotificationOptions notificationOptions;

    public static Creator<CastMediaOptions> CREATOR = new AutoCreator<CastMediaOptions>(CastMediaOptions.class);
}
