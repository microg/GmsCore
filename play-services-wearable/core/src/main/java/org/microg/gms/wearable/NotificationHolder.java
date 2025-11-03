/*
 * Copyright (C) 2023 microG Project Team
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

package org.microg.gms.wearable;

import android.app.Notification;
import android.os.Bundle;

public class NotificationHolder {
    public String packageName;
    public int id;
    public String tag;
    public String title;
    public String text;

    public NotificationHolder(String packageName, int id, String tag, Notification notification) {
        this.packageName = packageName;
        this.id = id;
        this.tag = tag;

        Bundle extras = notification.extras;
        this.title = extras.getString(Notification.EXTRA_TITLE);
        this.text = extras.getString(Notification.EXTRA_TEXT);
    }
}
